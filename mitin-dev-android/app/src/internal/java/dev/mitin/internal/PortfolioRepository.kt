package dev.mitin.internal

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Serializable data class PortfolioItem(
    val slug: String,
    val title: String,
    @SerialName("short_description") val shortDescription: String,
    val description: String,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("project_url") val projectUrl: String? = null
)
class PortfolioFailure(val status: Int = 503) : Exception("Portfolio unavailable ($status)")

fun safePortfolioUrl(value: String?): String? {
    if (value == null || value.length > 2048 || value.any { it.isWhitespace() || it.code < 32 } || '\\' in value) return null
    val url = value.toHttpUrlOrNull() ?: return null
    return value.takeIf { url.scheme == "https" && url.username.isEmpty() && url.password.isEmpty() }
}

interface PortfolioRepository {
    suspend fun list(): List<PortfolioItem>
    suspend fun detail(slug: String): PortfolioItem
}

/** Separate unauthenticated calls, reusing the existing verified TLS transport policy.
 * No token, cookie, auth retry, persistent cache or user-dependent state. */
class HttpPortfolioRepository(baseUrl: String, private val client: OkHttpClient = publicClient) : PortfolioRepository {
    private val base = HttpAuthApi.checkedOrigin(baseUrl)
    private val json = Json { ignoreUnknownKeys = true }
    companion object {
        private val publicClient by lazy { HttpAuthApi.secureClient() }
        val slugPattern = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")
        fun validSlug(slug: String) = slug.length in 1..80 && slugPattern.matches(slug)
    }
    private fun validate(item: PortfolioItem): PortfolioItem {
        if (!validSlug(item.slug) || item.title.length !in 1..200 || item.description.length > 4000 ||
            item.shortDescription.length > 4000 || (item.category?.length ?: 0) > 300 ||
            item.tags.size > 12 || item.tags.any { it.length > 100 }) throw PortfolioFailure()
        return item.copy(coverImageUrl = safePortfolioUrl(item.coverImageUrl), projectUrl = safePortfolioUrl(item.projectUrl))
    }
    override suspend fun list(): List<PortfolioItem> {
        val text = bytes(base.newBuilder().encodedPath("/api/public/portfolio").build(), 1_048_576, "application/json").decodeToString()
        return try {
            json.decodeFromString<List<PortfolioItem>>(text).also {
                if (it.size > 100 || it.map { p -> p.slug }.distinct().size != it.size) throw PortfolioFailure()
            }.map(::validate)
        } catch (_: Exception) { throw PortfolioFailure() }
    }
    override suspend fun detail(slug: String): PortfolioItem {
        if (!validSlug(slug)) throw PortfolioFailure(404)
        val text = bytes(base.newBuilder().encodedPath("/api/public/portfolio/$slug").build(), 65_536, "application/json").decodeToString()
        return try { validate(json.decodeFromString<PortfolioItem>(text)).also {
            if (it.slug != slug) throw PortfolioFailure()
        } } catch (_: Exception) { throw PortfolioFailure() }
    }
    suspend fun image(url: String): ByteArray {
        val parsed = safePortfolioUrl(url)?.toHttpUrlOrNull() ?: throw PortfolioFailure()
        // This stage's images are served by the isolated backend static route.
        if (parsed.host != base.host || parsed.port != base.port || !parsed.encodedPath.startsWith("/portfolio/")) throw PortfolioFailure()
        return bytes(parsed, 4L * 1024 * 1024, "image/")
    }
    private suspend fun bytes(url: HttpUrl, limit: Long, contentType: String): ByteArray {
        val call = client.newCall(Request.Builder().url(url).header("Accept", if (contentType.endsWith('/')) "${contentType}*" else contentType)
            .header("Cache-Control", "no-cache").build())
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(PortfolioFailure())
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            if (!response.isSuccessful) throw PortfolioFailure(response.code)
                            if (response.headers("Set-Cookie").isNotEmpty() ||
                                !(response.header("Content-Type") ?: "").startsWith(contentType)) throw PortfolioFailure()
                            val source = response.body.source()
                            source.request(limit + 1)
                            if (source.buffer.size > limit) throw PortfolioFailure()
                            val body = source.readByteArray()
                            if (continuation.isActive) continuation.resume(body)
                        } catch (e: Exception) {
                            if (continuation.isActive) continuation.resumeWithException(e as? PortfolioFailure ?: PortfolioFailure())
                        }
                    }
                }
            })
        }
    }
}
