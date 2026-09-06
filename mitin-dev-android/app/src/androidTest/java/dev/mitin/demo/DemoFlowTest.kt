package dev.mitin.demo

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoFlowTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val large get() = InstrumentationRegistry.getArguments().getString("largeFont") == "true"

    @Before fun reset() {
        // Each test starts from its own synthetic local fixture.
        context.getSharedPreferences("mitin_offline_demo_v1", Context.MODE_PRIVATE).edit().clear().commit()
        ui.activityRule.scenario.recreate()
        // Recreation retains ViewModel; reset through the real public demo UI as well.
        tap("demo-menu"); tap("choose-client"); tap("demo-login"); tap("open-home")
        tap("nav-c-profile"); tap("reset")
        ui.onNodeWithText("Сбросить", substring = false).performClick()
        screen("c-welcome")
    }
    private fun tap(tag: String) {
        val node = ui.onNodeWithTag(tag)
        if (ui.onAllNodes(hasTestTag(tag) and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty()) {
            node.performScrollTo()
        }
        node.performClick(); ui.waitForIdle()
    }
    private fun top(tag: String) { ui.onNodeWithTag(tag).performClick(); ui.waitForIdle() }
    private fun screen(name: String) { ui.waitUntil(10_000) { ui.onAllNodesWithTag("screen-$name").fetchSemanticsNodes().isNotEmpty() } }
    private fun shot(name: String) {
        ui.waitForIdle(); device.waitForIdle()
        val folder = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        assertTrue(device.takeScreenshot(File(folder, "${if (large) "large-" else ""}$name.png")))
    }
    private fun chooseOwner() { top("demo-menu"); tap("choose-owner") }
    private fun chooseClientHome() { top("demo-menu"); tap("choose-client"); tap("demo-login"); tap("open-home") }

    @Test fun clientAndOwnerShareCompleteOfflineJourney() {
        if (large) assertTrue(context.resources.configuration.fontScale >= 1.5f)
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission(Manifest.permission.INTERNET))
        shot("01-client-welcome")
        tap("create-request"); tap("type-0"); screen("c-brief"); shot("02-client-brief")
        ui.onNodeWithTag("task").performTextReplacement("Сайт для вымышленного автосервиса")
        tap("brief-next"); tap("budget"); top("budget-option-3")
        tap("review"); screen("c-review")
        ui.onNodeWithText("40–70 тыс.").assertExists()
        shot("03-client-review")
        tap("submit"); screen("c-sent"); ui.onNodeWithText("Заявка №1046\nсохранена").assertExists()
        shot("04-client-confirmation")
        tap("sent-home"); screen("c-home"); shot("05-client-home")
        tap("client-project-1"); screen("c-project"); shot("06-client-project")
        tap("stage-detail"); screen("c-stage"); shot("07-client-stage")
        tap("stage-demo"); screen("c-demo"); shot("08-client-demo")
        tap("open-demo"); screen("c-demo-site"); shot("09-client-demo-site")
        tap("demo-return"); screen("c-demo")
        device.pressBack(); screen("c-stage")
        tap("stage-demo"); tap("open-demo")
        device.pressBack(); screen("c-demo")
        chooseOwner(); tap("owner-leads"); screen("o-leads"); shot("10-owner-leads")
        tap("lead-1046"); screen("o-lead")
        ui.onNodeWithText("Сайт для вымышленного автосервиса").assertExists()
        ui.onNodeWithText("40–70 тыс.").assertExists()
        shot("11-owner-lead")
        tap("convert-lead"); screen("o-project")
        ui.onNodeWithText("Стоимость не согласована").assertExists()
        shot("12-owner-project")
        tap("edit-stages"); tap("stage-picker"); top("stage-picker-option-2")
        tap("status-picker"); top("status-picker-option-2"); shot("13-owner-stage-edit")
        tap("save-stage"); screen("o-project")
        chooseClientHome(); tap("client-project-2"); screen("c-project")
        ui.onNodeWithText("Разработка · Ожидаем клиента").assertExists()
        shot("14-client-shared-project")
        tap("view-demo"); ui.onNodeWithText("Демо ещё нет").assertExists()
        tap("sample-demo"); screen("c-demo-site")
        device.pressBack(); screen("c-demo")
        ui.onNodeWithText("Демо ещё нет").assertExists()
        device.pressBack(); screen("c-project")
        ui.onNodeWithText("Сайт · заявка №1046").assertExists()
        // Read a fresh repository from disk; state is not merely retained in Compose memory.
        val fresh = DemoViewModel(context.applicationContext as Application).repository.state.value
        val created = fresh.projects.single { it.leadId == 1046 }
        assertEquals(StageStatus.WAITING_CLIENT, created.stages[2])
        assertNull(created.agreedPrice)
        assertEquals(2, fresh.projects.size)
    }

    @Test fun keyboardSystemBackRotationAndLongContent() {
        tap("create-request"); tap("type-5")
        val text = "Вымышленная идея: " + "подробное описание проекта с пробелами. ".repeat(12)
        ui.onNodeWithTag("task").performScrollTo().performClick().performTextReplacement(text)
        ui.waitUntil(10_000) {
            ViewCompat.getRootWindowInsets(ui.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        shot("15-keyboard-long-brief")
        device.pressBack()
        ui.waitUntil(10_000) {
            ViewCompat.getRootWindowInsets(ui.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == false
        }
        screen("c-brief")
        device.pressBack(); screen("c-type")
        tap("type-5"); ui.onNodeWithTag("task").assertTextContains(text)
        ui.activityRule.scenario.recreate()
        screen("c-brief"); ui.onNodeWithTag("task").assertTextContains(text)
        tap("brief-next"); tap("review"); tap("submit"); screen("c-sent")
        shot("16-long-brief-completed")
    }

    @Test fun demoDecisionAndMessagesPersistAcrossRoles() {
        tap("demo-login"); tap("open-home"); tap("client-project-1"); tap("view-demo")
        tap("demo-feedback"); ui.onNodeWithTag("feedback").performTextInput("Увеличить заголовок · демо")
        tap("save-feedback"); screen("c-demo-result")
        chooseOwner(); tap("owner-projects"); tap("owner-project-1"); tap("owner-demo")
        ui.onNodeWithText("Увеличить заголовок · демо").assertExists()
        top("nav-o-more"); tap("owner-chat")
        ui.onNodeWithTag("message").performScrollTo().performTextInput("Учтём в локальном макете")
        tap("save-message")
        chooseClientHome(); top("nav-c-chat")
        ui.onNodeWithText("Учтём в локальном макете").assertExists()
        shot("17-shared-chat")
    }
}
