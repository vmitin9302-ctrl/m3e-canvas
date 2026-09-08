package dev.mitin.internal

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.mitin.demo.BuildConfig
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import okhttp3.*
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Serializable data class Capabilities(
    @SerialName("mobile_ai_brief") val ai: Boolean = false,
    @SerialName("mobile_lead_submission") val submission: Boolean = false,
    @SerialName("client_auth") val auth: Boolean = false
)
@Serializable data class ServiceMeta(@SerialName("api_version") val version: String, val service: String, val capabilities: Capabilities)

class MetaRepository {
    suspend fun load(): Capabilities = withTimeoutOrNull(20_000) {
        val base = HttpAuthApi.checkedOrigin(BuildConfig.API_BASE_URL)
        val call = HttpAuthApi.secureClient().newCall(Request.Builder().url(base.newBuilder().encodedPath("/api/v1/meta").build())
            .header("Accept", "application/json").header("Cache-Control", "no-cache").build())
        // A coroutine deadline must also cover Android DNS, which can outlive OkHttp's call timeout offline.
        suspendCancellableCoroutine<Capabilities> { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val value = response.use {
                            check(it.isSuccessful && it.header("Content-Type")?.startsWith("application/json") == true)
                            val source = it.body.source(); check(!source.request(16_385))
                            val meta = Json { ignoreUnknownKeys = true }.decodeFromString<ServiceMeta>(source.readUtf8())
                            check(meta.version == "v1" && meta.service == "MITIN DEV")
                            meta.capabilities
                        }
                        if (continuation.isActive) continuation.resume(value)
                    } catch (e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
                }
            })
        }
    } ?: throw IOException("Metadata deadline exceeded")
}

class MetaViewModel(app: Application) : AndroidViewModel(app) {
    var loading by mutableStateOf(BuildConfig.FLAVOR == "production"); private set
    var failed by mutableStateOf(false); private set
    init { if (BuildConfig.FLAVOR == "production") reload() }
    fun reload() {
        if (!loading || failed) loading = true
        failed = false
        viewModelScope.launch {
            try {
                val value = MetaRepository().load()
                getApplication<InternalApplication>().capabilities = value
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { failed = true }
            finally { loading = false }
        }
    }
}
