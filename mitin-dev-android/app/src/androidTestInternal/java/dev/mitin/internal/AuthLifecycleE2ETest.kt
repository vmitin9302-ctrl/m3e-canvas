package dev.mitin.internal

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.mitin.testing.replaceWhenReady
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/** Run each phase in a fresh instrumentation process against the disposable TLS harness. */
class AuthLifecycleE2ETest {
    @get:Rule val ui=createAndroidComposeRule<InternalActivity>()
    private val manager get()=(ui.activity.application as InternalApplication).manager!!
    private val origin="https://localhost:8443/"
    private fun waitFor(tag:String) { ui.waitUntil(60_000){ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()} }
    private fun tap(tag:String) {
        waitFor(tag)
        val node=ui.onNodeWithTag(tag)
        if(ui.onAllNodes(hasTestTag(tag) and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty())node.performScrollTo()
        node.performClick();ui.waitForIdle()
    }
    private fun fill(tag:String,value:String) { waitFor(tag);ui.replaceWhenReady(tag,value){ui.activity} }
    private fun hideIme() {
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        var visible=false
        ui.runOnUiThread { visible=androidx.core.view.ViewCompat.getRootWindowInsets(ui.activity.window.decorView)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())==true }
        if(visible)device.pressBack()
        ui.waitForIdle()
    }
    @Test fun registerVerifyLoginAndLeaveSecureSession() {
        waitFor("cabinet-name") // Clean install must start on registration, not Cases.
        val email="cabinet-entry-${UUID.randomUUID()}@example.test"
        fill("cabinet-name","Синтетический клиент");fill("cabinet-email",email)
        fill("cabinet-password","Six123");fill("cabinet-password-repeat","Six123");hideIme()
        tap("cabinet-consent");tap("cabinet-terms")
        ui.onNodeWithTag("cabinet-auth-submit").assertIsEnabled()
        tap("cabinet-auth-submit")
        waitFor("verification-instructions")
        ui.onNodeWithTag("cabinet-token").assertDoesNotExist()
        ui.activityRule.scenario.recreate();waitFor("verification-instructions")
        runBlocking(Dispatchers.IO) {
            val body=buildJsonObject {put("email",email);put("purpose","verify_email")}
            val token=HttpAuthApi.secureClient().newCall(Request.Builder().url(origin+"test/cabinet-mail")
                .post(body.toString().toRequestBody("application/json".toMediaType())).build()).execute().use {
                    check(it.isSuccessful);Json.parseToJsonElement(it.body.string()).jsonObject.text("token")
                }
            // Same action as the HTTPS confirmation page, using only captured synthetic mail.
            CabinetApi(origin).call("auth/verify-email","POST",body=buildJsonObject{put("token",token)})
        }
        tap("auth-login");fill("cabinet-email",email);fill("cabinet-password","Six123");hideIme();tap("cabinet-auth-submit")
        ui.waitUntil(60_000){manager.state.value.profile != null}
        waitFor("internal-nav-0")
        assertNotNull(manager.state.value.profile)
        ui.onNodeWithTag("internal-nav-0").assertIsSelected()
        // Native business audit is a signed-in tab: answers survive recreation, Back steps out to the brief tab.
        tap("internal-nav-1");waitFor("business-audit")
        fill("audit-business","Синтетическая студия");hideIme()
        tap("audit-option-100");tap("audit-next")
        ui.activityRule.scenario.recreate();waitFor("business-audit")
        ui.onNodeWithTag("internal-nav-1").assertIsSelected()
        tap("audit-back");ui.onNodeWithTag("audit-option-100").assertIsSelected()
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack();ui.waitForIdle()
        ui.onNodeWithTag("internal-nav-0").assertIsSelected()
        tap("internal-nav-3");waitFor("cabinet-profile")
        ui.activityRule.scenario.recreate();waitFor("cabinet-profile")
        ui.onNodeWithTag("company-contacts").performScrollTo().assertExists()
        tap("internal-nav-0")
        assertNotNull(runBlocking{KeystoreRefreshStore(ui.activity).read()?.refresh})
    }
    @Test fun restoredSessionOpensMainAndLogoutReturnsToLogin() {
        ui.waitUntil(60_000){!manager.state.value.restoring && manager.state.value.profile != null}
        waitFor("internal-nav-0")
        assertNotNull(manager.state.value.profile)
        ui.onNodeWithTag("internal-nav-0").assertIsSelected()
        tap("internal-nav-3");waitFor("cabinet-logout");tap("cabinet-logout")
        waitFor("cabinet-email")
        ui.onNodeWithTag("cabinet-name").assertDoesNotExist()
        assertNull(manager.state.value.profile)
        assertNull(runBlocking{KeystoreRefreshStore(ui.activity).read()})
    }
    @Test fun loggedOutOrRevokedRestartOpensLoginNotOnboarding() {
        waitFor("cabinet-email")
        ui.onNodeWithTag("cabinet-name").assertDoesNotExist()
        assertNull(manager.state.value.profile)
        assertTrue((ui.activity.application as InternalApplication).authEntryHistory.returning)
        ui.onNodeWithTag("auth-register").assertExists()
    }
    @Test fun revokeServerSessionWithoutClearingLocalCredentials() {
        ui.waitUntil(60_000){!manager.state.value.restoring && manager.state.value.profile != null}
        waitFor("internal-nav-0")
        runBlocking {
            manager.authorizedMutation { token -> HttpAuthApi(origin).logout(token,0,false) }
            assertNotNull(KeystoreRefreshStore(ui.activity).read()?.refresh)
        }
    }
}
