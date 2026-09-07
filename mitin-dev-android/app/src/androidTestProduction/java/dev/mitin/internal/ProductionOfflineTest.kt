package dev.mitin.internal

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.After
import androidx.lifecycle.ViewModelProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay

class ProductionOfflineTest {
    @get:Rule val ui = createAndroidComposeRule<InternalActivity>()
    @After fun diagnostics() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(inst)
        inst.runOnMainSync {
            val meta = ViewModelProvider(ui.activity)[MetaViewModel::class.java]
            println("Metadata loading=${meta.loading}, failed=${meta.failed}; foreground=${device.currentPackageName}")
        }
        val file = File(inst.targetContext.getExternalFilesDir(null), "offline-result.png")
        device.takeScreenshot(file)
        device.executeShellCommand("mkdir -p /sdcard/Download/mitin-production")
        device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/mitin-production/offline-result.png")
    }
    @Test fun offlineStartAndRetry() {
        ui.waitUntil(35_000) { ui.onAllNodesWithTag("startup-retry").fetchSemanticsNodes().isNotEmpty() }
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("cmd connectivity airplane-mode disable")
        device.executeShellCommand("svc wifi enable")
        device.executeShellCommand("svc data enable")
        // Enabling the radio is asynchronous; wait for real DNS/TLS connectivity before testing Retry.
        runBlocking {
            var restored = false
            for (attempt in 1..3) {
                if (runCatching { MetaRepository().load() }.isSuccess) { restored = true; break }
                delay(2000)
            }
            check(restored) { "Emulator network did not recover after airplane mode" }
        }
        ui.onNodeWithTag("startup-retry").performSemanticsAction(SemanticsActions.OnClick) { it() }
        ui.waitUntil(35_000) { ui.onAllNodesWithTag("portfolio-ritmassage").fetchSemanticsNodes().isNotEmpty() }
    }
}
