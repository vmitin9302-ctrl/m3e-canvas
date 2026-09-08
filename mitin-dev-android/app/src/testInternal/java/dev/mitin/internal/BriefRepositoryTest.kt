package dev.mitin.internal

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.*
import okhttp3.tls.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class BriefRepositoryTest {
    @Test fun lostMutationIsNotRetriedAndStatusUsesSameSession() = runBlocking {
        val cert=HeldCertificate.Builder().commonName("localhost").addSubjectAlternativeName("localhost").build()
        val serverTls=HandshakeCertificates.Builder().heldCertificate(cert).build()
        val trust=HandshakeCertificates.Builder().addTrustedCertificate(cert.certificate).build()
        val server=MockWebServer();server.useHttps(serverTls.sslSocketFactory(),false);server.start()
        val client=HttpAuthApi.secureClient().newBuilder().sslSocketFactory(trust.sslSocketFactory(),trust.trustManager)
            .callTimeout(20,TimeUnit.SECONDS).build()
        try {
            val repo=HttpBriefRepository(server.url("/").toString(),client)
            val id="00000000-0000-4000-8000-000000000001"
            val record=BriefRecord(id,"synthetic-session-secret-do-not-log",JsonObject(emptyMap()),"sessions/$id/confirm",buildJsonObject {put("request_id","same-request")})
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
            assertTrue(runCatching {repo.execute(record)}.exceptionOrNull() is BriefFailure)
            assertEquals(1,server.requestCount)
            server.enqueue(MockResponse().setBody("""{"session_id":"$id","revision":4,"expires_at":"2026-09-14T00:00:00Z","service":"site","budget_range":"unknown","legal_path":"/consent.html","legal_digest":"digest","submitted":true,"reference":"42"}"""))
            val status=repo.get(record)
            assertTrue(status.submitted);assertEquals("42",status.reference)
            assertEquals("POST",server.takeRequest().method)
            val get=server.takeRequest();assertEquals("GET",get.method);assertEquals("Bearer ${record.secret}",get.getHeader("Authorization"))
            assertFalse(record.toString().contains(record.secret));assertFalse(status.toString().contains("42"))
        } finally {server.shutdown()}
    }
}
