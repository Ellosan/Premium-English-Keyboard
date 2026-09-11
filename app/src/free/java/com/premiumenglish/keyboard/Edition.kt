package com.premiumenglish.keyboard

/**
 * The free edition.
 *
 * Premium English ships in two editions, and this is the half of the split
 * that is open source. The free edition offers the Refined tier: an ordinary
 * keyboard that lifts the register of what you write. Courtly and Sovereign —
 * thou and thee, the -est and -eth endings, the archaic vocabulary and the
 * ceremonial flourishes — belong to the Pro edition, along with the several
 * hundred lexicon entries behind them.
 *
 * The tables are not withheld at runtime; they are simply not in this build.
 * There is no flag to flip and nothing to unlock, which is why [MAX_TIER] is
 * the honest answer to what this edition can do rather than a check that could
 * be patched out.
 *
 * The Pro edition supplies its own version of this file.
 */
object Edition {

    /** Shown in the settings screen so each build says which one it is. */
    const val NAME = "Free"

    const val IS_PRO = false

    /** The highest tier this build is capable of. */
    const val MAX_TIER = PremiumEnglish.TIER_REFINED

    /** Where the Pro edition is sold. */
    const val STORE_URL = "https://ellosan.itch.io/premium-english-keyboard"

    val words: Map<Int, Map<String, String>> = emptyMap()
    val phrases: Map<Int, Map<String, String>> = emptyMap()
    val openers: Map<Int, List<String>> = emptyMap()
    val closers: Map<Int, List<String>> = emptyMap()
    val oldeSpellings: Map<String, String> = emptyMap()
}
