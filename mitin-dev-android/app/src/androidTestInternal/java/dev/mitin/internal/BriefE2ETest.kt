package dev.mitin.internal

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID
import java.security.SecureRandom
import android.util.Base64

private fun counts(): JsonObject = HttpAuthApi.secureClient().newCall(Request.Builder()
    .url("https://localhost:8443/test/brief-counts").build()).execute().use {
        check(it.isSuccessful); Json.parseToJsonElement(it.body.string()).jsonObject
    }
private fun count() = counts().getValue("leads").jsonPrimitive.int
private fun fresh(budget: String): BriefRecord {
    val id=UUID.randomUUID().toString()
    val secret=Base64.encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) },Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    val body=buildJsonObject { put("session_id",id);put("session_secret",secret);put("service","site");put("budget_range",budget) }
    return BriefRecord(id,secret,body,"sessions",body)
}
private fun action(record: BriefRecord,state: BriefState,path: String,extra: JsonObject=JsonObject(emptyMap())) = record.copy(
    pendingPath="sessions/${record.id}/$path",pendingBody=buildJsonObject {
        put("request_id",UUID.randomUUID().toString());put("revision",state.revision);extra.forEach { (k,v)->put(k,v) }
    })
private suspend fun prepared(api: HttpBriefRepository,record: BriefRecord): BriefState {
    var state=api.execute(record)
    state=api.execute(action(record,state,"messages",buildJsonObject {put("message","Нужен сайт для вымышленных тестовых услуг")}))
    assertTrue(state.messages.last().text.contains("?"))
    state=api.execute(action(record,state,"final"));assertNotNull(state.finalBrief)
    return api.execute(action(record,state,"prepare",buildJsonObject {
        put("name","Синтетический клиент");put("contact_type","email");put("contact","synthetic@example.com")
    }))
}
private fun confirm(record: BriefRecord,state: BriefState) = action(record,state,"confirm",buildJsonObject {
    put("proof",state.proof);put("consent",true);put("legal_digest",state.legalDigest)
})

class BriefNetworkE2ETest {
    @Test fun providerAndValidationFailuresNeverCreateLead() = runBlocking {
        val api=HttpBriefRepository("https://localhost:8443/");val record=fresh("under_10k");val before=count()
        var state=api.execute(record)
        suspend fun rejected(request: BriefRecord,status:Int) {
            val failure=runCatching {api.execute(request)}.exceptionOrNull()
            assertTrue(failure is BriefFailure);assertEquals(status,(failure as BriefFailure).status);assertEquals(before,count())
        }
        for((message,status) in listOf("TEST_TIMEOUT" to 504,"TEST_PROVIDER_ERROR" to 503,"x".repeat(4001) to 422)) {
            rejected(action(record,state,"messages",buildJsonObject {put("message",message)}),status)
        }
        state=api.execute(action(record,state,"messages",buildJsonObject {put("message","Синтетическая задача для MVP")}))
        state=api.execute(action(record,state,"final"))
        rejected(action(record,state,"prepare",buildJsonObject {put("name","Тест");put("contact_type","email");put("contact","invalid")}),422)
        state=api.execute(action(record,state,"prepare",buildJsonObject {put("name","Тест");put("contact_type","email");put("contact","synthetic@example.com")}))
        rejected(action(record,state,"confirm",buildJsonObject {put("proof",state.proof!!.dropLast(6)+"xxxxxx");put("consent",true);put("legal_digest",state.legalDigest)}),410)
        rejected(action(record,state,"confirm",buildJsonObject {put("proof",state.proof);put("consent",false);put("legal_digest",state.legalDigest)}),422)
        rejected(action(record,state,"confirm",buildJsonObject {put("proof",state.proof);put("consent",true);put("legal_digest",state.legalDigest);put("budget_range","70k_plus")}),422)
    }
    @Test fun sixBudgetsRealPostgresAndDuplicateConfirmation() = runBlocking {
        val api=HttpBriefRepository("https://localhost:8443/")
        briefBudgets.keys.forEach { budget ->
            val before=count();val record=fresh(budget);val state=prepared(api,record)
            assertEquals(before,count()); assertEquals(budget,state.budget)
            val request=confirm(record,state)
            val results=coroutineScope { (1..3).map { async { api.execute(request) } }.awaitAll() }
            assertTrue(results.all {it.submitted});assertEquals(1,results.map {it.reference}.distinct().size)
            assertEquals(before+1,count())
            assertEquals(results.first().reference,api.get(record).reference)
        }
    }
    @Test fun lostResponseAfterCommitResolvesExistingLead() = runBlocking {
        val api=HttpBriefRepository("https://localhost:8443/");val lost=HttpBriefRepository("https://localhost:8444/")
        val before=count();val record=fresh("unknown");val state=prepared(api,record)
        val request=confirm(record,state)
        val store=BriefStore(InstrumentationRegistry.getInstrumentation().targetContext)
        store.save(request)
        try { lost.execute(request);fail("Expected transport loss") } catch(_: BriefFailure) {}
        val restored=store.read()!!;val status=api.get(restored)
        assertTrue(status.submitted);assertEquals(before+1,count())
        assertEquals(status.reference,api.execute(restored).reference);assertEquals(before+1,count())
        assertEquals(counts()["leads"],counts()["mobile_crm_leads"])
        store.clear()
    }
}

class BriefUiE2ETest {
    @get:Rule val ui=createAndroidComposeRule<InternalActivity>()
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private fun shot(stage:String) {
        ui.waitForIdle()
        val device=UiDevice.getInstance(instrumentation)
        val label=InstrumentationRegistry.getArguments().getString("portfolioCase") ?: "brief"
        val file=File(instrumentation.targetContext.getExternalFilesDir(null),"brief-$label-$stage.png")
        assertTrue(device.takeScreenshot(file))
        device.executeShellCommand("mkdir -p /sdcard/Download/mitin-network")
        device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/mitin-network/${file.name}")
    }
    private fun waitFor(tag:String)=ui.waitUntil(60_000) {ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()}
    private fun tap(tag:String) {
        waitFor(tag)
        val node=ui.onNodeWithTag(tag)
        if(ui.onAllNodes(hasTestTag(tag) and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty()) node.performScrollTo()
        node.performClick();ui.waitForIdle()
    }
    @Test fun portfolioBriefReviewContactConfirmAndRecreation() {
        val before=runBlocking(Dispatchers.IO) {count()}
        tap("internal-nav-0");waitFor("portfolio-ritmassage");tap("portfolio-open-ritmassage");waitFor("portfolio-detail-title");tap("portfolio-discuss")
        waitFor("brief-start");tap("brief-start");waitFor("brief-message")
        ui.onNodeWithTag("brief-message").performScrollTo().performTextInput("Нужен сайт для синтетических клиентов, MVP и запись на услуги")
        tap("brief-send");waitFor("brief-finalize");tap("brief-finalize");waitFor("brief-to-contact")
        ui.onNodeWithTag("brief-final").performScrollTo();shot("final")
        val rotationDevice=UiDevice.getInstance(instrumentation)
        rotationDevice.setOrientationLeft();ui.waitForIdle()
        rotationDevice.setOrientationNatural();ui.waitForIdle()
        ui.activityRule.scenario.recreate();ui.waitForIdle();waitFor("brief-to-contact")
        tap("brief-to-contact");waitFor("brief-name")
        ui.onNodeWithTag("brief-name").performScrollTo().performTextInput("Синтетический клиент")
        ui.onNodeWithTag("brief-contact").performScrollTo().performTextInput("synthetic@example.com")
        UiDevice.getInstance(instrumentation).pressBack();ui.waitForIdle()
        tap("brief-prepare");waitFor("brief-consent")
        assertEquals(before,runBlocking(Dispatchers.IO) {count()})
        tap("brief-consent");tap("brief-confirm");waitFor("brief-new")
        assertEquals(before+1,runBlocking(Dispatchers.IO) {count()})
        val device=UiDevice.getInstance(instrumentation)
        val label=InstrumentationRegistry.getArguments().getString("portfolioCase") ?: "brief"
        val file=File(instrumentation.targetContext.getExternalFilesDir(null),"brief-$label.png")
        assertTrue(device.takeScreenshot(file));device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/mitin-network/${file.name}")
        tap("brief-new");waitFor("brief-start")
    }
}

/** The host removes/restores adb reverse around these separate OS-process phases. */
class BriefOfflineUiTest {
    @get:Rule val ui=createAndroidComposeRule<InternalActivity>()
    private fun waitFor(tag:String)=ui.waitUntil(60_000) {ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()}
    @Test fun persistStartWithoutNetwork() {
        waitFor("internal-nav-1")
        ui.onNodeWithTag("internal-nav-1").performClick();waitFor("brief-start")
        ui.onNodeWithTag("brief-start").performScrollTo().performClick();waitFor("brief-error")
        ui.onNodeWithTag("brief-retry").assertExists()
    }
    @Test fun resumePersistedStart() {
        waitFor("internal-nav-1")
        ui.onNodeWithTag("internal-nav-1").performClick();waitFor("brief-message")
        ui.onNodeWithTag("brief-reset").performScrollTo().performClick();waitFor("brief-start")
    }
}
