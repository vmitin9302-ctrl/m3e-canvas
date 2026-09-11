package dev.mitin.internal

import androidx.compose.ui.test.*
import dev.mitin.testing.replaceWhenReady
import dev.mitin.testing.settleWindow
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.semantics.SemanticsProperties
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
    private var iteration = 0
    private val vm get() = ViewModelProvider(ui.activity)[BriefViewModel::class.java]
    private fun evidence(stage: String) {
        val ime=imeVisible()
        val focused=ui.onAllNodes(hasTestTag("brief-message") and isFocused()).fetchSemanticsNodes().isNotEmpty()
        val snapshot = ui.runOnIdle {
            val value = vm.state
            buildJsonObject {
                put("stage", stage); put("iteration", iteration)
                put("route", if (value?.submitted == true) "submitted" else if (vm.contactStep) "contact" else if (value == null) "start" else "discussion")
                put("loading", vm.busy); put("has_error", vm.error != null)
                put("ime_visible", ime); put("composer_focused", focused)
                put("revision", value?.revision); put("acknowledged_revision", vm.acknowledgedRevision)
                put("message_count", value?.messages?.size ?: 0)
                put("user_messages", value?.messages?.count { it.role == "user" } ?: 0)
                put("ai_messages", value?.messages?.count { it.role == "assistant" } ?: 0)
                put("finalization_control_ready", value?.messages?.isNotEmpty() == true && !vm.busy)
                put("server_pending", value?.pending == true)
                put("has_final_document", value?.finalBrief != null)
                put("send_invocations", vm.messageInvocations)
                put("last_network_category", vm.lastNetworkCategory)
            }.toString()
        }
        val label = InstrumentationRegistry.getArguments().getString("portfolioCase") ?: "brief"
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "brief-$label-$iteration-$stage.json")
        file.writeText(snapshot)
        val device = UiDevice.getInstance(instrumentation)
        device.executeShellCommand("mkdir -p /sdcard/Download/mitin-network")
        device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/mitin-network/${file.name}")
        android.util.Log.i("MitinBriefValidation", snapshot)
    }
    private fun checkpoint(stage: String, condition: () -> Boolean) {
        try { ui.waitUntil(60_000, condition); evidence(stage) }
        catch (failure: Throwable) {
            runCatching { evidence("timeout-$stage"); shot("timeout-$stage") }
            throw AssertionError("Brief checkpoint failed: $stage (${failure.javaClass.simpleName})")
        }
    }
    private fun imeVisible():Boolean {
        var visible=false
        instrumentation.runOnMainSync {
            visible=androidx.core.view.ViewCompat.getRootWindowInsets(ui.activity.window.decorView)
                ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
        }
        return visible
    }
    private fun shot(stage:String) {
        ui.waitForIdle()
        val device=UiDevice.getInstance(instrumentation)
        val label=InstrumentationRegistry.getArguments().getString("portfolioCase") ?: "brief"
        val file=File(instrumentation.targetContext.getExternalFilesDir(null),"brief-$label-$stage.png")
        val raw=instrumentation.uiAutomation.takeScreenshot()
        assertNotNull(raw)
        val bitmap=raw.copy(android.graphics.Bitmap.Config.ARGB_8888,true)
        raw.recycle()
        val canvas=android.graphics.Canvas(bitmap)
        val paint=android.graphics.Paint().apply { color=android.graphics.Color.DKGRAY }
        for (tag in listOf("brief-private-content", "brief-message")) {
            ui.onAllNodesWithTag(tag).fetchSemanticsNodes().forEach { node ->
                val b=node.boundsInWindow
                canvas.drawRect(b.left,b.top,b.right,b.bottom,paint)
            }
        }
        instrumentation.runOnMainSync {
            val ime=androidx.core.view.ViewCompat.getRootWindowInsets(ui.activity.window.decorView)
                ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())?.bottom ?: 0
            if(ime > 0) canvas.drawRect(0f,(bitmap.height-ime).toFloat(),bitmap.width.toFloat(),bitmap.height.toFloat(),paint)
        }
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
        device.executeShellCommand("mkdir -p /sdcard/Download/mitin-network")
        device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/mitin-network/${file.name}")
    }
    private fun waitFor(tag:String) = checkpoint("waiting-$tag") {
        ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
    private fun tap(tag:String) {
        waitFor(tag)
        // Restored server state can render while its durable save is still in
        // progress. A visible disabled control must not be treated as clickable.
        ui.waitUntil(60_000) { ui.onAllNodes(hasTestTag(tag) and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        val node=ui.onNodeWithTag(tag)
        if(ui.onAllNodes(hasTestTag(tag) and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty()) node.performScrollTo()
        ui.waitForIdle()
        ui.settleWindow { ui.activity }
        node.assertIsDisplayed().assertIsEnabled().performClick();ui.waitForIdle()
    }
    @Test fun portfolioBriefReviewContactConfirmAndRecreation() {
        // The tabs exist only after a real server login; the brief itself stays anonymous server-side.
        val manager=(ui.activity.application as InternalApplication).manager!!
        runBlocking { withTimeout(60_000) { while(manager.state.value.restoring) delay(50) }
            if(manager.state.value.profile == null) manager.login(CLIENT_A,Secret(TEST_PASSWORD)) }
        waitFor("internal-nav-2")
        val label=InstrumentationRegistry.getArguments().getString("portfolioCase") ?: "brief"
        val repetitions=if(label == "brief-ui-360-1.0") 3 else 1
        repeat(repetitions) { index ->
            iteration=index+1
            journey(submit=iteration == repetitions)
        }
    }
    private fun journey(submit: Boolean) {
        val before=runBlocking(Dispatchers.IO) {count()}
        tap("internal-nav-2")
        // The portfolio ViewModel deliberately retains the selected case across tabs.
        if(iteration > 1) tap("portfolio-back")
        waitFor("portfolio-ritmassage");tap("portfolio-open-ritmassage");waitFor("portfolio-detail-title");tap("portfolio-discuss")
        waitFor("brief-start");tap("brief-start");waitFor("brief-message")
        ui.replaceWhenReady("brief-message", "Нужен сайт для синтетических клиентов, MVP и запись на услуги") { ui.activity }
        val revision=ui.runOnIdle { vm.state!!.revision }
        val invocations=ui.runOnIdle { vm.messageInvocations }
        evidence("input-ready")
        tap("brief-send")
        checkpoint("send-dispatched") {
            vm.messageInvocations == invocations+1 &&
                ui.onNodeWithTag("brief-message").fetchSemanticsNode().config[SemanticsProperties.EditableText].text.isEmpty()
        }
        checkpoint("message-acknowledged") {
            !vm.busy && vm.lastNetworkCategory == "success" && vm.state!!.revision > revision &&
                vm.acknowledgedRevision == vm.state!!.revision &&
                vm.state!!.messages.any { it.role == "user" } && vm.state!!.messages.any { it.role == "assistant" }
        }
        val record=runBlocking { BriefStore(instrumentation.targetContext).read()!! }
        val server=runBlocking { HttpBriefRepository("https://localhost:8443/").get(record) }
        ui.runOnIdle {
            assertEquals(server.revision,vm.state!!.revision)
            assertEquals(server.messages.size,vm.state!!.messages.size)
            assertFalse(server.pending)
        }
        evidence("server-verified")
        waitFor("brief-finalize");tap("brief-finalize");waitFor("brief-to-contact")
        checkpoint("final-document-acknowledged") { !vm.busy && vm.state?.finalBrief != null }
        ui.waitUntil(10_000) { !imeVisible() }
        ui.onNodeWithTag("brief-final").performScrollTo();shot("final")
        val rotationDevice=UiDevice.getInstance(instrumentation)
        android.util.Log.i("MitinBriefLifecycle", "rotate landscape")
        rotationDevice.setOrientationLeft()
        ui.waitUntil(30_000) { instrumentation.targetContext.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }
        ui.waitForIdle();waitFor("brief-to-contact")
        android.util.Log.i("MitinBriefLifecycle", "rotate portrait")
        rotationDevice.setOrientationNatural()
        ui.waitUntil(30_000) { instrumentation.targetContext.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT }
        ui.waitForIdle();waitFor("brief-to-contact")
        // Repeated teardown covers the SlotTable disposal regression, without retries.
        repeat(2) { cycle ->
            android.util.Log.i("MitinBriefLifecycle", "recreate ${cycle + 1}")
            ui.activityRule.scenario.recreate();ui.waitForIdle();waitFor("brief-to-contact")
        }
        tap("brief-to-contact");waitFor("brief-name")
        ui.replaceWhenReady("brief-name", "Синтетический клиент") { ui.activity }
        ui.replaceWhenReady("brief-contact", "synthetic@example.com") { ui.activity }
        ui.waitUntil(10_000) { imeVisible() }
        UiDevice.getInstance(instrumentation).pressBack()
        ui.waitUntil(10_000) { !imeVisible() };ui.waitForIdle()
        if (!submit) {
            evidence("repeated-journey-pass")
            tap("brief-discuss");tap("brief-reset");waitFor("brief-start")
            return
        }
        tap("brief-prepare");waitFor("brief-consent")
        assertEquals(before,runBlocking(Dispatchers.IO) {count()})
        tap("brief-consent");tap("brief-confirm");waitFor("brief-new")
        assertEquals(before+1,runBlocking(Dispatchers.IO) {count()})
        val device=UiDevice.getInstance(instrumentation)
        val label=InstrumentationRegistry.getArguments().getString("portfolioCase") ?: "brief"
        val file=File(instrumentation.targetContext.getExternalFilesDir(null),"brief-$label.png")
        assertTrue(device.takeScreenshot(file));device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/mitin-network/${file.name}")
        evidence("complete-journey-pass")
        tap("brief-new");waitFor("brief-start")
    }
}

/** The host removes/restores adb reverse around these separate OS-process phases. */
class BriefOfflineUiTest {
    @get:Rule val ui=createAndroidComposeRule<InternalActivity>()
    private val manager get()=(ui.activity.application as InternalApplication).manager!!
    private fun waitFor(tag:String)=ui.waitUntil(60_000) {ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()}
    private fun tap(tag:String) {
        waitFor(tag)
        val node=ui.onNodeWithTag(tag)
        if(ui.onAllNodes(hasTestTag(tag) and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty()) node.performScrollTo()
        node.performClick();ui.waitForIdle()
    }
    private fun hideIme() {
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        var visible=false
        ui.runOnUiThread { visible=androidx.core.view.ViewCompat.getRootWindowInsets(ui.activity.window.decorView)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())==true }
        if(visible)device.pressBack()
        ui.waitForIdle()
    }
    /** A fresh install defaults to registration mode; switch explicitly before a login flow. */
    private fun ensureLoginMode() {
        if(ui.onAllNodesWithTag("entry-login").fetchSemanticsNodes().isNotEmpty()) tap("entry-login")
        waitFor("cabinet-password")
    }
    /** Offline process start: no session can be restored, so only native login is shown and a login attempt fails safely. */
    @Test fun persistStartWithoutNetwork() {
        waitFor("cabinet-email")
        ui.onNodeWithTag("internal-navigation-bar").assertDoesNotExist()
        ensureLoginMode()
        ui.replaceWhenReady("cabinet-email",CLIENT_A){ui.activity};ui.replaceWhenReady("cabinet-password",TEST_PASSWORD){ui.activity};hideIme()
        tap("cabinet-auth-submit")
        ui.waitUntil(60_000) { ui.onAllNodesWithText("Сервис временно недоступен. Проверьте подключение.").fetchSemanticsNodes().isNotEmpty() }
        assertNull(manager.state.value.profile)
        ui.onNodeWithTag("internal-navigation-bar").assertDoesNotExist()
        waitFor("cabinet-auth-submit")
    }
    /** Fresh OS process online: the same install logs in and reaches the AI brief home tab. */
    @Test fun resumePersistedStart() {
        waitFor("cabinet-email")
        ensureLoginMode()
        ui.replaceWhenReady("cabinet-email",CLIENT_A){ui.activity};ui.replaceWhenReady("cabinet-password",TEST_PASSWORD){ui.activity};hideIme()
        tap("cabinet-auth-submit")
        ui.waitUntil(60_000){manager.state.value.profile != null}
        waitFor("internal-nav-0");ui.onNodeWithTag("internal-nav-0").assertIsSelected()
        waitFor("brief-start")
        ui.onNodeWithTag("brief-start").performScrollTo().performClick();waitFor("brief-message")
        ui.activityRule.scenario.recreate();ui.waitForIdle();waitFor("brief-message")
        ui.onNodeWithTag("brief-reset").performScrollTo().performClick();waitFor("brief-start")
        runBlocking { manager.logout() };waitFor("cabinet-email")
    }
}
