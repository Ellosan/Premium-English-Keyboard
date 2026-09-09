package com.premiumenglish.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the typing loop: what ends up in the field as you type, what the
 * Translate key does to it, and what happens when it is taken back.
 */
class SegmentBufferTest {

    /** A text field with the cursor at the end, which is the case while typing. */
    private class FakeField : TextTarget {
        val text = StringBuilder()

        override fun replace(before: Int, after: Int, text: String) {
            this.text.setLength((this.text.length - before).coerceAtLeast(0))
            this.text.append(text)
        }

        override fun textBeforeCursor(count: Int): CharSequence =
            text.substring((text.length - count).coerceAtLeast(0))

        override fun textAfterCursor(count: Int): CharSequence = ""

        override fun sendBackspace() {
            if (text.isNotEmpty()) text.setLength(text.length - 1)
        }
    }

    private val field = FakeField()
    private var settings = SegmentSettings(PremiumOptions(PremiumEnglish.TIER_COURTLY))
    private val buffer = SegmentBuffer(field) { settings }

    private fun type(text: String) = text.forEach { buffer.type(it) }
    private fun goLive() { settings = settings.copy(live = true) }

    // ------------------------------------------------------------------ typing

    @Test
    fun `typing leaves your words alone until you ask`() {
        type("i think you are nice")
        assertEquals("i think you are nice", field.text.toString())
    }

    @Test
    fun `the translate key does the whole message at once`() {
        type("hello. i think you are nice")
        assertTrue(buffer.translateNow())
        assertEquals("Hail. Methinks thou art pleasant", field.text.toString())
    }

    @Test
    fun `typing carries on after a translation, and translates only the new part`() {
        type("hello")
        buffer.translateNow()
        type(" i think you are nice")
        buffer.translateNow()
        assertEquals("Hail Methinks thou art pleasant", field.text.toString())
    }

    @Test
    fun `pressing translate twice does not translate the output again`() {
        type("hello")
        assertTrue(buffer.translateNow())
        val once = field.text.toString()
        assertFalse("the second press should do nothing", buffer.translateNow())
        assertEquals(once, field.text.toString())
    }

    @Test
    fun `translating an empty message does nothing`() {
        assertFalse(buffer.translateNow())
        assertEquals("", field.text.toString())
    }

    @Test
    fun `text already in the field can be translated`() {
        // Nothing was typed on this keyboard: the field was filled elsewhere.
        field.text.append("you are late")
        assertTrue(buffer.translateNow())
        assertEquals("Thou art tardy", field.text.toString())
    }

    @Test
    fun `backspace deletes what was typed`() {
        type("hello")
        repeat(2) { buffer.backspace() }
        assertEquals("hel", field.text.toString())
        assertEquals("hel", buffer.source)
    }

    @Test
    fun `the preview shows what the translate key would produce`() {
        assertEquals("", buffer.preview())
        type("i think you are nice")
        assertEquals("Methinks thou art pleasant", buffer.preview())
        // The field itself is untouched until the key is pressed.
        assertEquals("i think you are nice", field.text.toString())
    }

    @Test
    fun `the preview is exactly what the key delivers`() {
        type("hello. i think you look great today")
        val promised = buffer.preview()
        buffer.translateNow()
        assertEquals(promised, field.text.toString())
    }

    // ------------------------------------------------------------------ taking it back

    @Test
    fun `revert puts the typed message back after a translation`() {
        type("i think you are nice")
        buffer.translateNow()
        assertEquals("Methinks thou art pleasant", field.text.toString())
        assertTrue(buffer.revert())
        assertEquals("i think you are nice", field.text.toString())
    }

    @Test
    fun `a reverted message can be translated again`() {
        type("hello")
        buffer.translateNow()
        buffer.revert()
        assertEquals("hello", field.text.toString())
        assertTrue(buffer.translateNow())
        assertEquals("Hail", field.text.toString())
    }

    @Test
    fun `there is nothing to revert on an untouched field`() {
        assertFalse(buffer.canRevert)
        type("hi")
        assertTrue(buffer.canRevert)
    }

    // ------------------------------------------------------------------ conveniences

    @Test
    fun `two spaces after a word become a full stop`() {
        type("hello  ")
        assertEquals("hello. ", field.text.toString())
        buffer.translateNow()
        assertEquals("Hail. ", field.text.toString())
    }

    @Test
    fun `capitals are offered at the start and after a sentence`() {
        assertTrue(buffer.atSentenceStart())
        type("hello")
        assertFalse(buffer.atSentenceStart())
        type(". ")
        assertTrue(buffer.atSentenceStart())
    }

    @Test
    fun `with translation off it is an ordinary keyboard`() {
        settings = settings.copy(translate = false)
        type("i think you are nice")
        assertEquals("i think you are nice", field.text.toString())
        assertFalse(buffer.translateNow())
    }

    @Test
    fun `emoji and held symbols go in unharmed`() {
        type("hello ")
        buffer.type("👑")
        buffer.translateNow()
        assertEquals("Hail 👑", field.text.toString())
    }

    // ------------------------------------------------------------------ live mode

    @Test
    fun `live mode translates as you type`() {
        goLive()
        val message = "hello. how are you? i think you look great today."
        type(message)
        assertEquals(
            PremiumEnglish.translate(message, settings.options, finished = true),
            field.text.toString()
        )
    }

    @Test
    fun `live mode leaves the word being typed alone until it is finished`() {
        goLive()
        type("i think you are nic")
        assertEquals("Methinks thou art nic", field.text.toString())
        type("e ")
        assertEquals("Methinks thou art pleasant ", field.text.toString())
    }

    @Test
    fun `live mode backspace rewinds the typed words, not the ornate ones`() {
        goLive()
        type("i think you are nice")
        repeat(6) { buffer.backspace() }
        assertEquals("i think you ar", buffer.source)
        assertEquals(
            PremiumEnglish.translateLive("i think you ar", settings.options),
            field.text.toString()
        )
    }

    @Test
    fun `live mode revert puts back exactly what was typed`() {
        goLive()
        type("i think you are nice")
        assertTrue(buffer.revert())
        assertEquals("i think you are nice", field.text.toString())
    }

    @Test
    fun `changing tier mid-sentence redraws live text`() {
        goLive()
        type("hello there ")
        assertEquals("Hail there ", field.text.toString())
        settings = settings.copy(options = PremiumOptions(PremiumEnglish.TIER_REFINED))
        buffer.retranslate()
        assertEquals("Greetings there ", field.text.toString())
    }

    @Test
    fun `live mode still turns two spaces into a full stop`() {
        goLive()
        type("hello  ")
        assertEquals("Hail. ", field.text.toString())
    }
}
