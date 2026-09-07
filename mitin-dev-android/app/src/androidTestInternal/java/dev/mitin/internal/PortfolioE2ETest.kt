package dev.mitin.internal

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Real HTTPS -> FastAPI -> site's PROJECTS; no mock server in this test. */
class PortfolioE2ETest {
    @get:Rule val ui = createAndroidComposeRule<InternalActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(instrumentation)
    private fun waitFor(tag: String) = ui.waitUntil(30_000) { ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun tap(tag: String) {
        val node = ui.onNodeWithTag(tag)
        if (ui.onAllNodes(hasTestTag(tag) and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty()) node.performScrollTo()
        node.performClick(); ui.waitForIdle()
    }
    private fun shot(name: String) {
        ui.waitForIdle(); device.waitForIdle()
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "portfolio-shots"); dir.mkdirs()
        val label = InstrumentationRegistry.getArguments().getString("portfolioCase") ?: "default"
        val file = File(dir, "portfolio-$label-$name.png")
        assertTrue(device.takeScreenshot(file))
        device.executeShellCommand("mkdir -p /sdcard/Download/mitin-network")
        device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/mitin-network/${file.name}")
    }
    @Test fun publicCatalogBeforeLoginDetailImagesRotationAndSessionIndependence() {
        val manager = (instrumentation.targetContext.applicationContext as InternalApplication).manager!!
        runBlocking { withTimeout(30_000) { while (manager.state.value.restoring) delay(50) }; manager.logout() }
        assertNull(manager.state.value.profile)
        tap("internal-nav-0"); waitFor("portfolio-ritmassage")
        ui.onNodeWithText("РиТМассаж").assertExists()
        ui.onNodeWithText("DIVEEV STUDIO").assertExists()
        shot("list")
        tap("portfolio-open-ritmassage"); waitFor("portfolio-detail-title")
        ui.onNodeWithTag("portfolio-detail-title").assertTextEquals("РиТМассаж")
        ui.onNodeWithTag("portfolio-detail-description").assertTextContains("Система для частного массажиста", substring = true)
        waitFor("portfolio-image")
        ui.onNodeWithTag("portfolio-image").performScrollTo().assertIsDisplayed(); shot("cover")
        ui.onNodeWithTag("portfolio-site").performScrollTo().assertIsDisplayed(); shot("detail")
        ui.activityRule.scenario.recreate(); ui.waitForIdle()
        // The existing tab defaults to Cabinet after recreation; re-enter the retained portfolio VM.
        tap("internal-nav-0"); waitFor("portfolio-detail-title")
        device.pressBack(); ui.waitForIdle(); waitFor("portfolio-ritmassage")
        tap("portfolio-open-diveev-studio"); waitFor("portfolio-detail-title")
        ui.onNodeWithTag("portfolio-detail-title").assertTextEquals("DIVEEV STUDIO")
        tap("portfolio-discuss"); ui.onNodeWithText("AI-бриф — следующий этап").assertExists()
        runBlocking { manager.login(CLIENT_A, Secret(TEST_PASSWORD)) }
        assertNotNull(manager.state.value.profile)
        tap("internal-nav-0"); tap("portfolio-back"); tap("portfolio-refresh"); waitFor("portfolio-ritmassage")
        runBlocking { manager.logout() }
        tap("portfolio-refresh"); waitFor("portfolio-ritmassage")
        assertNull(manager.state.value.profile)
        tap("internal-nav-2"); waitFor("login-email")
    }
}
