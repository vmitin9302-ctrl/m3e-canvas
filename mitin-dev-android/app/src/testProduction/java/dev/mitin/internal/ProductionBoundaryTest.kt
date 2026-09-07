package dev.mitin.internal

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ProductionBoundaryTest {
    @Test fun capabilitiesDefaultClosedAndParseActualContract() {
        val json = Json { ignoreUnknownKeys = true }
        val meta = json.decodeFromString<ServiceMeta>("""{"api_version":"v1","service":"MITIN DEV","capabilities":{"mobile_ai_brief":true,"mobile_lead_submission":false,"client_auth":false,"projects":false}}""")
        assertTrue(meta.capabilities.ai); assertFalse(meta.capabilities.submission); assertFalse(meta.capabilities.auth)
        assertEquals(Capabilities(), json.decodeFromString<Capabilities>("{}"))
    }
    @Test fun productionOriginHasNoTestFallback() {
        for (url in listOf("http://24promtbot.ru/", "https://localhost:8443/", "https://127.0.0.1/", "https://evil.test/"))
            assertTrue(runCatching { HttpAuthApi.checkedOrigin(url) }.isFailure)
        assertEquals("24promtbot.ru", HttpAuthApi.checkedOrigin("https://24promtbot.ru/").host)
    }
    @Test fun contactAndConfirmationAreBlockedBeforeNetwork() = runBlocking {
        val api = HttpBriefRepository("https://24promtbot.ru/")
        for (path in listOf("prepare", "confirm")) {
            val record = BriefRecord("00000000-0000-4000-8000-000000000001", "synthetic-test-only", JsonObject(emptyMap()),
                "sessions/00000000-0000-4000-8000-000000000001/$path", JsonObject(emptyMap()))
            val e = runCatching { api.execute(record) }.exceptionOrNull()
            assertTrue(e is BriefFailure); assertEquals(403, (e as BriefFailure).status)
        }
    }
}
