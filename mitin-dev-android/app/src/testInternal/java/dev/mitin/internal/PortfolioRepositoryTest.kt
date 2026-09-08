package dev.mitin.internal

import kotlinx.coroutines.*
import okhttp3.mockwebserver.*
import okhttp3.tls.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class PortfolioRepositoryTest {
    private val item = """{"slug":"server-case","title":"Server title","short_description":"Short","description":"Long","cover_image_url":null,"project_url":"https://example.com"}"""
    private fun tls(block: suspend CoroutineScope.(MockWebServer, HttpPortfolioRepository) -> Unit) = runBlocking {
        val cert = HeldCertificate.Builder().commonName("localhost").addSubjectAlternativeName("localhost").build()
        val serverTls = HandshakeCertificates.Builder().heldCertificate(cert).build()
        val trust = HandshakeCertificates.Builder().addTrustedCertificate(cert.certificate).build()
        val server = MockWebServer(); server.useHttps(serverTls.sslSocketFactory(), false); server.start()
        val client = HttpAuthApi.secureClient().newBuilder().sslSocketFactory(trust.sslSocketFactory(), trust.trustManager)
            .callTimeout(10, TimeUnit.SECONDS).build()
        try { block(server, HttpPortfolioRepository(server.url("/").toString(), client)) }
        finally { server.shutdown() }
    }
    private fun response(body: String, status: Int = 200) = MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body)
    @Test fun serverDataAndRefreshAreUnauthenticated() = tls { server, repo ->
        server.enqueue(response("[$item]")); server.enqueue(response("[]")); server.enqueue(response(item))
        assertEquals("Server title", repo.list().single().title)
        assertTrue(repo.list().isEmpty())
        assertEquals("server-case", repo.detail("server-case").slug)
        repeat(3) { val r = server.takeRequest(); assertNull(r.getHeader("Authorization")); assertNull(r.getHeader("Cookie")); assertEquals("no-cache", r.getHeader("Cache-Control")) }
    }
    @Test fun statusAndMalformedPayloadFailWithoutFakeData() = tls { server, repo ->
        for (status in listOf(404, 500)) {
            server.enqueue(response("{}", status))
            assertEquals(status, (runCatching { repo.list() }.exceptionOrNull() as PortfolioFailure).status)
        }
        for (body in listOf("not json", "{}", "[{}]", "[$item,$item]")) {
            server.enqueue(response(body)); assertTrue(runCatching { repo.list() }.exceptionOrNull() is PortfolioFailure)
        }
        server.enqueue(response(item.replace("server-case", "other")))
        assertTrue(runCatching { repo.detail("server-case") }.isFailure)
    }
    @Test fun timeoutThenRetryRecovers() = tls { server, repo ->
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        assertTrue(runCatching { repo.list() }.exceptionOrNull() is PortfolioFailure)
        server.enqueue(response("[$item]")); assertEquals(1, repo.list().size)
    }
    @Test fun cancellationStopsSlowRequest() = tls { server, repo ->
        server.enqueue(response("[$item]").setBodyDelay(5, TimeUnit.SECONDS))
        val job = launch { repo.list() }; delay(100); job.cancelAndJoin(); assertTrue(job.isCancelled)
    }
    @Test fun image404SlowResponseAndCancellation() = tls { server, repo ->
        val url = server.url("/portfolio/cover.jpg").toString()
        server.enqueue(MockResponse().setResponseCode(404))
        assertEquals(404, (runCatching { repo.image(url) }.exceptionOrNull() as PortfolioFailure).status)
        server.enqueue(MockResponse().setHeader("Content-Type", "image/jpeg").setBody("bytes").setBodyDelay(5, TimeUnit.SECONDS))
        val job = launch { repo.image(url) }; delay(100); job.cancelAndJoin(); assertTrue(job.isCancelled)
    }
    @Test fun unsafeLinksAndSlugsNeverOpen() {
        for (url in listOf("javascript:x", "file:///a", "content://a", "intent://a", "data:x", "http://a", "https://u:p@a", "https://a\\evil", "https://a\n")) assertNull(safePortfolioUrl(url))
        assertEquals("https://example.com", safePortfolioUrl("https://example.com"))
        for (slug in listOf("../a", "x/y", "", "a".repeat(81))) assertFalse(HttpPortfolioRepository.validSlug(slug))
    }
}
