package dev.mitin.internal

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.mitin.demo.MitinTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Actual composer + ViewModel, deterministic long replies; no HTTP or provider. */
class ProductionComposerTest {
    @get:Rule val ui = createComposeRule()
    @Test fun longConversationRemainsEditableAfterResponse() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val app = inst.targetContext.applicationContext as InternalApplication
        app.capabilities = Capabilities(true, false, false)
        runBlocking { BriefStore(app).clear() }
        var sent = 0
        val repository = object : BriefRepository {
            override suspend fun get(record: BriefRecord) = state(record)
            override suspend fun execute(record: BriefRecord): BriefState {
                if (record.pendingPath?.endsWith("messages") == true) sent++
                return state(record)
            }
            fun state(record: BriefRecord) = BriefState(record.id, sent + 1, "2099-01-01", "site", "unknown",
                messages = listOf(BriefMessage("assistant", "Описание проекта и возможных этапов. ".repeat(200))),
                legalPath = "/consent.html", legalDigest = "synthetic")
        }
        lateinit var vm: BriefViewModel
        ui.runOnUiThread { vm = BriefViewModel(app, repository) }
        ui.setContent { MitinTheme { Box(Modifier.fillMaxSize().imePadding().safeDrawingPadding()) { BriefScreen(vm) } } }
        ui.waitUntil(10_000) { !vm.busy }
        ui.onNodeWithTag("brief-start").performScrollTo().performClick()
        ui.waitUntil(10_000) { vm.state != null && !vm.busy }
        val input = ui.onNodeWithTag("brief-message")
        val send = ui.onNodeWithTag("brief-send")
        repeat(2) {
            input.performClick()
            Thread.sleep(800)
            input.performTextReplacement("Длинный черновик для проверки доступности поля. ".repeat(8))
            input.assertIsDisplayed().assertHeightIsAtLeast(64.dp)
            send.assertIsDisplayed().assertIsEnabled().performClick()
            ui.waitUntil(10_000) { sent == it + 1 && !vm.busy }
            input.assertIsDisplayed()
            UiDevice.getInstance(inst).pressBack()
            ui.waitForIdle()
        }
        runBlocking { BriefStore(app).clear() }
    }
}
