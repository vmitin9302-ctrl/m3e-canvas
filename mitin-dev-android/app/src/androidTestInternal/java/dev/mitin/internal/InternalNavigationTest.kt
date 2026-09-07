package dev.mitin.internal

import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Runs against the shipped InternalApp, including its BackHandler and insets.
 * No server/account is required: the public CI build has no API origin. */
@RunWith(AndroidJUnit4::class)
class InternalNavigationTest {
    @get:Rule val ui = createAndroidComposeRule<InternalActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val device get() = UiDevice.getInstance(instrumentation)
    private val args get() = InstrumentationRegistry.getArguments()

    private fun assertNavigationFits() {
        val density = context.resources.displayMetrics.density
        var safeBottom = 0
        var safeLeft = 0
        var safeRight = 0
        instrumentation.runOnMainSync {
            val decor = ui.activity.window.decorView
            val insets = requireNotNull(ViewCompat.getRootWindowInsets(decor))
                .getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            safeBottom = decor.height - insets.bottom
            safeLeft = insets.left
            safeRight = decor.width - insets.right
            assertTrue("System navigation inset must be present", insets.bottom > 0)
        }
        val labels = listOf("Готовые проекты", "Обсудить с AI", "Мой кабинет")
        val shortLabels = listOf("Кейсы", "AI-бриф", "Кабинет")
        val itemBounds = labels.indices.map { index ->
            val item = ui.onNodeWithTag("internal-nav-$index").assertIsDisplayed().assertHasClickAction()
                .assertContentDescriptionEquals(labels[index])
            val label = ui.onNodeWithTag("internal-nav-label-$index", useUnmergedTree = true)
                .assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            label.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            val displayed = layout.layoutInput.text.text
            assertTrue("Unexpected section label", displayed == labels[index] || displayed == shortLabels[index])
            assertTrue("Navigation label requires more than two lines", layout.lineCount <= 2)
            for (line in 0 until layout.lineCount - 1) {
                val end = layout.getLineEnd(line, visibleEnd = true)
                val next = layout.getLineStart(line + 1)
                assertTrue("Word split across navigation lines: $displayed",
                    displayed.substring(end, next).any { it.isWhitespace() } ||
                        (end > 0 && displayed[end - 1] == '-'))
            }
            assertFalse("${labels[index]} overflows its text layout", layout.hasVisualOverflow)
            for (line in 0 until layout.lineCount) assertFalse(layout.isLineEllipsized(line))
            val textBounds = label.fetchSemanticsNode().boundsInWindow
            val bounds = item.fetchSemanticsNode().boundsInWindow
            assertTrue("Text is clipped horizontally", textBounds.width + 1 >= layout.size.width)
            assertTrue("Text is clipped vertically", textBounds.height + 1 >= layout.size.height)
            assertTrue("Label exceeds item", textBounds.left >= bounds.left - 1 && textBounds.right <= bounds.right + 1)
            assertTrue("Label exceeds item vertically", textBounds.top >= bounds.top - 1 && textBounds.bottom <= bounds.bottom + 1)
            assertTrue("Touch width below 48dp", bounds.width + 1 >= 48 * density)
            assertTrue("Touch height below 48dp", bounds.height + 1 >= 48 * density)
            assertTrue("Item overlaps system navigation", bounds.bottom <= safeBottom + 1)
            assertTrue("Item exceeds safe screen width", bounds.left >= safeLeft - 1 && bounds.right <= safeRight + 1)
            bounds
        }
        itemBounds.zipWithNext().forEach { (left, right) ->
            assertTrue("Navigation touch targets overlap", left.right <= right.left + 1)
        }
    }

    private fun shot(index: Int) {
        ui.waitForIdle()
        val drawn = CountDownLatch(1)
        instrumentation.runOnMainSync {
            val decor = ui.activity.window.decorView
            if (android.os.Build.VERSION.SDK_INT >= 29 && decor.isHardwareAccelerated) {
                decor.viewTreeObserver.registerFrameCommitCallback { drawn.countDown() }
                decor.invalidate()
            } else decor.postOnAnimation { drawn.countDown() }
        }
        assertTrue("Navigation frame was not rendered", drawn.await(10, TimeUnit.SECONDS))
        device.waitForIdle()
        val name = "${args.getString("navCase", "local")}-tab-$index.png"
        val file = File(context.getExternalFilesDir(null), name)
        assertTrue(device.takeScreenshot(file))
        device.executeShellCommand("mkdir -p /sdcard/Download/mitin-navigation")
        device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/mitin-navigation/$name")
        assertEquals(file.length(), device.executeShellCommand("stat -c %s /sdcard/Download/mitin-navigation/$name").trim().toLong())
    }

    @Test fun fullLabelsTouchTargetsInsetsAndSystemBack() {
        args.getString("expectedWidthDp")?.toInt()?.let { width ->
            assertEquals(width, context.resources.configuration.screenWidthDp)
        }
        args.getString("expectedFontScale")?.toFloat()?.let { scale ->
            assertEquals(scale, context.resources.configuration.fontScale, 0.01f)
        }
        ui.waitUntil(30_000) { ui.onAllNodesWithTag("internal-nav-2").fetchSemanticsNodes().isNotEmpty() }
        for (index in 0..2) {
            val bounds = ui.onNodeWithTag("internal-nav-$index").fetchSemanticsNode().boundsInWindow
            assertTrue(device.click(bounds.center.x.toInt(), bounds.center.y.toInt()))
            ui.waitForIdle()
            ui.onNodeWithTag("internal-nav-$index").assertIsSelected()
            when (index) {
                0 -> ui.onNodeWithText("Портфолио — следующий этап").assertExists()
                1 -> ui.onNodeWithText("AI-бриф — следующий этап").assertExists()
                2 -> ui.onNodeWithText("МОЙ КАБИНЕТ").assertExists()
            }
            shot(index)
            assertNavigationFits()
            if (index != 2) {
                device.pressBack(); ui.waitForIdle()
                ui.onNodeWithTag("internal-nav-2").assertIsSelected()
                assertNavigationFits()
            }
        }
        val previous = ui.activity
        device.pressBack()
        ui.waitUntil(30_000) {
            var stopped = false
            instrumentation.runOnMainSync {
                val stage = ActivityLifecycleMonitorRegistry.getInstance().getLifecycleStageOf(previous)
                stopped = stage == Stage.STOPPED || stage == Stage.DESTROYED
            }
            stopped
        }
        context.startActivity(Intent.makeMainActivity(android.content.ComponentName(context, InternalActivity::class.java))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
        ui.waitUntil(30_000) {
            var resumed = false
            instrumentation.runOnMainSync {
                resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .any { it is InternalActivity }
            }
            resumed
        }
        ui.waitForIdle()
        ui.onNodeWithTag("internal-nav-2").assertIsSelected()
    }
}
