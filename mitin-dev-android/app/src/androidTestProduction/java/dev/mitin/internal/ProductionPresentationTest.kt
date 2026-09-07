package dev.mitin.internal

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.mitin.demo.MitinTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ProductionPresentationTest {
    @get:Rule val ui = createComposeRule()
    @Test fun launchAndSafeErrorStatesWithoutProductionRequests() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val app = inst.targetContext.applicationContext as InternalApplication
        app.capabilities = Capabilities(true,false,false)
        var current by mutableStateOf<BriefViewModel?>(null)
        ui.setContent { MitinTheme {
            if(current == null) LaunchScreen()
            else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(androidx.compose.ui.unit.Dp(20f))) { BriefScreen(current!!) }
        } }
        ui.onNodeWithTag("launch-screen").assertIsDisplayed(); ui.waitForIdle()
        val device = UiDevice.getInstance(inst)
        val file = File(inst.targetContext.getExternalFilesDir(null), "launch.png")
        device.waitForIdle(); assertTrue(device.takeScreenshot(file))
        device.executeShellCommand("mkdir -p /sdcard/Download/mitin-production")
        device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/mitin-production/launch.png")
        for ((code, text) in listOf(0 to "Нет соединения", 504 to "AI не успел", 401 to "Сессия недоступна", 410 to "Срок сессии", 429 to "Слишком много запросов", 503 to "Не удалось подтвердить")) {
            runBlocking { BriefStore(app).clear() }
            val repository = object : BriefRepository {
                override suspend fun get(record: BriefRecord): BriefState = throw BriefFailure(code)
                override suspend fun execute(record: BriefRecord): BriefState = throw BriefFailure(code)
            }
            ui.runOnIdle { current = BriefViewModel(app, repository) }
            ui.waitUntil(10_000) { current?.busy == false }; ui.onNodeWithTag("brief-start").performScrollTo().performClick()
            ui.waitUntil(10_000) { ui.onAllNodesWithTag("brief-error").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText(text, substring = true).assertExists()
            ui.onNodeWithTag("brief-retry").performScrollTo().performClick()
            ui.waitUntil(10_000) { current?.busy == false }; ui.onNodeWithText("Заявка отправлена").assertDoesNotExist()
        }
        runBlocking { BriefStore(app).clear() }
    }
}
