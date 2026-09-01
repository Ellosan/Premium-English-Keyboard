package com.premiumenglish.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The keyboard itself: a status bar showing what your words are being turned
 * into, and four rows of keys.
 *
 * Built in code rather than XML because the layers are just lists of keys, and
 * a list of keys is much easier to read than three parallel layout files.
 */
@SuppressLint("ViewConstructor")
class KeyboardPanel(context: Context, private val listener: Listener) : LinearLayout(context) {

    interface Listener {
        fun onChar(c: Char)
        fun onBackspace()
        fun onEnter()
        fun onCycleTier()
        fun onToggleTranslation()
        fun onOpenSettings()
    }

    private enum class Kind { CHAR, SHIFT, BACKSPACE, ENTER, LAYER, SPACE }

    private class Key(
        val label: String,
        val kind: Kind = Kind.CHAR,
        val code: Char = ' ',
        val weight: Float = 1f,
        val layer: Int = 0
    )

    private companion object {
        const val LAYER_LETTERS = 0
        const val LAYER_SYMBOLS = 1
        const val LAYER_MORE = 2

        const val SHIFT_OFF = 0
        const val SHIFT_ONCE = 1
        const val SHIFT_LOCK = 2

        const val REPEAT_DELAY_MS = 400L
        const val REPEAT_RATE_MS = 55L
    }

    private val handler = Handler(Looper.getMainLooper())

    private var layer = LAYER_LETTERS
    private var shift = SHIFT_OFF
    private var translating = true
    private var tier = PremiumEnglish.TIER_COURTLY

    private lateinit var previewView: TextView
    private lateinit var tierChip: TextView
    private lateinit var powerChip: TextView
    private val letterKeys = ArrayList<TextView>()
    private val keyRows = LinearLayout(context).apply { orientation = VERTICAL }

    init {
        orientation = VERTICAL
        setBackgroundColor(color(R.color.keyboard_background))
        addView(buildStatusBar())
        addView(keyRows)
        renderLayer()
    }

    // ------------------------------------------------------------------ public

    /** Shows the premium rendering of whatever is currently being typed. */
    fun setPreview(text: String) {
        previewView.text = if (text.isBlank()) idleHint() else text
        previewView.alpha = if (text.isBlank()) 0.55f else 1f
    }

    fun setTier(tier: Int) {
        this.tier = tier
        tierChip.text = Prefs.tierName(tier)
        setPreview("")
    }

    fun setTranslating(on: Boolean) {
        translating = on
        powerChip.text = if (on) "◆" else "◇"
        powerChip.alpha = if (on) 1f else 0.4f
        tierChip.alpha = if (on) 1f else 0.4f
        setPreview("")
    }

    private fun idleHint(): String =
        if (translating) "Premium English · ${Prefs.tierName(tier)}" else "Translation suspended"

    // ------------------------------------------------------------------ status bar

    private fun buildStatusBar(): View {
        val bar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setBackgroundColor(color(R.color.status_background))
        }

        powerChip = chip("◆") { listener.onToggleTranslation() }
        bar.addView(powerChip)

        previewView = TextView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(color(R.color.preview_text))
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.START
            setPadding(dp(10), 0, dp(10), 0)
        }
        bar.addView(previewView)

        tierChip = chip(Prefs.tierName(tier)) { listener.onCycleTier() }
        bar.addView(tierChip)
        bar.addView(chip("⚙") { listener.onOpenSettings() })
        return bar
    }

    private fun chip(text: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(color(R.color.accent))
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setBackgroundResource(R.drawable.chip_background)
            isClickable = true
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
        }

    // ------------------------------------------------------------------ layers

    private fun rowsFor(layer: Int): List<List<Key>> = when (layer) {
        LAYER_SYMBOLS -> listOf(
            charRow("1234567890"),
            charRow("@#\$%&-+()"),
            listOf(Key("=\\<", Kind.LAYER, weight = 1.5f, layer = LAYER_MORE)) +
                charRow("*\"':;!?") +
                listOf(Key("⌫", Kind.BACKSPACE, weight = 1.5f)),
            bottomRow("ABC", LAYER_LETTERS)
        )
        LAYER_MORE -> listOf(
            charRow("~`|•√π÷×¶"),
            charRow("£¢€¥^°={}"),
            listOf(Key("?123", Kind.LAYER, weight = 1.5f, layer = LAYER_SYMBOLS)) +
                charRow("\\©®™[]") +
                listOf(Key("⌫", Kind.BACKSPACE, weight = 1.5f)),
            bottomRow("ABC", LAYER_LETTERS)
        )
        else -> listOf(
            charRow("qwertyuiop"),
            charRow("asdfghjkl"),
            listOf(Key("⇧", Kind.SHIFT, weight = 1.5f)) +
                charRow("zxcvbnm") +
                listOf(Key("⌫", Kind.BACKSPACE, weight = 1.5f)),
            bottomRow("?123", LAYER_SYMBOLS)
        )
    }

    private fun charRow(chars: String): List<Key> = chars.map { Key(it.toString(), Kind.CHAR, it) }

    private fun bottomRow(layerLabel: String, target: Int): List<Key> = listOf(
        Key(layerLabel, Kind.LAYER, weight = 1.5f, layer = target),
        Key(",", Kind.CHAR, ','),
        Key("space", Kind.SPACE, ' ', weight = 5f),
        Key(".", Kind.CHAR, '.'),
        Key("↵", Kind.ENTER, weight = 1.5f)
    )

    private fun renderLayer() {
        keyRows.removeAllViews()
        letterKeys.clear()
        for (row in rowsFor(layer)) {
            val rowView = LinearLayout(context).apply {
                orientation = HORIZONTAL
                setPadding(dp(3), dp(3), dp(3), dp(3))
            }
            for (key in row) rowView.addView(buildKey(key))
            keyRows.addView(rowView)
        }
        applyShiftToLabels()
    }

    private fun buildKey(key: Key): View {
        val view = TextView(context).apply {
            text = key.label
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (key.kind == Kind.CHAR) 19f else 15f)
            setTextColor(color(if (key.kind == Kind.ENTER) R.color.key_accent_text else R.color.key_text))
            setBackgroundResource(
                when (key.kind) {
                    Kind.CHAR -> R.drawable.key_background
                    Kind.ENTER -> R.drawable.key_background_accent
                    else -> R.drawable.key_background_modifier
                }
            )
            layoutParams = LayoutParams(0, dp(46), key.weight).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
            isClickable = true
        }
        if (key.kind == Kind.CHAR && key.code.isLetter()) letterKeys.add(view)

        if (key.kind == Kind.BACKSPACE) {
            attachRepeat(view) { listener.onBackspace() }
        } else {
            view.setOnClickListener { press(it, key) }
        }
        return view
    }

    private fun press(view: View, key: Key) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        when (key.kind) {
            Kind.CHAR -> {
                listener.onChar(if (shift != SHIFT_OFF && key.code.isLetter()) key.code.uppercaseChar() else key.code)
                if (shift == SHIFT_ONCE) {
                    shift = SHIFT_OFF
                    applyShiftToLabels()
                }
            }
            Kind.SPACE -> listener.onChar(' ')
            Kind.ENTER -> listener.onEnter()
            Kind.BACKSPACE -> listener.onBackspace()
            Kind.SHIFT -> {
                shift = when (shift) {
                    SHIFT_OFF -> SHIFT_ONCE
                    SHIFT_ONCE -> SHIFT_LOCK
                    else -> SHIFT_OFF
                }
                applyShiftToLabels()
            }
            Kind.LAYER -> {
                layer = key.layer
                renderLayer()
            }
        }
    }

    /** Hold to delete: one press, then a steady stream. */
    private fun attachRepeat(view: View, action: () -> Unit) {
        val repeat = object : Runnable {
            override fun run() {
                action()
                handler.postDelayed(this, REPEAT_RATE_MS)
            }
        }
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    action()
                    handler.postDelayed(repeat, REPEAT_DELAY_MS)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    handler.removeCallbacks(repeat)
                    true
                }
                else -> false
            }
        }
    }

    private fun applyShiftToLabels() {
        val upper = shift != SHIFT_OFF
        for (view in letterKeys) {
            val label = view.text.toString()
            view.text = if (upper) label.uppercase() else label.lowercase()
        }
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    // ------------------------------------------------------------------ helpers

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    @Suppress("DEPRECATION")
    private fun color(id: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) resources.getColor(id, null)
        else resources.getColor(id)
}
