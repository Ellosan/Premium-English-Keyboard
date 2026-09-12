package com.premiumenglish.keyboard

import android.view.View
import android.widget.RadioButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * What the free edition is, and is not.
 *
 * The premium tiers are not withheld by a check that could be patched out —
 * the tables are not in this build at all. These tests state that plainly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FreeEditionTest {

    @Test
    fun `the free edition stops at the refined tier`() {
        assertFalse(Edition.IS_PRO)
        assertEquals(PremiumEnglish.TIER_REFINED, Edition.MAX_TIER)
    }

    @Test
    fun `asking for a premium tier still gives refined english`() {
        val asked = PremiumEnglish.translate(
            "hello, how are you?",
            PremiumOptions(PremiumEnglish.TIER_SOVEREIGN, flourish = true, oldeSpelling = true),
            finished = true
        )
        assertFalse("no thou in the free edition: $asked", asked.contains("thou", ignoreCase = true))
        assertFalse("no hath in the free edition: $asked", asked.contains("hath", ignoreCase = true))
    }

    @Test
    fun `the premium tables are absent, not merely unused`() {
        assertEquals(emptyMap<Int, Map<String, String>>(), Edition.words)
        assertEquals(emptyMap<Int, Map<String, String>>(), Edition.phrases)
        assertEquals(emptyMap<String, String>(), Edition.oldeSpellings)
        // Only tier one is in the lexicon at all.
        assertEquals(setOf(1), Lexicon.WORDS.keys)
        assertEquals(setOf(1), Lexicon.PHRASES.keys)
    }

    @Test
    fun `the settings screen offers the upgrade and locks the premium tiers`() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        assertEquals(
            View.VISIBLE,
            activity.findViewById<View>(R.id.upgrade_card).visibility
        )
        assertFalse(activity.findViewById<RadioButton>(R.id.tier_courtly).isEnabled)
        assertFalse(activity.findViewById<RadioButton>(R.id.tier_sovereign).isEnabled)
        assertTrue(activity.findViewById<RadioButton>(R.id.tier_refined).isEnabled)
    }

    @Test
    fun `settings cannot store a tier this edition cannot reach`() {
        val prefs = Prefs(RuntimeEnvironment.getApplication())
        prefs.tier = PremiumEnglish.TIER_SOVEREIGN
        assertEquals(PremiumEnglish.TIER_REFINED, prefs.tier)
        assertFalse(prefs.canChooseTier)
    }
}
