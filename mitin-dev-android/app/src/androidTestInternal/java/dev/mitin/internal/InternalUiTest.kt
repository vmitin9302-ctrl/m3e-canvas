package dev.mitin.internal

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class InternalUiTest {
    @get:Rule val ui=createAndroidComposeRule<InternalActivity>()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val device get()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val large get()=InstrumentationRegistry.getArguments().getString("largeFont")=="true"
    private val manager get()=(context.applicationContext as InternalApplication).manager!!
    @Before fun reset() {
        runBlocking { withTimeout(30_000) {while(manager.state.value.restoring) delay(50)}; manager.logout(); manager.restore() }
        ui.waitForIdle();tap("internal-nav-2")
    }
    private fun tap(tag:String) {
        val node=ui.onNodeWithTag(tag)
        if(ui.onAllNodes(hasTestTag(tag) and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty()) node.performScrollTo()
        node.performClick();ui.waitForIdle()
    }
    private fun waitFor(tag:String) {ui.waitUntil(30_000){ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()}}
    private fun shot(name:String) {
        ui.waitForIdle();device.waitForIdle()
        val dir=File(context.getExternalFilesDir(null),"network-shots").apply{mkdirs()}
        val file=File(dir,"${if(large) "large-" else ""}$name.png")
        assertTrue(device.takeScreenshot(file))
        device.executeShellCommand("mkdir -p /sdcard/Download/mitin-network")
        device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/mitin-network/${file.name}")
        assertEquals(file.length(),device.executeShellCommand("stat -c %s /sdcard/Download/mitin-network/${file.name}").trim().toLong())
    }
    private fun login(email:String) {
        waitFor("login-email")
        ui.onNodeWithTag("login-email").performScrollTo().performTextReplacement(email)
        ui.onNodeWithTag("login-password").performScrollTo().performTextReplacement(TEST_PASSWORD)
        ui.onNodeWithTag("network-login").assertIsEnabled()
        tap("network-login");waitFor("server-profile")
    }
    @Test fun realLoginProfileSessionsRevocationLogoutAllAndNewAccount() {
        if(large) assertTrue(context.resources.configuration.fontScale>=1.5f)
        shot("01-network-login")
        login(CLIENT_A);shot("02-server-profile-a")
        // A second independent real server session is created outside this app manager.
        runBlocking { HttpAuthApi("https://localhost:8443/").login(CLIENT_A,Secret(TEST_PASSWORD),701) }
        tap("open-sessions");waitFor("session-0");shot("03-real-sessions")
        tap("revoke-0");tap("confirm-revoke")
        ui.waitUntil(30_000) { ui.onAllNodes(hasText("Отозвана:", substring=true)).fetchSemanticsNodes().isNotEmpty() }
        // Most recently created second session is item 0; current app stays signed in.
        assertNotNull(manager.state.value.profile)
        shot("04-session-revoked")
        tap("logout-all");tap("confirm-logout-all");waitFor("login-email")
        assertEquals("Все сессии отозваны на сервере.", manager.state.value.message)
        shot("05-server-logout-all")
        val previousActivity=ui.activity
        device.pressBack()
        // Android 12+ backgrounds a launcher root on Back; older versions finish it.
        // Observe either real lifecycle result, then reopen as a launcher would.
        ui.waitUntil(30_000) {
            var backgrounded=false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val stage=ActivityLifecycleMonitorRegistry.getInstance().getLifecycleStageOf(previousActivity)
                backgrounded=stage==Stage.STOPPED || stage==Stage.DESTROYED
            }
            backgrounded
        }
        device.executeShellCommand("am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n dev.mitin.app.internal/dev.mitin.internal.InternalActivity")
        ui.waitUntil(30_000) {
            var resumed=false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                resumed=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .any { it is InternalActivity && !it.isFinishing }
            }
            resumed
        }
        ui.waitForIdle()
        assertNull(manager.state.value.profile)
        waitFor("login-email")
        login(CLIENT_B);shot("06-server-profile-b")
        ui.onNodeWithText("Тестовый клиент Б").assertExists()
        ui.onNodeWithText("Тестовый клиент А").assertDoesNotExist()
        tap("network-logout");waitFor("login-email");shot("07-local-data-cleared")
    }
    @Test fun keyboardBackPasswordNotSavedAndFutureSectionsHonest() {
        tap("internal-nav-0");ui.onNodeWithText("Портфолио — следующий этап").assertExists();shot("08-portfolio-next-stage")
        tap("internal-nav-1");ui.onNodeWithText("AI-бриф — следующий этап").assertExists();shot("09-ai-next-stage")
        tap("internal-nav-2")
        ui.onNodeWithTag("login-email").performScrollTo().performTextReplacement(CLIENT_A)
        ui.onNodeWithTag("login-password").performScrollTo().performClick().performTextReplacement(TEST_PASSWORD)
        ui.waitUntil(10_000){ ViewCompat.getRootWindowInsets(ui.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime())==true }
        device.pressBack()
        ui.waitUntil(10_000){ ViewCompat.getRootWindowInsets(ui.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime())==false }
        ui.activityRule.scenario.recreate();waitFor("login-password")
        assertEquals("", ui.onNodeWithTag("login-password").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        shot("10-recreated-login-no-password")
    }
}
