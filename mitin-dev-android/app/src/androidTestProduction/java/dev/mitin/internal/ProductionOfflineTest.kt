package dev.mitin.internal

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.After
import androidx.lifecycle.ViewModelProvider
import java.io.File

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
        Thread.sleep(4000)
        ui.onNodeWithTag("startup-retry").performClick()
        ui.waitUntil(35_000) { ui.onAllNodesWithTag("portfolio-ritmassage").fetchSemanticsNodes().isNotEmpty() }
    }
}
