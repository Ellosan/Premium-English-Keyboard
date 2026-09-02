package com.premiumenglish.keyboard

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KeyboardPanelTest {

    /** Records what the keyboard asked the input method to do. */
    private class Recorder : KeyboardPanel.Listener {
        val chars = StringBuilder()
        val texts = ArrayList<String>()
        var backspaces = 0
        var enters = 0
        var tierCycles = 0
        var reverts = 0

        override fun onChar(c: Char) { chars.append(c) }
        override fun onText(text: String) { texts.add(text) }
        override fun onBackspace() { backspaces++ }
        override fun onEnter() { enters++ }
        override fun onCycleTier() { tierCycles++ }
        override fun onToggleTranslation() = Unit
        override fun onOpenSettings() = Unit
        override fun onSwitchKeyboard() = Unit
        override fun onRevert() { reverts++ }
    }

    private lateinit var recorder: Recorder
    private lateinit var panel: KeyboardPanel

    @Before
    fun setUp() {
        recorder = Recorder()
        panel = KeyboardPanel(RuntimeEnvironment.getApplication(), recorder)
        layOut()
    }

    private fun layOut() {
        val width = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
        val height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        panel.measure(width, height)
        panel.layout(0, 0, 1080, panel.measuredHeight)
    }

    // ------------------------------------------------------------------ helpers

    private fun findKey(label: String, root: View = panel): TextView? {
        if (root is TextView && root.text?.toString() == label) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findKey(label, root.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun tap(view: View, holdMs: Long = 0) {
        val now = SystemClock.uptimeMillis()
        view.dispatchTouchEvent(
            MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 4f, 4f, 0)
        )
        if (holdMs > 0) {
            // Let the long-press runnable fire.
            org.robolectric.shadows.ShadowLooper.idleMainLooper(
                holdMs, java.util.concurrent.TimeUnit.MILLISECONDS
            )
        }
        view.dispatchTouchEvent(
            MotionEvent.obtain(now, now + holdMs, MotionEvent.ACTION_UP, 4f, 4f, 0)
        )
    }

    // ------------------------------------------------------------------ tests

    @Test
    fun `the letter keys are all present`() {
        for (c in "qwertyuiopasdfghjklzxcvbnm") {
            assertNotNull("missing key: $c", findKey(c.toString()))
        }
        assertNotNull(findKey("space"))
        assertNotNull(findKey("⌫"))
        assertNotNull(findKey("⇧"))
    }

    @Test
    fun `tapping a letter types it`() {
        tap(findKey("q")!!)
        tap(findKey("a")!!)
        assertEquals("qa", recorder.chars.toString())
    }

    @Test
    fun `shift capitalises one letter and then releases`() {
        tap(findKey("⇧")!!)
        assertNotNull("labels should be upper case", findKey("Q"))
        tap(findKey("Q")!!)
        tap(findKey("w")!!)
        assertEquals("Qw", recorder.chars.toString())
    }

    @Test
    fun `holding a letter types the character printed on it`() {
        tap(findKey("q")!!, holdMs = 400)
        assertEquals(listOf("1"), recorder.texts)
        assertEquals("", recorder.chars.toString())
    }

    @Test
    fun `backspace and enter reach the listener`() {
        tap(findKey("⌫")!!)
        tap(findKey("↵")!!)
        assertEquals(1, recorder.backspaces)
        assertEquals(1, recorder.enters)
    }

    @Test
    fun `the size setting changes how tall the keys are`() {
        val heightAt = { step: Int ->
            panel.setLayoutOptions(KeyboardLayoutOptions(sizeScale = Prefs.SIZE_SCALES[step]))
            layOut()
            findKey("q")!!.height
        }
        val compact = heightAt(0)
        val standard = heightAt(2)
        val huge = heightAt(4)
        assertTrue("compact ($compact) should be shorter than standard ($standard)", compact < standard)
        assertTrue("huge ($huge) should be taller than standard ($standard)", huge > standard)
    }

    @Test
    fun `the whole keyboard grows with the size setting`() {
        panel.setLayoutOptions(KeyboardLayoutOptions(sizeScale = Prefs.SIZE_SCALES[0]))
        layOut()
        val small = panel.measuredHeight
        panel.setLayoutOptions(KeyboardLayoutOptions(sizeScale = Prefs.SIZE_SCALES[4]))
        layOut()
        assertTrue("expected a taller keyboard", panel.measuredHeight > small)
    }

    @Test
    fun `the number row can be turned on`() {
        assertNull(findKey("1"))
        panel.setLayoutOptions(KeyboardLayoutOptions(numberRow = true))
        layOut()
        for (c in "1234567890") assertNotNull("missing digit: $c", findKey(c.toString()))
        // With a real number row, holding a letter has nothing left to offer.
        tap(findKey("q")!!, holdMs = 400)
        assertEquals(emptyList<String>(), recorder.texts)
    }

    @Test
    fun `the symbol and emoji layers are reachable and return`() {
        tap(findKey("?123")!!)
        layOut()
        assertNotNull(findKey("#"))

        tap(findKey("ABC")!!)
        layOut()
        assertNotNull(findKey("q"))

        tap(findKey("☺")!!)
        layOut()
        val emoji = findKey("👑")
        assertNotNull("emoji layer should be showing", emoji)
        // Emoji cells and the status chips are ordinary clickable views.
        emoji!!.performClick()
        assertEquals(listOf("👑"), recorder.texts)

        tap(findKey("ABC")!!)
        layOut()
        assertNotNull(findKey("q"))
    }

    @Test
    fun `the status bar offers the tier and a way back to what you typed`() {
        panel.setSource("i think you are nice")
        findKey("Courtly")!!.performClick()
        assertEquals(1, recorder.tierCycles)
        findKey("↺")!!.performClick()
        assertEquals(1, recorder.reverts)
    }

    @Test
    fun `the revert control only appears when there is something to revert`() {
        panel.setSource("")
        assertEquals(View.GONE, findKey("↺")!!.visibility)
        panel.setSource("hello")
        assertEquals(View.VISIBLE, findKey("↺")!!.visibility)
    }

    @Test
    fun `the settings screen opens with a working keyboard in it`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        val output = activity.findViewById<TextView>(R.id.sample_output)
        assertTrue("the sample should be translated", output.text.contains("thou", true))
        val holder = activity.findViewById<ViewGroup>(R.id.preview_keyboard)
        assertTrue("the preview keyboard should be attached", holder.childCount == 1)
    }
}
