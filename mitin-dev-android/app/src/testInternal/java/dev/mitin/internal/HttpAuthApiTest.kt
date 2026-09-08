package dev.mitin.internal

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.*
import org.junit.Test

class HttpAuthApiTest {
    @Test fun mfaChallengeIsStrictAndVerificationUsesBodyOnly() = tls { server, api -> runBlocking {
        val raw = "c".repeat(43)
        server.enqueue(response("""{"mfa_required":true,"challenge_token":"$raw","expires_in":180}"""))
        val challenge = runCatching { api.login("owner@example.com", Secret("synthetic-password"), 1) }.exceptionOrNull()
        assertTrue(challenge is MfaRequired)
        assertFalse(challenge.toString().contains(raw))
        server.takeRequest()
        server.enqueue(response("""{"access_token":"${"a".repeat(200)}","refresh_token":"mdr1_${"r".repeat(43)}","token_type":"Bearer","expires_in":600}"""))
        api.verifyMfa((challenge as MfaRequired).challenge, Secret("123456"), 1)
        val request = server.takeRequest()
        assertEquals("/api/v1/auth/mfa/verify", request.path)
        assertNull(request.getHeader("Authorization")); assertNull(request.getHeader("Cookie"))
        assertTrue(request.body.readUtf8().contains("challenge_token"))
        for (body in listOf(
            """{"mfa_required":true,"challenge_token":"$raw","expires_in":181}""",
            """{"mfa_required":"true","challenge_token":"$raw","expires_in":180}""",
            """{"mfa_required":true,"challenge_token":"$raw","expires_in":"180"}""",
            """{"mfa_required":true,"challenge_token":"short","expires_in":180}"""
        )) {
            server.enqueue(response(body))
            assertTrue(runCatching { api.login("owner", Secret("synthetic-password"), 1) }.exceptionOrNull() is AuthFailure)
        }
    } }
    private fun response(body: String, status: Int = 200) = MockResponse().setResponseCode(status).setHeader("Cache-Control","no-store").setBody(body)
    private val me = """{"user_id":"00000000-0000-0000-0000-000000000001","client_profile_id":"00000000-0000-0000-0000-000000000002","display_name":"Синтетический клиент","role":"client"}"""
    private fun tls(block: (MockWebServer, HttpAuthApi) -> Unit) {
        val cert=HeldCertificate.Builder().commonName("test localhost").addSubjectAlternativeName("localhost").build()
        val serverTls=HandshakeCertificates.Builder().heldCertificate(cert).build()
        val trusted=HandshakeCertificates.Builder().addTrustedCertificate(cert.certificate).build()
        val server=MockWebServer(); server.useHttps(serverTls.sslSocketFactory(),false); server.start()
        try {
            val client=HttpAuthApi.secureClient().newBuilder().sslSocketFactory(trusted.sslSocketFactory(),trusted.trustManager).build()
            block(server,HttpAuthApi(server.url("/").toString(),client))
        } finally { server.shutdown() }
    }
    @Test fun verifiedTlsAndExactContractNoSecretUrl() = tls { server,api -> runBlocking {
        server.enqueue(response(me))
        assertEquals("Синтетический клиент",api.me(Secret("synthetic-access"),1).displayName)
        val r=server.takeRequest();assertEquals("/api/v1/me",r.path)
        assertEquals("Bearer synthetic-access",r.getHeader("Authorization"));assertNull(r.getHeader("Cookie"))
        assertEquals("no-store",r.getHeader("Cache-Control"))
    } }
    @Test fun redirectsAreNeverFollowed() = tls { server,api -> runBlocking {
        server.enqueue(response("",302).setHeader("Location",server.url("/other")))
        assertTrue(runCatching { api.login("synthetic@example.com",Secret("synthetic-password"),1) }.isFailure)
        assertEquals(1,server.requestCount)
    } }
    @Test fun unknownFieldsAreRejectedWithoutEchoingBody() = tls { server,api -> runBlocking {
        server.enqueue(response(me.dropLast(1)+",\"note\":\"internal-secret\"}"))
        val error=runCatching { api.me(Secret("synthetic-access"),1) }.exceptionOrNull()
        assertTrue(error is AuthFailure);assertFalse(error.toString().contains("internal-secret"))
    } }
    @Test fun missingNoStoreAndCookieResponsesAreRejected() = tls { server,api -> runBlocking {
        server.enqueue(MockResponse().setBody(me))
        assertTrue(runCatching { api.me(Secret("synthetic-access"),1) }.isFailure)
        server.enqueue(response(me).setHeader("Set-Cookie","session=synthetic"))
        assertTrue(runCatching { api.me(Secret("synthetic-access"),1) }.isFailure)
    } }
    @Test fun untrustedCertificateFailsClosed() = tls { server,_ -> runBlocking {
        assertTrue(runCatching { HttpAuthApi(server.url("/").toString()).me(Secret("synthetic-access"),1) }.isFailure)
        assertEquals(0,server.requestCount)
    } }
    @Test fun wrongHostnameFailsEvenWithTrustedCertificate() {
        val cert=HeldCertificate.Builder().commonName("wrong").addSubjectAlternativeName("wrong.invalid").build()
        val tls=HandshakeCertificates.Builder().heldCertificate(cert).build()
        val trust=HandshakeCertificates.Builder().addTrustedCertificate(cert.certificate).build()
        val server=MockWebServer();server.useHttps(tls.sslSocketFactory(),false);server.start()
        try { runBlocking {
            val client=HttpAuthApi.secureClient().newBuilder().sslSocketFactory(trust.sslSocketFactory(),trust.trustManager).build()
            assertTrue(runCatching { HttpAuthApi(server.url("/").toString(),client).me(Secret("synthetic-access"),1) }.isFailure)
            assertEquals(0,server.requestCount)
        } } finally {server.shutdown()}
    }
    @Test fun droppedLoginIsNotAutomaticallyRetried() = tls { server,api -> runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        assertTrue(runCatching { api.login("synthetic@example.com",Secret("synthetic-password"),1) }.isFailure)
        assertEquals(1,server.requestCount)
    } }
    @Test fun sequentialAuthCallsDoNotReuseIdleTlsConnections() = tls { server,api -> runBlocking {
        server.enqueue(response(me));server.enqueue(response(me))
        api.me(Secret("synthetic-access"),1)
        assertEquals(0,server.takeRequest().sequenceNumber)
        // The peer offers keep-alive. Once the first call is released, the next
        // call must establish a fresh verified TLS connection, not retry a stale one.
        kotlinx.coroutines.delay(100)
        api.me(Secret("synthetic-access"),1)
        assertEquals(0,server.takeRequest().sequenceNumber)
        assertEquals(2,server.requestCount)
    } }
    @Test fun unsafeOriginsRejectedBeforeAnyConnection() {
        for(url in listOf("http://localhost:8443/","https://example.com/","https://localhost/path","https://user:password@localhost/","https://localhost/?token=value"))
            assertTrue(runCatching { HttpAuthApi(url) }.isFailure)
    }
}
