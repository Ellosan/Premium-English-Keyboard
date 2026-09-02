package com.premiumenglish.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the typing loop: what ends up in the field after each keystroke, and
 * what happens when it is taken back.
 */
class SegmentBufferTest {

    /** A text field with the cursor at the end, which is the case while typing. */
    private class FakeField : TextTarget {
        val text = StringBuilder()

        override fun replace(before: Int, text: String) {
            this.text.setLength((this.text.length - before).coerceAtLeast(0))
            this.text.append(text)
        }

        override fun textBeforeCursor(count: Int): CharSequence =
            text.substring((text.length - count).coerceAtLeast(0))

        override fun sendBackspace() {
            if (text.isNotEmpty()) text.setLength(text.length - 1)
        }
    }

    private val field = FakeField()
    private var settings = SegmentSettings(PremiumOptions(PremiumEnglish.TIER_COURTLY))
    private val buffer = SegmentBuffer(field) { settings }

    private fun type(text: String) = text.forEach { buffer.type(it) }

    // ------------------------------------------------------------------ typing

    @Test
    fun `typing key by key lands in the same place as one full translation`() {
        val message = "hello. how are you? i think you look great today. can you help me?"
        type(message)
        assertEquals(
            PremiumEnglish.translate(message, settings.options, finished = true),
            field.text.toString()
        )
    }

    @Test
    fun `the word being typed is left alone until it is finished`() {
        type("i think you are nic")
        assertEquals("Methinks thou art nic", field.text.toString())
        buffer.type('e')
        buffer.type(' ')
        assertEquals("Methinks thou art pleasant ", field.text.toString())
    }

    @Test
    fun `a new sentence starts a new segment`() {
        type("hello. you are late")
        assertEquals("Hail. Thou art late", field.text.toString())
    }

    // ------------------------------------------------------------------ taking it back

    @Test
    fun `backspace rewinds the words that were typed, not the ornate ones`() {
        type("i think you are nice")
        repeat(6) { buffer.backspace() }
        assertEquals("i think you ar", buffer.source)
        assertEquals(PremiumEnglish.translateLive("i think you ar", settings.options), field.text.toString())
    }

    @Test
    fun `backspacing past the start clears the field`() {
        type("hello")
        repeat(20) { buffer.backspace() }
        assertEquals("", field.text.toString())
    }

    @Test
    fun `revert puts back exactly what was typed`() {
        type("i think you are nice")
        buffer.revert()
        assertEquals("i think you are nice", field.text.toString())
        assertTrue("the segment should be finished with", buffer.isEmpty)
        // And typing carries on from there without disturbing it.
        type(" hello")
        assertEquals("i think you are nice hello", field.text.toString())
    }

    // ------------------------------------------------------------------ conveniences

    @Test
    fun `two spaces after a word become a full stop`() {
        type("hello  ")
        assertEquals("Hail. ", field.text.toString())
    }

    @Test
    fun `two spaces do nothing when there is no word in front of them`() {
        type(" ")
        buffer.type(' ')
        assertEquals("  ", field.text.toString())
    }

    @Test
    fun `the double space full stop can be turned off`() {
        settings = settings.copy(doubleSpacePeriod = false)
        type("hello  ")
        assertEquals("Hail  ", field.text.toString())
    }

    @Test
    fun `capitals are offered at the start and after a sentence`() {
        assertTrue(buffer.atSentenceStart())
        type("hello")
        assertFalse(buffer.atSentenceStart())
        type(". ")
        assertTrue(buffer.atSentenceStart())
        type("you")
        assertFalse(buffer.atSentenceStart())
    }

    // ------------------------------------------------------------------ switched off

    @Test
    fun `with translation off it is an ordinary keyboard`() {
        settings = settings.copy(translate = false)
        type("i think you are nice")
        assertEquals("i think you are nice", field.text.toString())
        buffer.backspace()
        assertEquals("i think you are nic", field.text.toString())
    }

    @Test
    fun `the double space full stop still works with translation off`() {
        settings = settings.copy(translate = false)
        type("hello  ")
        assertEquals("hello. ", field.text.toString())
    }

    // ------------------------------------------------------------------ tiers

    @Test
    fun `changing tier mid-sentence redraws what is already there`() {
        type("hello there ")
        assertEquals("Hail there ", field.text.toString())
        settings = settings.copy(options = PremiumOptions(PremiumEnglish.TIER_REFINED))
        buffer.retranslate()
        assertEquals("Greetings there ", field.text.toString())
    }

    @Test
    fun `emoji and held symbols go in unharmed`() {
        type("hello ")
        buffer.type("👑")
        assertEquals("Hail 👑", field.text.toString())
    }
}
