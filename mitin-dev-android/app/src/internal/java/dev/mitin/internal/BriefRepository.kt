package dev.mitin.internal

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.*
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Serializable data class BriefMessage(val role: String, val text: String)
@Serializable data class BriefSection(val title: String, val text: String)
@Serializable data class BriefContact(val name: String, val contact: String, @SerialName("contact_type") val contactType: String)
@Serializable data class BriefState(
    @SerialName("session_id") val sessionId: String, val revision: Int,
    @SerialName("expires_at") val expiresAt: String, val service: String,
    @SerialName("budget_range") val budget: String,
    @SerialName("source_portfolio_slug") val portfolio: String? = null,
    val messages: List<BriefMessage> = emptyList(),
    @SerialName("final_brief") val finalBrief: String? = null,
    val sections: List<BriefSection> = emptyList(), val prepared: BriefContact? = null,
    val proof: String? = null, @SerialName("legal_path") val legalPath: String,
    @SerialName("legal_digest") val legalDigest: String,
    val submitted: Boolean = false, val reference: String? = null, val pending: Boolean = false
) { override fun toString() = "BriefState(<redacted>)" }

@Serializable data class BriefRecord(
    val id: String, val secret: String, val start: JsonObject,
    val pendingPath: String? = null, val pendingBody: JsonObject? = null
) { override fun toString() = "BriefRecord(<redacted>)" }
class BriefFailure(val status: Int, val code: String = "unavailable") : Exception("Brief operation failed ($status)")

interface BriefRepository {
    suspend fun get(record: BriefRecord): BriefState
    suspend fun execute(record: BriefRecord): BriefState
}

class HttpBriefRepository(baseUrl: String, private val client: OkHttpClient = HttpAuthApi.secureClient().newBuilder()
    .readTimeout(55, TimeUnit.SECONDS).callTimeout(60, TimeUnit.SECONDS).build()) : BriefRepository {
    private val base = HttpAuthApi.checkedOrigin(baseUrl)
    private val json = Json { ignoreUnknownKeys = true }
    override suspend fun get(record: BriefRecord) = request(record, "sessions/${record.id}", null)
    override suspend fun execute(record: BriefRecord) = request(record, record.pendingPath ?: "sessions", record.pendingBody ?: record.start)
    private suspend fun request(record: BriefRecord, path: String, body: JsonObject?): BriefState {
        require(Regex("sessions(?:/[a-f0-9-]{36}(?:/(messages|final|prepare|confirm))?)?").matches(path))
        val url = base.newBuilder().encodedPath("/api/public/mobile-brief/$path").build()
        val request = Request.Builder().url(url).header("Accept", "application/json")
            .header("Authorization", "Bearer ${record.secret}")
            .apply { if(body != null) post(body.toString().toRequestBody("application/json".toMediaType())) }.build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if(continuation.isActive) continuation.resumeWithException(BriefFailure(if(e is java.net.SocketTimeoutException) 504 else 0))
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use {
                            val source = it.body.source()
                            if(source.request(1_048_577)) throw BriefFailure(503)
                            val text = source.readUtf8()
                            if(!it.isSuccessful) {
                                val code = runCatching { json.parseToJsonElement(text).jsonObject["detail"]?.jsonObject?.get("code")?.jsonPrimitive?.content }.getOrNull()
                                throw BriefFailure(it.code, code ?: "unavailable")
                            }
                            val value = json.decodeFromString<BriefState>(text)
                            if(value.sessionId != record.id || value.messages.size > 40 || (value.finalBrief?.length ?: 0) > 12000) throw BriefFailure(503)
                            if(continuation.isActive) continuation.resume(value)
                        }
                    } catch(e: Exception) { if(continuation.isActive) continuation.resumeWithException(if(e is BriefFailure) e else BriefFailure(503)) }
                }
            })
        }
    }
}
