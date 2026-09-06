package dev.mitin.internal

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Secrets have no diagnostic export. Only explicit wire/storage operations unwrap them. */
class Secret(val value: String) { override fun toString() = "<redacted>" }
class TokenPair(val access: Secret, val refresh: Secret, val expiresIn: Int) {
    override fun toString() = "TokenPair(<redacted>)"
}
@Serializable data class Me(
    @SerialName("user_id") val userId: String,
    @SerialName("client_profile_id") val clientProfileId: String,
    @SerialName("display_name") val displayName: String,
    val role: String
)
@Serializable data class RemoteSession(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("absolute_expires_at") val absoluteExpiresAt: String,
    @SerialName("idle_expires_at") val idleExpiresAt: String,
    @SerialName("revoked_at") val revokedAt: String?,
    @SerialName("revocation_reason") val revocationReason: String?,
    val current: Boolean
)
@Serializable data class SessionPage(val items: List<RemoteSession>, @SerialName("next_offset") val nextOffset: Int?)

class AuthFailure(val status: Int = 503) : Exception("Authentication operation failed ($status)")
class SignedOut : Exception("Sign in required")
class Superseded : Exception("Account operation superseded")

interface AuthApi {
    suspend fun login(email: String, password: Secret, epoch: Long): TokenPair
    suspend fun refresh(token: Secret, epoch: Long): TokenPair
    suspend fun me(access: Secret, epoch: Long): Me
    suspend fun sessions(access: Secret, epoch: Long, offset: Int): SessionPage
    suspend fun revoke(access: Secret, epoch: Long, id: String)
    suspend fun logout(access: Secret, epoch: Long, all: Boolean)
    fun cancel(epoch: Long)
}

/** No production fallback. The current integration supports loopback TLS only. */
class HttpAuthApi(baseUrl: String, private val client: OkHttpClient = secureClient()) : AuthApi {
    private val base = checkedOrigin(baseUrl)
    private val json = Json { ignoreUnknownKeys = false }
    companion object {
        fun checkedOrigin(value: String): HttpUrl {
            val url = try { value.toHttpUrl() } catch (_: Exception) { throw AuthFailure() }
            if (url.scheme != "https" || url.host !in setOf("localhost", "127.0.0.1") ||
                url.encodedPath != "/" || url.query != null || url.fragment != null ||
                url.username.isNotEmpty() || url.password.isNotEmpty()) throw AuthFailure()
            return url
        }
        fun secureClient() = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
            .authenticator(Authenticator.NONE).proxyAuthenticator(Authenticator.NONE)
            .cookieJar(CookieJar.NO_COOKIES).cache(null)
            .dns { host ->
                if (host !in setOf("localhost", "127.0.0.1")) throw java.net.UnknownHostException("Untrusted origin")
                listOf(InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1)))
            }.build()
    }
    private class AccountCall(val epoch: Long)
    override fun cancel(epoch: Long) {
        (client.dispatcher.queuedCalls() + client.dispatcher.runningCalls()).forEach {
            if (it.request().tag(AccountCall::class.java)?.epoch == epoch) it.cancel()
        }
    }
    private suspend fun request(path: String, method: String, epoch: Long, access: Secret? = null, body: JsonObject? = null, offset: Int? = null): String {
        val url = base.newBuilder().encodedPath("/api/v1/$path").apply {
            if (offset != null) { addQueryParameter("limit", "20"); addQueryParameter("offset", offset.toString()) }
        }.build()
        // All paths originate in fixed typed methods below. No caller supplies a URL.
        check(url.host == base.host && url.port == base.port && url.scheme == base.scheme)
        val builder = Request.Builder().url(url).tag(AccountCall::class.java, AccountCall(epoch))
            .header("Cache-Control", "no-store").header("Accept", "application/json")
        if (access != null) builder.header("Authorization", "Bearer ${access.value}")
        val payload = if (method == "POST") (body?.toString() ?: "{}").toRequestBody("application/json; charset=utf-8".toMediaType()) else null
        builder.method(method, payload)
        val call = client.newCall(builder.build())
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(AuthFailure())
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            // Redirects, cookies and missing no-store are not successful auth responses.
                            if (!response.isSuccessful) throw AuthFailure(response.code)
                            if (response.header("Cache-Control") != "no-store" || response.headers("Set-Cookie").isNotEmpty()) throw AuthFailure()
                            val source = response.body.source()
                            source.request(65_537)
                            if (source.buffer.size > 65_536) throw AuthFailure()
                            val text = source.readUtf8()
                            if (continuation.isActive) continuation.resume(text)
                        } catch (_: IOException) {
                            if (continuation.isActive) continuation.resumeWithException(AuthFailure())
                        } catch (failure: AuthFailure) {
                            if (continuation.isActive) continuation.resumeWithException(failure)
                        }
                    }
                }
            })
        }
    }
    private fun pair(text: String): TokenPair = try {
        val value = json.parseToJsonElement(text).jsonObject
        if (value.keys != setOf("access_token", "refresh_token", "token_type", "expires_in") || value["token_type"]?.jsonPrimitive?.content != "Bearer") throw AuthFailure()
        val access = value.getValue("access_token").jsonPrimitive.content
        val refresh = value.getValue("refresh_token").jsonPrimitive.content
        val ttl = value.getValue("expires_in").jsonPrimitive.int
        if (access.length !in 100..8192 || !Regex("mdr1_[A-Za-z0-9_-]{43}").matches(refresh) || ttl !in 1..900) throw AuthFailure()
        TokenPair(Secret(access), Secret(refresh), ttl)
    } catch (_: Exception) { throw AuthFailure() }
    override suspend fun login(email: String, password: Secret, epoch: Long) = pair(request("auth/login", "POST", epoch, body = buildJsonObject {
        put("email", email); put("password", password.value)
    }))
    override suspend fun refresh(token: Secret, epoch: Long) = pair(request("auth/refresh", "POST", epoch, body = buildJsonObject { put("refresh_token", token.value) }))
    override suspend fun me(access: Secret, epoch: Long): Me {
        val text = request("me", "GET", epoch, access)
        return try { json.decodeFromString<Me>(text).also {
            UUID.fromString(it.userId); UUID.fromString(it.clientProfileId)
            if (it.role != "client" || it.displayName.length !in 1..200) throw AuthFailure()
        } } catch (_: Exception) { throw AuthFailure() }
    }
    override suspend fun sessions(access: Secret, epoch: Long, offset: Int): SessionPage {
        if (offset !in 0..10000) throw AuthFailure(422)
        val text = request("auth/sessions", "GET", epoch, access, offset = offset)
        return try { json.decodeFromString<SessionPage>(text).also {
            if (it.items.size > 20 || (it.nextOffset != null && it.nextOffset !in (offset + 1)..10000)) throw AuthFailure()
            it.items.forEach { item -> UUID.fromString(item.id) }
        } } catch (_: Exception) { throw AuthFailure() }
    }
    override suspend fun revoke(access: Secret, epoch: Long, id: String) {
        val uuid = try { UUID.fromString(id) } catch (_: Exception) { throw AuthFailure(404) }
        request("auth/sessions/$uuid", "DELETE", epoch, access)
    }
    override suspend fun logout(access: Secret, epoch: Long, all: Boolean) {
        request(if (all) "auth/logout-all" else "auth/logout", "POST", epoch, access)
    }
}
