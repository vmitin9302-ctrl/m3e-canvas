package dev.mitin.internal

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** A bounded, same-origin transport. The Repository owns all paths and credentials. */
class CabinetApi(baseUrl: String, private val client: OkHttpClient = HttpAuthApi.secureClient()) {
    private val base = HttpAuthApi.checkedOrigin(baseUrl)
    suspend fun call(path: String, method: String = "GET", access: Secret? = null, body: JsonObject? = null,
                     offset: Int? = null, bytes: ByteArray? = null, filename: String? = null,
                     contentType: String = "application/json", materialId: String? = null): ByteArray {
        require(Regex("[a-zA-Z0-9/-]+").matches(path) && !path.contains("..") && !path.startsWith('/'))
        require(method in setOf("GET", "POST", "PATCH", "PUT"))
        require(offset == null || offset in 0..10000)
        val url = base.newBuilder().encodedPath("/api/v1/$path").apply {
            offset?.let { addQueryParameter("limit", "20"); addQueryParameter("offset", it.toString()) }
        }.build()
        val request = Request.Builder().url(url).header("Cache-Control", "no-store")
        access?.let { request.header("Authorization", "Bearer ${it.value}") }
        filename?.let { request.header("X-Filename", java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20")) }
        materialId?.let { request.header("X-Material-Request-Id", it) }
        val payload = if (method == "GET") null else (bytes ?: (body?.toString() ?: "{}").toByteArray()).toRequestBody(contentType.toMediaType())
        val call = client.newCall(request.method(method, payload).build())
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(AuthFailure()) }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            if (!it.isSuccessful) throw AuthFailure(it.code)
                            if (it.header("Cache-Control") != "no-store" || it.headers("Set-Cookie").isNotEmpty()) throw AuthFailure()
                            val source = it.body.source()
                            val limit = if (path.endsWith("/download")) 10L * 1024 * 1024 else 1024L * 1024
                            if (source.request(limit + 1)) throw AuthFailure()
                            if (continuation.isActive) continuation.resume(source.readByteArray())
                        } catch (failure: Exception) {
                            if (continuation.isActive) continuation.resumeWithException(if (failure is AuthFailure) failure else AuthFailure())
                        }
                    }
                }
            })
        }
    }
}

class CabinetRepository(private val manager: SessionManager, private val api: CabinetApi) {
    suspend fun read(path: String, offset: Int? = null): JsonObject = manager.authorizedRead { token ->
        Json.parseToJsonElement(api.call(path, access=token, offset=offset).decodeToString()).jsonObject
    }
    suspend fun mutate(path: String, method: String, body: JsonObject): JsonObject = manager.authorizedMutation { token ->
        Json.parseToJsonElement(api.call(path, method, token, body).decodeToString()).jsonObject
    }
    suspend fun publicAction(path: String, body: JsonObject): JsonObject =
        Json.parseToJsonElement(api.call(path, "POST", body=body).decodeToString()).jsonObject
    suspend fun upload(project: String, name: String, type: String, bytes: ByteArray, material: String?): JsonObject = manager.authorizedMutation { token ->
        Json.parseToJsonElement(api.call("projects/$project/files", "POST", token, bytes=bytes, filename=name, contentType=type, materialId=material).decodeToString()).jsonObject
    }
    suspend fun download(project: String, id: String): ByteArray = manager.authorizedRead { token -> api.call("projects/$project/files/$id/download", access=token) }
}
