package dev.mitin.internal

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.By
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ProductionValidationTest {
    @get:Rule val ui = createAndroidComposeRule<InternalActivity>()
    private val inst get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(inst)
    private val context get() = inst.targetContext
    private fun dismissUnrelatedLauncherDialog() {
        // API 35's bundled Pixel Launcher may ANR on a freshly booted headless emulator.
        // Never dismiss an MITIN DEV failure or any other application dialog.
        if (device.findObject(By.text("Pixel Launcher isn't responding")) != null)
            device.findObject(By.text("Close app"))?.click()
    }
    private fun waitFor(tag: String) = ui.waitUntil(75_000) {
        dismissUnrelatedLauncherDialog()
        // Returning from Chrome temporarily leaves no resumed Compose root.
        runCatching { ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
    }
    private fun tap(tag: String) {
        waitFor(tag)
        ui.waitUntil(75_000) { ui.onAllNodes(hasTestTag(tag) and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        val node = ui.onNodeWithTag(tag)
        if (ui.onAllNodes(hasTestTag(tag) and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty()) node.performScrollTo()
        ui.waitForIdle(); node.performClick(); ui.waitForIdle()
    }
    private fun vm(): BriefViewModel {
        lateinit var value: BriefViewModel
        inst.runOnMainSync { value = ViewModelProvider(ui.activity)[BriefViewModel::class.java] }
        return value
    }
    private fun stable() = ui.waitUntil(75_000) { !vm().busy }
    private fun shot(name: String) {
        dismissUnrelatedLauncherDialog()
        ui.waitForIdle()
        val frame = CountDownLatch(1)
        inst.runOnMainSync {
            val decor = ui.activity.window.decorView
            decor.viewTreeObserver.registerFrameCommitCallback { frame.countDown() }; decor.invalidate()
        }
        assertTrue("Frame not committed", frame.await(10, TimeUnit.SECONDS))
        device.waitForIdle()
        val file = File(context.getExternalFilesDir(null), "$name.png")
        assertTrue(device.takeScreenshot(file))
        device.executeShellCommand("mkdir -p /sdcard/Download/mitin-production")
        device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/mitin-production/${file.name}")
    }
    private fun requireSignedIn() {
        val manager = (context.applicationContext as InternalApplication).manager!!
        ui.waitUntil(75_000) { !manager.state.value.restoring }
        // Tabs exist only after login. This suite never creates production accounts or sessions.
        org.junit.Assume.assumeTrue("Requires an already authenticated production session", manager.state.value.profile != null)
    }
    @Test fun realProductionThreeBriefsNoSubmission() {
        assertEquals("yes", InstrumentationRegistry.getArguments().getString("authorizedProductionAi"))
        requireSignedIn()
        tap("internal-nav-2"); waitFor("portfolio-ritmassage")
        val caps = (context.applicationContext as InternalApplication).capabilities!!
        assertTrue(caps.ai); assertFalse(caps.submission); assertTrue(caps.auth)
        ui.onNodeWithText("DIVEEV STUDIO").assertExists()
        ui.waitUntil(30_000) { ui.onAllNodesWithTag("portfolio-image").fetchSemanticsNodes().size == 2 }
        shot("catalog")
        val reports = mutableListOf<JsonObject>()
        val cases = listOf(null to "under_10k", "diveev-studio" to "10_20k", "ritmassage" to "unknown")
        for ((slug, budget) in cases) {
            if (slug != null) {
                tap("internal-nav-2")
                if (ui.onAllNodesWithTag("portfolio-back").fetchSemanticsNodes().isNotEmpty()) tap("portfolio-back")
                tap("portfolio-open-$slug"); waitFor("portfolio-detail-title"); waitFor("portfolio-image")
                ui.onNodeWithTag("portfolio-site").performScrollTo().assertIsDisplayed(); shot("case-$slug")
                tap("portfolio-site")
                ui.waitUntil(10_000) { device.currentPackageName != context.packageName && device.currentPackageName != null }
                assertNotEquals("External browser did not open", context.packageName, device.currentPackageName)
                // Bring the existing activity back even if Chrome is showing its first-run screen.
                context.startActivity(android.content.Intent(context, InternalActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                waitFor("portfolio-discuss")
                tap("portfolio-discuss")
            } else tap("internal-nav-0")
            tap("brief-service-site"); tap("brief-budget-$budget"); shot("selection-$budget"); tap("brief-start"); stable()
            waitFor("brief-message")
            assertEquals(budget, vm().state!!.budget); assertEquals(slug, vm().state!!.portfolio)
            val messages = listOf(
                "Это проверка приложения на вымышленном проекте. Нужен сайт небольшой студии керамики: показать услуги и работы, объяснить цены и дать ссылку на запись. Аудитория — жители города. На первом этапе достаточно простого MVP без оплаты и личного кабинета.",
                "Основные функции: главная, услуги, галерея и ссылка на запись. Тексты и фотографии подготовим сами. Срок пока не согласован. Учти выбранный мной бюджет как ориентир, выдели MVP и возможные сторонние расходы. ${if(slug!=null) "Кейс используем только как направление, не копируем его дизайн или материалы." else "Развитие можно вынести на следующий этап."}"
            )
            val scenarioMessages = messages.mapIndexed { index, text ->
                if (index != 0) text else text + when (slug) {
                    "diveev-studio" -> " Я самозанятый."
                    "ritmassage" -> " Возможно, позже добавим форму с именем и телефоном; состав данных и инфраструктура пока не определены."
                    else -> ""
                }
            }
            for (message in scenarioMessages) {
                val input = ui.onNodeWithTag("brief-message")
                input.performClick()
                Thread.sleep(800); ui.waitForIdle()
                input.performTextReplacement(message); ui.waitForIdle(); input.assertTextContains(message)
                ui.onNodeWithTag("brief-send").assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick) { it() }
                if (budget == "under_10k" && message == messages.last()) {
                    device.executeShellCommand("cmd connectivity airplane-mode enable")
                    device.executeShellCommand("svc wifi disable"); device.executeShellCommand("svc data disable")
                    stable()
                    device.executeShellCommand("cmd connectivity airplane-mode disable")
                    device.executeShellCommand("svc wifi enable"); device.executeShellCommand("svc data enable")
                    Thread.sleep(4000)
                    if (vm().error != null) tap("brief-retry")
                }
                stable(); assertNull("AI request failed", vm().error)
                assertTrue("Sent user message missing from acknowledged server history", vm().state!!.messages.any { it.role == "user" && it.text == message })
            }
            shot("chat-$budget")
            tap("brief-finalize"); stable(); assertNull("Final generation failed", vm().error)
            val state = vm().state!!
            assertFalse(state.submitted); assertNull(state.reference)
            val final = state.finalBrief ?: error("No final brief")
            assertTrue("Final brief empty", final.length > 100)
            ui.onNodeWithTag("brief-final").performScrollTo().assertExists(); shot("final-$budget")
            reports += buildJsonObject {
                put("budget", budget); put("source_portfolio_slug", slug); put("submitted", state.submitted)
                put("assistant_turns", state.messages.count { it.role != "user" }); put("final_characters", final.length)
                put("user_messages_acknowledged", scenarioMessages.all { sent -> state.messages.any { it.role == "user" && it.text == sent } })
                put("synthetic_final_brief", final)
                put("final_sections", JsonArray(state.sections.map { JsonPrimitive(it.title) }))
                put("assistant_excerpt", state.messages.filter { it.role != "user" }.joinToString("\n---\n") { it.text.take(1500) })
                val lines = final.lines()
                val indices = lines.indices.filter { index -> listOf("бюджет","MVP","этап","расход","стоим","срок","следующ").any { lines[index].contains(it, true) } }
                    .flatMap { index -> (index..minOf(index + 3, lines.lastIndex)).toList() }.distinct().sorted()
                put("budget_mvp_excerpt", indices.joinToString("\n") { lines[it] }.take(3000))
            }
            tap("brief-to-contact"); waitFor("submission-disabled"); tap("brief-submission-check"); stable()
            waitFor("brief-error"); assertFalse(vm().state!!.submitted); assertNull(vm().state!!.reference)
            ui.onNodeWithText("Заявка отправлена").assertDoesNotExist(); shot("submission-closed-$budget")
            writeReport(reports)
            if (budget != "unknown") { tap("brief-reset"); stable(); waitFor("brief-start") }
        }
    }

    private fun writeReport(reports: List<JsonObject>) {
        val report = File(context.getExternalFilesDir(null), "production-evidence.json")
        report.writeText(buildJsonObject {
            put("ai",true); put("submission",false); put("client_auth",false); put("lead_submission_requests",0)
            put("scenarios",JsonArray(reports))
        }.toString())
        device.executeShellCommand("cp ${report.absolutePath} /sdcard/Download/mitin-production/production-evidence.json")
    }

    @Test fun retainedRealBriefMatrix() {
        requireSignedIn()
        waitFor("internal-nav-0"); tap("internal-nav-0"); stable(); waitFor("brief-to-contact")
        assertTrue(vm().state!!.finalBrief!!.length > 100)
        val label = InstrumentationRegistry.getArguments().getString("matrix") ?: "matrix"
        ui.onNodeWithTag("brief-message").performClick()
        ui.waitUntil(10_000) { ui.activity.window.decorView.rootWindowInsets?.isVisible(android.view.WindowInsets.Type.ime()) == true }
        ui.waitForIdle()
        ui.onNodeWithTag("brief-message").performTextReplacement("Черновик без отправки")
        Thread.sleep(800); ui.waitForIdle(); ui.onNodeWithTag("brief-message").assertIsDisplayed()
        ui.onNodeWithTag("brief-message").assertTextContains("Черновик без отправки").assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(64f))
        ui.onNodeWithTag("brief-send").assertIsDisplayed().assertIsEnabled().assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(48f))
        shot("$label-keyboard"); device.pressBack(); ui.waitForIdle()
        waitFor("internal-nav-0")
        ui.onNodeWithTag("brief-message").assertTextContains("Черновик без отправки").performClick()
        Thread.sleep(800); ui.onNodeWithTag("brief-send").assertIsDisplayed()
        ui.activityRule.scenario.recreate(); ui.waitForIdle(); stable()
        ui.onNodeWithTag("brief-message").assertTextContains("Черновик без отправки")
        // Recreation may restore focus without reopening the IME. Explicitly
        // open it before testing Back; otherwise Back correctly leaves the tab.
        ui.onNodeWithTag("brief-message").performClick()
        ui.waitUntil(10_000) { ui.activity.window.decorView.rootWindowInsets?.isVisible(android.view.WindowInsets.Type.ime()) == true }
        device.pressBack(); ui.waitForIdle()
        waitFor("brief-final")
        ui.onNodeWithTag("brief-final").performScrollTo(); shot("$label-final")
        tap("brief-to-contact"); waitFor("submission-disabled"); shot("$label-closed")
        ui.activityRule.scenario.recreate(); ui.waitForIdle(); stable()
        tap("internal-nav-3"); waitFor("cabinet")
        tap("internal-nav-2"); waitFor("portfolio-ritmassage")
        tap("portfolio-open-ritmassage"); waitFor("portfolio-detail-title"); device.pressBack(); ui.waitForIdle(); waitFor("portfolio-ritmassage")
    }
}
