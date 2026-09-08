package dev.mitin.internal

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.mockwebserver.*
import okhttp3.tls.*
import org.junit.Assert.*
import org.junit.Test

class CabinetTransportTest {
    private fun tls(block:suspend(MockWebServer,CabinetApi)->Unit)=runBlocking {
        val cert=HeldCertificate.Builder().commonName("localhost").addSubjectAlternativeName("localhost").build()
        val serverTls=HandshakeCertificates.Builder().heldCertificate(cert).build()
        val trust=HandshakeCertificates.Builder().addTrustedCertificate(cert.certificate).build()
        val server=MockWebServer();server.useHttps(serverTls.sslSocketFactory(),false);server.start()
        val client=HttpAuthApi.secureClient().newBuilder().sslSocketFactory(trust.sslSocketFactory(),trust.trustManager).build()
        try {block(server,CabinetApi(server.url("/").toString(),client))} finally {server.shutdown()}
    }
    @Test fun lostMutationResponseNeverCausesAnAutomaticReplay()=tls {server,api->
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        val error=runCatching {api.call("projects/00000000-0000-4000-8000-000000000001/messages","POST",Secret("synthetic-access"),buildJsonObject{put("body","test")})}.exceptionOrNull()
        assertTrue(error is AuthFailure);assertEquals(1,server.requestCount)
        val request=server.takeRequest();assertEquals("POST",request.method);assertEquals("no-store",request.getHeader("Cache-Control"))
    }
    @Test fun redirectsCookiesAndCacheablePrivateResponsesAreRejected()=tls {server,api->
        for(response in listOf(MockResponse().setResponseCode(302).setHeader("Location","https://example.com"),MockResponse().setBody("{}").setHeader("Cache-Control","public"),MockResponse().setBody("{}").setHeader("Cache-Control","no-store").setHeader("Set-Cookie","session=synthetic"))) {
            server.enqueue(response);assertTrue(runCatching{api.call("projects",access=Secret("synthetic-access"))}.exceptionOrNull() is AuthFailure)
        }
        assertEquals(3,server.requestCount)
    }
    @Test fun oversizedJsonAndUnsafeOriginsAreRejected()=tls {server,api->
        server.enqueue(MockResponse().setHeader("Cache-Control","no-store").setBody("x".repeat(1024*1024+1)))
        assertTrue(runCatching{api.call("projects")}.exceptionOrNull() is AuthFailure)
        assertTrue(runCatching{CabinetApi("http://localhost/")}.exceptionOrNull() is AuthFailure)
        assertTrue(runCatching{api.call("../projects")}.isFailure)
        assertEquals(1,server.requestCount)
    }
}
