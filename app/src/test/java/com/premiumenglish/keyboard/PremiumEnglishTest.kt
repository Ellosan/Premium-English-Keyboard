package com.premiumenglish.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumEnglishTest {

    private val refined = PremiumOptions(PremiumEnglish.TIER_REFINED)

    private fun refined(text: String) = PremiumEnglish.translate(text, refined)

    // ------------------------------------------------------------------ vocabulary

    @Test
    fun `refined tier lifts the register without archaic pronouns`() {
        assertEquals("Greetings, how are you?", PremiumEnglish.translate("hello, how are you?", refined))
    }

    @Test
    fun `the refined tier never turns archaic`() {
        // Refined is a lift in register, not a costume: anything below belongs
        // to Courtly and above.
        val archaic = listOf(
            "thou", "thee", "thy", "thine", "hath", "dost", "doth", "hast",
            "methinks", "verily", "prithee", "forsooth", "nay", "aye", "anon",
            "mayhap", "ere", "oft", "eth "
        )
        val sentences = listOf(
            "hello, how are you? i think you look great today",
            "i don't know what you want, but please tell me",
            "he runs to the shop every morning and buys a beer",
            "no way, i have to go home right now",
            "thanks a lot, see you later. oh my god, what time is it?",
            "she says it seems fine, anyway i guess we will figure out something"
        )
        for (sentence in sentences) {
            val out = PremiumEnglish.translate(sentence, refined, finished = true).lowercase()
            for (word in archaic) {
                assertFalse(
                    "refined output \"$out\" should not contain \"$word\"",
                    Regex("\\b" + Regex.escape(word.trim()) + "\\b").containsMatchIn(out)
                )
            }
        }
    }

    // ------------------------------------------------------------------ pronouns

    // ------------------------------------------------------------------ agreement

    // ------------------------------------------------------------------ sovereign

    // ------------------------------------------------------------------ mechanics

    @Test
    fun `contractions are expanded before translation`() {
        assertEquals("You are late", refined("you're late"))
        assertEquals("I am late", refined("i'm late"))
    }

    @Test
    fun `capitalisation is preserved, and shouting stays shouted`() {
        assertEquals("Greetings", refined("Hello"))
        assertEquals("GREETINGS", refined("HELLO"))
        assertEquals("VERY WELL", refined("OK"))
    }

    @Test
    fun `whitespace and punctuation come back unchanged`() {
        assertEquals("Greetings ", refined("hello "))
        assertEquals("  Greetings", refined("  hello"))
        assertEquals("Greetings... No!", refined("hello... no!"))
    }

    @Test
    fun `blank input is returned untouched`() {
        assertEquals("", refined(""))
        assertEquals("   ", refined("   "))
    }

    // ------------------------------------------------------------------ live typing

    @Test
    fun `live typing never loses the trailing space the user typed`() {
        for (n in 1.."i think you are nice".length) {
            val typed = "i think you are nice".substring(0, n)
            val out = PremiumEnglish.translateLive(typed, refined)
            assertEquals(
                "trailing space mismatch for \"$typed\"",
                typed.endsWith(" "),
                out.endsWith(" ")
            )
        }
    }

    // ------------------------------------------------------------------ ceremony

    @Test
    fun `flourishes appear only on finished sentences`() {
        val opts = PremiumOptions(PremiumEnglish.TIER_REFINED, flourish = true)
        val sentences = listOf("he runs", "she is here", "they left", "it is done", "we arrived")

        // Unfinished sentences are never decorated.
        for (sentence in sentences) {
            val plain = PremiumEnglish.translate(sentence, opts, finished = false)
            assertEquals(PremiumEnglish.translate(sentence, opts.copy(flourish = false)), plain)
        }

        // Finished ones sometimes are — which one is decided by a hash of the
        // sentence, so check that it happens at all rather than to any one.
        val decorated = sentences.count { sentence ->
            val finished = PremiumEnglish.translate("$sentence.", opts, finished = true)
            finished.length > PremiumEnglish.translate("$sentence.", opts.copy(flourish = false)).length
        }
        assertTrue("expected at least one flourish across $sentences", decorated > 0)
    }

    @Test
    fun `the same sentence always draws the same flourish`() {
        val opts = PremiumOptions(PremiumEnglish.TIER_REFINED, flourish = true)
        val first = PremiumEnglish.translate("she is here.", opts, finished = true)
        repeat(20) {
            assertEquals(first, PremiumEnglish.translate("she is here.", opts, finished = true))
        }
    }

    @Test
    fun `translation terminates on adversarial input`() {
        val noise = "aaa bbb ccc ??? !!! ''' --- 123 \n\t hello you the a an"
        for (tier in 1..3) {
            val opts = PremiumOptions(tier, flourish = true, oldeSpelling = true)
            PremiumEnglish.translate(noise, opts, finished = true)
            PremiumEnglish.translateLive(noise, opts)
        }
    }
}
