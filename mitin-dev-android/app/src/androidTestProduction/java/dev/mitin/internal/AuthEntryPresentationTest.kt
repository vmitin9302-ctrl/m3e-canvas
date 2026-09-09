package dev.mitin.internal

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.By
import dev.mitin.testing.replaceWhenReady
import org.junit.Rule
import org.junit.Test

/** Read-only production smoke: never submits registration, login, email or AI requests. */
class AuthEntryPresentationTest {
    @get:Rule val ui=createAndroidComposeRule<InternalActivity>()
    private val device get()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun waitFor(tag:String) { ui.waitUntil(60_000) { ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() } }
    private fun tap(tag:String) { waitFor(tag);ui.onNodeWithTag(tag).performScrollTo().performClick();ui.waitForIdle() }
    private fun fill(tag:String,value:String) {
        waitFor(tag)
        ui.replaceWhenReady(tag,value){ui.activity}
    }
    private fun hideIme() {
        var visible=false
        ui.runOnUiThread { visible=androidx.core.view.ViewCompat.getRootWindowInsets(ui.activity.window.decorView)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())==true }
        if(visible)device.pressBack()
        ui.waitForIdle()
    }
    private fun shot(stage:String) {
        ui.waitForIdle()
        val inst=InstrumentationRegistry.getInstrumentation()
        val label=InstrumentationRegistry.getArguments().getString("authCase") ?: "manual"
        val folder=java.io.File(inst.targetContext.getExternalFilesDir(null),"auth-entry").apply{mkdirs()}
        org.junit.Assert.assertTrue(device.takeScreenshot(java.io.File(folder,"$label-$stage.png")))
        org.junit.Assert.assertFalse("Android ANR dialog obscures the tested app",device.hasObject(By.textContains("isn't responding")))
    }
    private fun registration() {
        ui.waitUntil(60_000) { ui.onAllNodesWithTag("cabinet-email").fetchSemanticsNodes().isNotEmpty() || ui.onAllNodesWithTag("startup-retry").fetchSemanticsNodes().isNotEmpty() }
        if(ui.onAllNodesWithTag("startup-retry").fetchSemanticsNodes().isNotEmpty()) {
            println("Startup showed a network error; exercising its explicit Retry action")
            ui.onNodeWithTag("startup-retry").performClick()
        }
        waitFor("cabinet-email")
        // Each matrix run starts with cleared app data. Later methods may use login mode.
        if(ui.onAllNodesWithTag("auth-register").fetchSemanticsNodes().isNotEmpty())tap("auth-register")
        waitFor("cabinet-name")
    }
    @Test fun validSixCharacterFormErrorsKeyboardBackAndRecreation() {
        registration()
        ui.onNodeWithTag("internal-nav-2").assertIsSelected()
        fill("cabinet-name","Синтетический клиент")
        fill("cabinet-email","synthetic@example.test")
        fill("cabinet-password","five5")
        ui.onNodeWithTag("cabinet-password-error",useUnmergedTree=true).assertExists()
        ui.waitUntil(10_000) {
            var visible=false
            ui.runOnUiThread { visible=androidx.core.view.ViewCompat.getRootWindowInsets(ui.activity.window.decorView)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())==true }
            visible
        }
        shot("password-error-keyboard")
        fill("cabinet-password","Six123")
        fill("cabinet-password-repeat","Six123")
        hideIme()
        ui.onNodeWithTag("cabinet-auth-submit").assertIsNotEnabled()
        tap("cabinet-consent")
        ui.onNodeWithTag("cabinet-auth-submit").assertIsNotEnabled()
        tap("cabinet-terms")
        ui.onNodeWithTag("cabinet-auth-submit").assertIsEnabled()
        fill("cabinet-phone","invalid")
        ui.onNodeWithTag("cabinet-phone-error",useUnmergedTree=true).assertExists()
        ui.onNodeWithTag("cabinet-auth-submit").assertIsNotEnabled()
        fill("cabinet-phone","")
        ui.onNodeWithTag("cabinet-auth-submit").assertIsEnabled()
        fill("cabinet-password-repeat","different")
        ui.onNodeWithTag("cabinet-password-repeat-error",useUnmergedTree=true).assertExists()
        ui.onNodeWithTag("cabinet-auth-submit").assertIsNotEnabled()
        fill("cabinet-password-repeat","Six123")
        hideIme()
        ui.activityRule.scenario.recreate()
        waitFor("cabinet-name")
        ui.onNodeWithTag("cabinet-name").assertTextContains("Синтетический клиент")
        org.junit.Assert.assertEquals("",ui.onNodeWithTag("cabinet-password").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text)
        ui.onNodeWithTag("cabinet-auth-submit").assertIsNotEnabled()
        fill("cabinet-password","TenChars12")
        fill("cabinet-password-repeat","TenChars12")
        hideIme()
        ui.onNodeWithTag("cabinet-auth-submit").assertIsEnabled()
        // Deliberately do not click: no real accounts or delivery in production tests.
        ui.onNodeWithTag("cabinet-auth-submit").performScrollTo()
        shot("registration-valid")
    }
    @Test fun emailLinkInstructionsNeverAskForAnEmailedPassword() {
        registration()
        ui.runOnUiThread { ViewModelProvider(ui.activity)[CabinetViewModel::class.java].authMode="verify-email" }
        waitFor("verification-instructions")
        ui.onNodeWithTag("cabinet-token").assertDoesNotExist()
        ui.onNodeWithTag("cabinet-password").assertDoesNotExist()
        ui.onNodeWithTag("cabinet-auth-submit").assertDoesNotExist()
        ui.activityRule.scenario.recreate()
        waitFor("verification-instructions")
        shot("email-link")
        tap("auth-login")
        waitFor("cabinet-password")
        ui.onNodeWithTag("cabinet-token").assertDoesNotExist()
        ui.onNodeWithTag("company-contacts").performScrollTo().assertExists()
        ui.onNodeWithTag("company-contact-email").assertExists()
    }
    @Test fun nativeAuditKeepsAnswersAndComputesWebsiteScoreWithoutNetwork() {
        registration()
        tap("open-business-audit")
        fill("audit-business","Синтетическая студия")
        hideIme()
        repeat(7) { step ->
            if(step==3) { ui.activityRule.scenario.recreate();waitFor("business-audit") }
            tap("audit-option-100");tap("audit-next")
        }
        ui.onNodeWithTag("audit-score").assertTextEquals("Digital Score: 100/100")
        shot("audit-result")
        ui.activityRule.scenario.recreate();waitFor("audit-score")
        ui.onNodeWithTag("audit-score").assertTextEquals("Digital Score: 100/100")
        tap("audit-back")
        ui.onNodeWithTag("audit-option-100").assertIsSelected()
        tap("audit-next")
        tap("audit-restart")
        ui.onNodeWithTag("audit-next").assertIsNotEnabled()
    }
}
