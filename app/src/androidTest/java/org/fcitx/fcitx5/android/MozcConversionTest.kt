package org.fcitx.fcitx5.android

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.fcitx.fcitx5.android.core.Fcitx
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.core.FcitxKeyMapping
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

/** Requires the matching Mozc plugin APK. Uses the real engine and packaged dictionary. */
class MozcConversionTest {
    companion object {
        private lateinit var fcitx: Fcitx
        private var previousEnabled: Array<String>? = null
        private var previousIme: String? = null

        @BeforeClass
        @JvmStatic
        fun start() = runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            fcitx = Fcitx(context)
            fcitx.start()
            withTimeout(60_000) { while (!fcitx.isReady) delay(50) }
            assertTrue("Install the matching Mozc plugin", fcitx.availableIme().any { it.uniqueName == "mozc" })
            previousEnabled = fcitx.enabledIme().map { it.uniqueName }.toTypedArray()
            fcitx.activate(android.os.Process.myUid(), context.packageName)
            fcitx.focus(true)
            previousIme = fcitx.currentIme().uniqueName
            fcitx.setEnabledIme(arrayOf("mozc"))
            fcitx.activateIme("mozc")
            assertEquals("mozc", fcitx.currentIme().uniqueName)
        }

        @AfterClass
        @JvmStatic
        fun stop() = runBlocking {
            try {
                if (fcitx.isReady) {
                    fcitx.reset()
                    previousEnabled?.let { fcitx.setEnabledIme(it) }
                    previousIme?.let { fcitx.activateIme(it) }
                    fcitx.focus(false)
                    fcitx.deactivate(android.os.Process.myUid())
                }
            } finally {
                fcitx.stop()
            }
        }
    }

    @Before
    fun reset() = runBlocking { fcitx.reset() }

    private suspend fun type(text: String) {
        text.forEach { fcitx.sendKey(it, KeyStates.Virtual.states) }
    }

    private suspend fun key(sym: Int, states: KeyStates = KeyStates.Virtual) {
        fcitx.sendKey(KeySym(sym), states)
    }

    private suspend fun conversion() {
        type("kyouhaiitenkidesu")
        key(FcitxKeyMapping.FcitxKey_space)
        assertFalse(fcitx.isEmpty())
    }

    private suspend fun committed(action: suspend () -> Unit): String = coroutineScope {
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(20_000) {
                fcitx.eventFlow.filterIsInstance<FcitxEvent.CommitStringEvent>().first().data.text
            }
        }
        action()
        result.await().also {
            assertTrue(it.isNotEmpty())
            assertFalse("Commit must not insert a newline", it.contains('\n'))
        }
    }

    @Test
    fun predictionTapCommitsWithoutEnter() = runBlocking {
        type("nihongo")
        val candidates = fcitx.getCandidates(0, 1)
        assertTrue(candidates.isNotEmpty())
        val text = committed { assertTrue(fcitx.select(0)) }
        assertEquals(candidates[0].text, text)
        assertTrue(fcitx.isEmpty())
    }

    @Test
    fun conversionTapCommitsSegmentAndKeepsRemainder() = runBlocking {
        conversion()
        // Mozc opens the conversion candidate list on the next Space.
        key(FcitxKeyMapping.FcitxKey_space)
        val candidates = fcitx.getCandidates(0, 1)
        assertTrue("Conversion candidates must be visible", candidates.isNotEmpty())
        assertEquals(candidates[0].text, committed { assertTrue(fcitx.select(0)) })
        assertFalse("The following segments must remain editable", fcitx.isEmpty())
        committed { key(FcitxKeyMapping.FcitxKey_Return, KeyStates.Empty) }
        assertTrue(fcitx.isEmpty())
    }

    @Test
    fun screenEnterCommitsOneSegment() = runBlocking {
        conversion()
        committed { key(FcitxKeyMapping.FcitxKey_Return) }
        assertFalse(fcitx.isEmpty())
        committed { key(FcitxKeyMapping.FcitxKey_Return, KeyStates.Empty) }
        assertTrue(fcitx.isEmpty())
    }

    @Test
    fun physicalEnterCommitsAllSegments() = runBlocking {
        conversion()
        committed { key(FcitxKeyMapping.FcitxKey_Return, KeyStates.Empty) }
        assertTrue(fcitx.isEmpty())
    }

    @Test
    fun screenEnterCommitsUnconvertedReading() = runBlocking {
        type("nihongo")
        assertEquals("にほんご", committed { key(FcitxKeyMapping.FcitxKey_Return) })
        assertTrue(fcitx.isEmpty())
    }

    @Test
    fun japanesePunctuationUsesToutenAndKuten() = runBlocking {
        type("nihongo,desu.")
        val text = committed { key(FcitxKeyMapping.FcitxKey_Return) }
        assertTrue(text.contains('、'))
        assertTrue(text.endsWith('。'))
        assertFalse(text.contains(','))
        assertFalse(text.contains('.'))
    }

    @Test
    fun alphanumericPunctuationKeepsCommaAndPeriod() = runBlocking {
        key(FcitxKeyMapping.FcitxKey_Eisu_toggle)
        try {
            type("abc,def.")
            assertEquals("abc,def.", committed { key(FcitxKeyMapping.FcitxKey_Return) })
        } finally {
            fcitx.reset()
            key(FcitxKeyMapping.FcitxKey_Hiragana_Katakana)
        }
    }

    @Test
    fun screenRightExpandsSegment() = runBlocking {
        conversion()
        val original = committed { key(FcitxKeyMapping.FcitxKey_Return) }
        fcitx.reset()
        conversion()
        key(FcitxKeyMapping.FcitxKey_Right)
        val expanded = committed { key(FcitxKeyMapping.FcitxKey_Return) }
        assertNotEquals("Screen right must expand the segment", original, expanded)
        assertFalse(fcitx.isEmpty())
    }

    @Test
    fun screenLeftShrinksSegmentWhilePhysicalLeftKeepsItsWidth() = runBlocking {
        conversion()
        val original = committed { key(FcitxKeyMapping.FcitxKey_Return) }
        fcitx.reset()
        conversion()
        key(FcitxKeyMapping.FcitxKey_Left, KeyStates.Empty)
        val physical = committed { key(FcitxKeyMapping.FcitxKey_Return) }
        assertEquals(original, physical)
        fcitx.reset()
        conversion()
        key(FcitxKeyMapping.FcitxKey_Left)
        val shortened = committed { key(FcitxKeyMapping.FcitxKey_Return) }
        assertNotEquals("Screen left must resize, not just move focus", original, shortened)
        assertFalse(fcitx.isEmpty())
    }
}
