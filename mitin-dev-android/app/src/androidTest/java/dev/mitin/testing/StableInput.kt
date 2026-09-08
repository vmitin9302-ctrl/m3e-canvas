package dev.mitin.testing

import android.app.Activity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue

/** Synchronize with the real window/IME, which Compose idle alone does not cover. */
fun ComposeTestRule.settleWindow(activity: () -> Activity) {
    waitForIdle()
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    UiDevice.getInstance(instrumentation).waitForIdle()
    val committed = CountDownLatch(1)
    instrumentation.runOnMainSync {
        val view = activity().window.decorView
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            view.viewTreeObserver.registerFrameCommitCallback { committed.countDown() }
            view.invalidate()
        } else view.postOnAnimation { committed.countDown() }
    }
    assertTrue("Window did not commit an input-ready frame", committed.await(10, TimeUnit.SECONDS))
    waitForIdle()
}

fun ComposeTestRule.replaceWhenReady(tag: String, value: String, activity: () -> Activity) {
    val node = onNodeWithTag(tag)
    if (onAllNodes(hasTestTag(tag) and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty()) {
        node.performScrollTo()
    }
    node.performClick()
    waitUntil(10_000) { onAllNodes(hasTestTag(tag) and isFocused()).fetchSemanticsNodes().isNotEmpty() }
    waitUntil(10_000) {
        var visible = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            visible = ViewCompat.getRootWindowInsets(activity().window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        visible
    }
    settleWindow(activity)
    node.performTextReplacement(value)
    settleWindow(activity)
    // EditableText is the visually transformed value (bullets for passwords).
    // InputText is the actual input in Compose 1.9+. Compare in memory only;
    // never let an assertion dump the code/password or retry a dropped edit.
    assertTrue("Input value was not retained after IME initialization (field: $tag)",
        node.fetchSemanticsNode().config[SemanticsProperties.InputText].text == value)
}
