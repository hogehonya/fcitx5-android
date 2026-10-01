package org.fcitx.fcitx5.android

import android.os.SystemClock
import android.view.MotionEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.keyboard.BaseKeyboard
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyView
import org.fcitx.fcitx5.android.input.keyboard.ReturnKey
import org.fcitx.fcitx5.android.input.popup.PopupAction
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MozcReturnGestureTest {
    private class Owner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)
    }

    @Test
    fun committedLongPressDoesNotAlsoSendEnterOnRelease() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val commits = AtomicInteger()
        val presses = AtomicInteger()
        val menus = AtomicInteger()
        val longPressed = CountDownLatch(1)
        lateinit var owner: Owner
        lateinit var key: KeyView
        val down = SystemClock.uptimeMillis()
        instrumentation.runOnMainSync {
            owner = Owner().apply { lifecycle.currentState = Lifecycle.State.RESUMED }
            val keyboard = object : BaseKeyboard(
                instrumentation.targetContext, ThemeManager.activeTheme, listOf(listOf(ReturnKey()))
            ) {
                override fun onAction(action: KeyAction, source: KeyActionListener.Source) {
                    presses.incrementAndGet()
                }
                override fun onPopupAction(action: PopupAction) {
                    if (action is PopupAction.ShowMenuAction) menus.incrementAndGet()
                }
            }
            keyboard.setViewTreeLifecycleOwner(owner)
            keyboard.returnLongPressListener = {
                commits.incrementAndGet()
                longPressed.countDown()
                true
            }
            key = keyboard.findViewById(R.id.button_return)
            key.layout(0, 0, 200, 200)
            MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, 50f, 50f, 0).also {
                key.onTouchEvent(it)
                it.recycle()
            }
        }
        try {
            assertTrue("Long press should fire", longPressed.await(10, TimeUnit.SECONDS))
            instrumentation.runOnMainSync {
                MotionEvent.obtain(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 50f, 50f, 0).also {
                    key.onTouchEvent(it)
                    it.recycle()
                }
            }
            assertEquals(1, commits.get())
            assertEquals(0, presses.get())
            assertEquals(0, menus.get())
        } finally {
            instrumentation.runOnMainSync { owner.lifecycle.currentState = Lifecycle.State.DESTROYED }
        }
    }

    @Test
    fun unhandledLongPressKeepsTheExistingPopup() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            var menus = 0
            val keyboard = object : BaseKeyboard(
                instrumentation.targetContext, ThemeManager.activeTheme, listOf(listOf(ReturnKey()))
            ) {
                override fun onPopupAction(action: PopupAction) {
                    if (action is PopupAction.ShowMenuAction) menus++
                }
            }
            keyboard.returnLongPressListener = { false }
            assertFalse(keyboard.findViewById<KeyView>(R.id.button_return).performLongClick())
            assertEquals(1, menus)
            keyboard.returnLongPressListener = null
            assertFalse(keyboard.findViewById<KeyView>(R.id.button_return).performLongClick())
            assertEquals(2, menus)
        }
    }
}
