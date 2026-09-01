package com.premiumenglish.keyboard

import android.content.Context
import android.content.SharedPreferences

/** The user's standing instructions, shared between the keyboard and its settings screen. */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var tier: Int
        get() = sp.getInt(KEY_TIER, PremiumEnglish.TIER_COURTLY)
            .coerceIn(PremiumEnglish.TIER_REFINED, PremiumEnglish.TIER_SOVEREIGN)
        set(value) = sp.edit().putInt(KEY_TIER, value).apply()

    /** Master switch: off turns this into an ordinary, regrettably modern keyboard. */
    var autoTranslate: Boolean
        get() = sp.getBoolean(KEY_AUTO, true)
        set(value) = sp.edit().putBoolean(KEY_AUTO, value).apply()

    /** Ceremonial openers and closers on finished sentences. */
    var flourish: Boolean
        get() = sp.getBoolean(KEY_FLOURISH, false)
        set(value) = sp.edit().putBoolean(KEY_FLOURISH, value).apply()

    /** Faux-antique respellings, Sovereign tier only. */
    var oldeSpelling: Boolean
        get() = sp.getBoolean(KEY_OLDE, false)
        set(value) = sp.edit().putBoolean(KEY_OLDE, value).apply()

    fun options(): PremiumOptions = PremiumOptions(tier, flourish, oldeSpelling)

    companion object {
        private const val NAME = "premium_english"
        private const val KEY_TIER = "tier"
        private const val KEY_AUTO = "auto_translate"
        private const val KEY_FLOURISH = "flourish"
        private const val KEY_OLDE = "olde_spelling"

        fun tierName(tier: Int): String = when (tier) {
            PremiumEnglish.TIER_REFINED -> "Refined"
            PremiumEnglish.TIER_SOVEREIGN -> "Sovereign"
            else -> "Courtly"
        }
    }
}
