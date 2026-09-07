package dev.mitin.internal

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test

class ProductionOfflineTest {
    @get:Rule val ui = createAndroidComposeRule<InternalActivity>()
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
