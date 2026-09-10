package dev.mitin.internal

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import dev.mitin.testing.replaceWhenReady
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Signed-out installs start on the native cabinet login; the four tabs appear only after a real server login. */
@RunWith(AndroidJUnit4::class)
class InternalUiTest {
    @get:Rule val ui=createAndroidComposeRule<InternalActivity>()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val device get()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val large get()=InstrumentationRegistry.getArguments().getString("largeFont")=="true"
    private val manager get()=(context.applicationContext as InternalApplication).manager!!
    private val sessionsVm get()=ViewModelProvider(ui.activity)[InternalViewModel::class.java]
    @Before fun reset() {
        runBlocking { withTimeout(30_000) {while(manager.state.value.restoring) delay(50)}; manager.logout(); manager.restore() }
        ui.waitForIdle();waitFor("cabinet-email")
        ui.onNodeWithTag("internal-navigation-bar").assertDoesNotExist()
    }
    private fun tap(tag:String) {
        waitFor(tag)
        val node=ui.onNodeWithTag(tag)
        if(ui.onAllNodes(hasTestTag(tag) and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty()) node.performScrollTo()
        node.performClick();ui.waitForIdle()
    }
    private fun waitFor(tag:String) {ui.waitUntil(30_000){ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()}}
    private fun fill(tag:String,value:String) { waitFor(tag);ui.replaceWhenReady(tag,value){ui.activity} }
    private fun shot(name:String) {
        ui.waitForIdle()
        // Semantics can be ready one rendered frame before SurfaceFlinger. Wait
        // for the actual app frame so screenshots show the asserted server state.
        val drawn=CountDownLatch(1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<InternalActivity>().single()
            val decor=activity.window.decorView
            if(android.os.Build.VERSION.SDK_INT>=29 && decor.isHardwareAccelerated) {
                decor.viewTreeObserver.registerFrameCommitCallback { drawn.countDown() }
                decor.invalidate()
            } else decor.postOnAnimation { drawn.countDown() }
        }
        assertTrue("App frame was not rendered",drawn.await(10,TimeUnit.SECONDS))
        device.waitForIdle()
        val dir=File(context.getExternalFilesDir(null),"network-shots").apply{mkdirs()}
        val file=File(dir,"${if(large) "large-" else ""}$name.png")
        assertTrue(device.takeScreenshot(file))
        device.executeShellCommand("mkdir -p /sdcard/Download/mitin-network")
        device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/mitin-network/${file.name}")
        assertEquals(file.length(),device.executeShellCommand("stat -c %s /sdcard/Download/mitin-network/${file.name}").trim().toLong())
    }
    private fun imeVisible():Boolean {
        var visible=false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<InternalActivity>().singleOrNull()
            visible=activity?.let { ViewCompat.getRootWindowInsets(it.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) }==true
        }
        return visible
    }
    private fun hideIme() { if(imeVisible()) { device.pressBack(); ui.waitUntil(10_000) { !imeVisible() } }; ui.waitForIdle() }
    /** Real cabinet login through the shipped form, then the cabinet tab of the signed-in shell. */
    private fun login(email:String) {
        waitFor("cabinet-email")
        // A fresh install defaults to registration mode; switch explicitly before a login flow.
        if(ui.onAllNodesWithTag("entry-login").fetchSemanticsNodes().isNotEmpty()) tap("entry-login")
        waitFor("cabinet-password")
        fill("cabinet-email",email);fill("cabinet-password",TEST_PASSWORD)
        ui.waitUntil(10_000) { imeVisible() }
        hideIme()
        ui.onNodeWithTag("cabinet-auth-submit").performScrollTo().assertIsDisplayed().assertIsEnabled()
        tap("cabinet-auth-submit")
        try {
            // onClick clears the password before the request completes. Never dump the field value.
            ui.waitUntil(5_000) {
                val fields=ui.onAllNodesWithTag("cabinet-password").fetchSemanticsNodes()
                fields.isEmpty() || fields.single().config[SemanticsProperties.EditableText].text.isEmpty()
            }
        } catch (failure: Throwable) { shot("11-login-action-not-invoked"); throw failure }
        try {
            ui.waitUntil(60_000) { manager.state.value.profile != null }
            waitFor("internal-nav-3")
        } catch (failure: Throwable) { shot("12-login-result-timeout"); throw failure }
        ui.onNodeWithTag("internal-nav-0").assertIsSelected()
        tap("internal-nav-3");waitFor("cabinet-projects")
    }
    @Test fun realLoginProfileSessionsRevocationLogoutAllAndNewAccount() {
        if(large) assertTrue(context.resources.configuration.fontScale>=1.5f)
        shot("01-network-login")
        login(CLIENT_A);shot("02-server-profile-a")
        assertEquals("Тестовый клиент А", manager.state.value.profile!!.displayName)
        // A second independent real server session is created outside this app manager.
        runBlocking { HttpAuthApi("https://localhost:8443/").login(CLIENT_A,Secret(TEST_PASSWORD),701) }
        tap("cabinet-sessions")
        ui.waitUntil(30_000) { sessionsVm.page.items.size >= 2 }
        val other=sessionsVm.page.items.first { !it.current && it.revokedAt == null }
        waitFor("revoke-${other.id}");shot("03-real-sessions")
        tap("revoke-${other.id}")
        ui.waitUntil(30_000) { !sessionsVm.busy && sessionsVm.page.items.any { it.id == other.id && it.revokedAt != null } }
        // The other session is revoked on the server; the current app stays signed in.
        assertNotNull(manager.state.value.profile)
        shot("04-session-revoked")
        tap("cabinet-logout-all");waitFor("cabinet-email")
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
        waitFor("cabinet-email")
        ui.onNodeWithTag("internal-navigation-bar").assertDoesNotExist()
        login(CLIENT_B);shot("06-server-profile-b")
        assertEquals("Тестовый клиент Б", manager.state.value.profile!!.displayName)
        ui.onNodeWithText("Тестовый клиент А").assertDoesNotExist()
        tap("cabinet-logout");waitFor("cabinet-email");shot("07-local-data-cleared")
        assertNull(manager.state.value.profile)
    }
    @Test fun keyboardBackPasswordNotSavedAndTabsOnlyAfterLogin() {
        fill("cabinet-email",CLIENT_A);fill("cabinet-password",TEST_PASSWORD)
        ui.waitUntil(10_000){ imeVisible() }
        device.pressBack()
        ui.waitUntil(10_000){ !imeVisible() }
        ui.activityRule.scenario.recreate();waitFor("cabinet-password")
        assertEquals("", ui.onNodeWithTag("cabinet-password").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        ui.onNodeWithTag("cabinet-email").assertTextContains(CLIENT_A)
        shot("10-recreated-login-no-password")
        login(CLIENT_A)
        tap("internal-nav-2");waitFor("portfolio-ritmassage");ui.onNodeWithText("РиТМассаж").assertExists();shot("08-public-portfolio")
        tap("internal-nav-0");ui.onNodeWithText("Что хотите создать?").assertExists();shot("09-ai-brief-setup")
        tap("internal-nav-1");waitFor("business-audit");shot("09-business-audit")
        device.pressBack();ui.waitForIdle()
        ui.onNodeWithTag("internal-nav-0").assertIsSelected()
        tap("internal-nav-3");tap("cabinet-logout");waitFor("cabinet-email")
        ui.onNodeWithTag("internal-navigation-bar").assertDoesNotExist()
    }
}
