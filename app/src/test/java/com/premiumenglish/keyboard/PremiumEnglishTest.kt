package com.premiumenglish.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumEnglishTest {

    private val refined = PremiumOptions(PremiumEnglish.TIER_REFINED)
    private val courtly = PremiumOptions(PremiumEnglish.TIER_COURTLY)
    private val sovereign = PremiumOptions(PremiumEnglish.TIER_SOVEREIGN)

    private fun courtly(text: String) = PremiumEnglish.translate(text, courtly)
    private fun sovereign(text: String) = PremiumEnglish.translate(text, sovereign)

    // ------------------------------------------------------------------ vocabulary

    @Test
    fun `refined tier lifts the register without archaic pronouns`() {
        assertEquals("Greetings, how are you?", PremiumEnglish.translate("hello, how are you?", refined))
    }

    @Test
    fun `tiers stack, so a word can climb through all three`() {
        assertEquals("Greetings", PremiumEnglish.translate("hello", refined))
        assertEquals("Hail", PremiumEnglish.translate("hello", courtly))
        assertEquals("Hail and most hearty greeting", PremiumEnglish.translate("hello", sovereign))
    }

    @Test
    fun `phrases beat the individual words inside them`() {
        assertEquals("I thank thee most heartily", courtly("thank you very much"))
        assertEquals("Methinks so", courtly("i think so"))
    }

    // ------------------------------------------------------------------ pronouns

    @Test
    fun `you is thou as a subject and thee as an object`() {
        assertEquals("Thou art wise", courtly("you are smart"))
        assertEquals("I will convey thee a missive", courtly("i will send you a message"))
    }

    @Test
    fun `possessives take thy, or thine before a vowel`() {
        assertEquals("Thy hound", courtly("your dog"))
        assertEquals("Thine abode", courtly("your house"))
    }

    // ------------------------------------------------------------------ agreement

    @Test
    fun `thou puts the -est ending on its verb`() {
        assertEquals("Thou knowest the way", courtly("you know the way"))
        assertEquals("Thou perambulatest apace", courtly("you walk quickly"))
    }

    @Test
    fun `an inverted auxiliary carries the marking instead of the verb`() {
        assertEquals("Dost thou know the way?", courtly("do you know the way?"))
        assertEquals("Canst thou aid me?", courtly("can you help me?"))
    }

    @Test
    fun `third person singular takes -eth`() {
        assertEquals("He runneth", courtly("he runs"))
        assertEquals("She hath a hound", courtly("she has a dog"))
        assertEquals("He seemeth so", courtly("he seems so"))
    }

    @Test
    fun `plural nouns ending in s are left alone`() {
        assertEquals("The matters I procure", courtly("the things i get"))
        assertEquals("My companions are delighted", refinedPlural())
    }

    private fun refinedPlural() = PremiumEnglish.translate("my friends are happy", refined)

    @Test
    fun `spelling rules survive the archaic endings`() {
        assertEquals("Thou criest", courtly("you cry"))
        assertEquals("He teacheth", courtly("he teaches"))
        assertEquals("Thou singest", courtly("you sing"))
        assertEquals("Thou cherishest", courtly("you love"))
    }

    // ------------------------------------------------------------------ sovereign

    @Test
    fun `sovereign tier drops the propped-up auxiliary`() {
        assertEquals("I know not what thou covetest", sovereign("i don't know what you want"))
        assertEquals("Knowest thou the way?", sovereign("do you know the way?"))
    }

    @Test
    fun `olde spellings are opt-in`() {
        val plain = PremiumOptions(PremiumEnglish.TIER_SOVEREIGN)
        val olde = PremiumOptions(PremiumEnglish.TIER_SOVEREIGN, oldeSpelling = true)
        assertEquals("The light", PremiumEnglish.translate("the light", plain))
        assertEquals("Ye lyght", PremiumEnglish.translate("the light", olde))
    }

    // ------------------------------------------------------------------ mechanics

    @Test
    fun `contractions are expanded before translation`() {
        assertEquals("Thou art late", courtly("you're late"))
        assertEquals("I am late", courtly("i'm late"))
    }

    @Test
    fun `articles follow the word that replaced the old one`() {
        assertEquals("An ale", courtly("a beer"))
    }

    @Test
    fun `capitalisation is preserved, and shouting stays shouted`() {
        assertEquals("Hail", courtly("Hello"))
        assertEquals("HAIL", courtly("HELLO"))
        assertEquals("VERY WELL", courtly("OK"))
    }

    @Test
    fun `whitespace and punctuation come back unchanged`() {
        assertEquals("Hail ", courtly("hello "))
        assertEquals("  Hail", courtly("  hello"))
        assertEquals("Hail... Nay!", courtly("hello... no!"))
    }

    @Test
    fun `blank input is returned untouched`() {
        assertEquals("", courtly(""))
        assertEquals("   ", courtly("   "))
    }

    // ------------------------------------------------------------------ live typing

    @Test
    fun `the word under the thumb is left alone until it is finished`() {
        assertEquals("I thin", PremiumEnglish.translateLive("i thin", courtly))
        assertEquals("Methinks ", PremiumEnglish.translateLive("i think ", courtly))
        assertEquals("Methinks thou art l", PremiumEnglish.translateLive("i think you are l", courtly))
    }

    @Test
    fun `live typing never loses the trailing space the user typed`() {
        for (n in 1.."i think you are nice".length) {
            val typed = "i think you are nice".substring(0, n)
            val out = PremiumEnglish.translateLive(typed, courtly)
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
        val opts = PremiumOptions(PremiumEnglish.TIER_SOVEREIGN, flourish = true)
        val unfinished = PremiumEnglish.translate("he runs", opts, finished = false)
        val finished = PremiumEnglish.translate("he runs.", opts, finished = true)
        assertEquals("He runneth", unfinished)
        assertTrue("expected a flourish, got: $finished", finished.length > "He runneth.".length)
    }

    @Test
    fun `the same sentence always draws the same flourish`() {
        val opts = PremiumOptions(PremiumEnglish.TIER_SOVEREIGN, flourish = true)
        val first = PremiumEnglish.translate("he runs.", opts, finished = true)
        repeat(20) {
            assertEquals(first, PremiumEnglish.translate("he runs.", opts, finished = true))
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
