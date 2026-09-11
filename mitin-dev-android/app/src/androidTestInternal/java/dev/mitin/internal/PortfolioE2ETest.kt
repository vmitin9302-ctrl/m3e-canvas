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

/** Real HTTPS -> FastAPI -> site's PROJECTS; no mock server in this test.
 * The catalog lives on the signed-in Cases tab; signed-out installs only see native login. */
class PortfolioE2ETest {
    @get:Rule val ui = createAndroidComposeRule<InternalActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(instrumentation)
    private fun waitFor(tag: String) = ui.waitUntil(30_000) { ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun tap(tag: String) {
        waitFor(tag)
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
    @Test fun catalogAfterLoginDetailImagesRotationAndSessionIndependence() {
        val manager = (instrumentation.targetContext.applicationContext as InternalApplication).manager!!
        runBlocking { withTimeout(30_000) { while (manager.state.value.restoring) delay(50) }; manager.logout() }
        assertNull(manager.state.value.profile)
        // Signed out: native login only, no tab bar and no catalog.
        waitFor("cabinet-email")
        ui.onNodeWithTag("internal-navigation-bar").assertDoesNotExist()
        ui.onAllNodesWithTag("portfolio-ritmassage").assertCountEquals(0)
        runBlocking { manager.login(CLIENT_A, Secret(TEST_PASSWORD)) }
        waitFor("internal-nav-2")
        ui.onNodeWithTag("internal-nav-0").assertIsSelected()
        tap("internal-nav-2"); waitFor("portfolio-ritmassage")
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
        waitFor("portfolio-detail-title")
        ui.onNodeWithTag("internal-nav-2").assertIsSelected()
        device.pressBack(); ui.waitForIdle(); waitFor("portfolio-ritmassage")
        tap("portfolio-open-diveev-studio"); waitFor("portfolio-detail-title")
        ui.onNodeWithTag("portfolio-detail-title").assertTextEquals("DIVEEV STUDIO")
        // "Discuss a similar project" lands on the AI brief tab, not the audit.
        tap("portfolio-discuss"); ui.onNodeWithText("Что хотите создать?").assertExists()
        ui.onNodeWithTag("internal-nav-0").assertIsSelected()
        tap("internal-nav-2"); tap("portfolio-back"); tap("portfolio-refresh"); waitFor("portfolio-ritmassage")
        runBlocking { manager.logout() }
        waitFor("cabinet-email"); assertNull(manager.state.value.profile)
        ui.onNodeWithTag("internal-navigation-bar").assertDoesNotExist()
        // Another account sees the same public catalog; the catalog is not account data.
        runBlocking { manager.login(CLIENT_B, Secret(TEST_PASSWORD)) }
        waitFor("internal-nav-2"); tap("internal-nav-2"); waitFor("portfolio-ritmassage")
        ui.onNodeWithText("DIVEEV STUDIO").assertExists()
        runBlocking { manager.logout() }
        waitFor("cabinet-email")
    }
}
