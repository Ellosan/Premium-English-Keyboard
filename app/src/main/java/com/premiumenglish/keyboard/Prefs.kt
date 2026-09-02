package com.premiumenglish.keyboard

import android.content.Context
import android.content.SharedPreferences

/** The user's standing instructions, shared between the keyboard and its settings screen. */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    // ---------------------------------------------------------------- translation

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

    // ---------------------------------------------------------------- keyboard

    /** Index into [SIZE_SCALES]. */
    var sizeStep: Int
        get() = sp.getInt(KEY_SIZE, DEFAULT_SIZE_STEP).coerceIn(0, SIZE_SCALES.size - 1)
        set(value) = sp.edit().putInt(KEY_SIZE, value.coerceIn(0, SIZE_SCALES.size - 1)).apply()

    /** Multiplier applied to the height of every key. */
    val sizeScale: Float get() = SIZE_SCALES[sizeStep]

    var numberRow: Boolean
        get() = sp.getBoolean(KEY_NUMBER_ROW, false)
        set(value) = sp.edit().putBoolean(KEY_NUMBER_ROW, value).apply()

    var vibrate: Boolean
        get() = sp.getBoolean(KEY_VIBRATE, true)
        set(value) = sp.edit().putBoolean(KEY_VIBRATE, value).apply()

    var keyPreview: Boolean
        get() = sp.getBoolean(KEY_PREVIEW, true)
        set(value) = sp.edit().putBoolean(KEY_PREVIEW, value).apply()

    var autoCapitalize: Boolean
        get() = sp.getBoolean(KEY_AUTO_CAPS, true)
        set(value) = sp.edit().putBoolean(KEY_AUTO_CAPS, value).apply()

    /** Two spaces in a row become a full stop and a space. */
    var doubleSpacePeriod: Boolean
        get() = sp.getBoolean(KEY_DOUBLE_SPACE, true)
        set(value) = sp.edit().putBoolean(KEY_DOUBLE_SPACE, value).apply()

    fun layout(): KeyboardLayoutOptions =
        KeyboardLayoutOptions(sizeScale, numberRow, vibrate, keyPreview)

    companion object {
        private const val NAME = "premium_english"
        private const val KEY_TIER = "tier"
        private const val KEY_AUTO = "auto_translate"
        private const val KEY_FLOURISH = "flourish"
        private const val KEY_OLDE = "olde_spelling"
        private const val KEY_SIZE = "size_step"
        private const val KEY_NUMBER_ROW = "number_row"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_PREVIEW = "key_preview"
        private const val KEY_AUTO_CAPS = "auto_capitalize"
        private const val KEY_DOUBLE_SPACE = "double_space_period"

        val SIZE_SCALES = floatArrayOf(0.78f, 0.89f, 1.0f, 1.15f, 1.32f)
        val SIZE_NAMES = arrayOf("Compact", "Small", "Standard", "Large", "Huge")
        const val DEFAULT_SIZE_STEP = 2

        fun tierName(tier: Int): String = when (tier) {
            PremiumEnglish.TIER_REFINED -> "Refined"
            PremiumEnglish.TIER_SOVEREIGN -> "Sovereign"
            else -> "Courtly"
        }
    }
}

/** Everything the keyboard view needs to know about how it should look and feel. */
data class KeyboardLayoutOptions(
    val sizeScale: Float = 1f,
    val numberRow: Boolean = false,
    val vibrate: Boolean = true,
    val keyPreview: Boolean = true
)
