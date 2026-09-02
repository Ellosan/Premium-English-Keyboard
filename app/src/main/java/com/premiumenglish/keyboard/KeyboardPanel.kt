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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * The keyboard itself: a status bar showing what you actually typed, and the
 * rows of keys.
 *
 * Built in code rather than XML because a layer is just a list of keys, which
 * is far easier to read than four parallel layout files — and because the key
 * height has to be recomputed whenever the size setting changes.
 */
@SuppressLint("ViewConstructor")
class KeyboardPanel(context: Context, private val listener: Listener) : FrameLayout(context) {

    interface Listener {
        fun onChar(c: Char)
        fun onText(text: String)
        fun onBackspace()
        fun onEnter()
        fun onCycleTier()
        fun onToggleTranslation()
        fun onOpenSettings()
        fun onSwitchKeyboard()
        fun onRevert()
    }

    private enum class Kind { CHAR, SHIFT, BACKSPACE, ENTER, LAYER, SPACE }

    private class Key(
        val label: String,
        val kind: Kind = Kind.CHAR,
        val output: String = label,
        /** Typed on a long press, the way Gboard puts digits on the top row. */
        val secondary: String? = null,
        val weight: Float = 1f,
        val layer: Int = -1
    )

    private companion object {
        const val LAYER_LETTERS = 0
        const val LAYER_SYMBOLS = 1
        const val LAYER_MORE = 2
        const val LAYER_EMOJI = 3

        const val SHIFT_OFF = 0
        const val SHIFT_ONCE = 1
        const val SHIFT_LOCK = 2

        const val BASE_KEY_HEIGHT_DP = 46
        const val LONG_PRESS_MS = 300L
        const val REPEAT_DELAY_MS = 380L
        const val REPEAT_RATE_MS = 55L

        /** Digits and punctuation reachable by holding a letter, as on Gboard. */
        val SECONDARIES = mapOf(
            'q' to "1", 'w' to "2", 'e' to "3", 'r' to "4", 't' to "5",
            'y' to "6", 'u' to "7", 'i' to "8", 'o' to "9", 'p' to "0",
            'a' to "@", 's' to "#", 'd' to "$", 'f' to "_", 'g' to "&",
            'h' to "-", 'j' to "+", 'k' to "(", 'l' to ")",
            'z' to "*", 'x' to "\"", 'c' to "'", 'v' to ":", 'b' to ";",
            'n' to "!", 'm' to "?"
        )

        val EMOJI = listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
            "🙂", "😉", "😊", "😍", "😘", "😗", "🤗", "🤔",
            "😐", "😴", "😪", "😥", "😢", "😭", "😤", "😠",
            "🤯", "😳", "🥺", "😬", "🙄", "😏", "😎", "🥳",
            "👍", "👎", "👌", "🙏", "👏", "💪", "🤝", "✍️",
            "❤️", "💔", "✨", "🔥", "🎉", "🎁", "👑", "⚔️",
            "🐴", "🐕", "🦉", "🌹", "🌙", "☀️", "⛈️", "🕯️",
            "🍺", "🍷", "☕", "🍞", "🧀", "🏰", "📜", "🗝️"
        )
    }

    private val handler = Handler(Looper.getMainLooper())

    private var layer = LAYER_LETTERS
    private var shift = SHIFT_OFF
    private var translating = true
    private var tier = PremiumEnglish.TIER_COURTLY
    private var options = KeyboardLayoutOptions()

    private val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val keyRows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val bubble: TextView
    private lateinit var sourceView: TextView
    private lateinit var tierChip: TextView
    private lateinit var powerChip: TextView
    private lateinit var revertChip: TextView
    private val letterKeys = ArrayList<Pair<TextView, Key>>()

    init {
        column.setBackgroundColor(color(R.color.keyboard_background))
        column.addView(buildStatusBar())
        column.addView(keyRows)
        addView(column, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        bubble = TextView(context).apply {
            gravity = Gravity.CENTER
            setTextColor(color(R.color.key_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            setBackgroundResource(R.drawable.key_preview_background)
            visibility = GONE
            elevation = dp(8).toFloat()
        }
        addView(bubble, LayoutParams(dp(48), dp(48)))

        renderLayer()
    }

    // ------------------------------------------------------------------ public

    /** Shows the plain modern text behind the ornate text in the field. */
    fun setSource(text: String) {
        val trimmed = text.trim()
        sourceView.text = if (trimmed.isEmpty()) idleHint() else "“$trimmed”"
        sourceView.alpha = if (trimmed.isEmpty()) 0.5f else 1f
        revertChip.visibility = if (trimmed.isEmpty() || !translating) GONE else VISIBLE
    }

    fun setTier(tier: Int) {
        this.tier = tier
        tierChip.text = Prefs.tierName(tier)
        setSource("")
    }

    fun setTranslating(on: Boolean) {
        translating = on
        powerChip.text = if (on) "◆" else "◇"
        powerChip.alpha = if (on) 1f else 0.4f
        tierChip.alpha = if (on) 1f else 0.4f
        setSource("")
    }

    /** Applies the size, number row and feedback settings, rebuilding the keys. */
    fun setLayoutOptions(options: KeyboardLayoutOptions) {
        val needsRebuild = options.sizeScale != this.options.sizeScale ||
            options.numberRow != this.options.numberRow
        this.options = options
        if (needsRebuild) renderLayer()
    }

    /** Turns on one-shot shift at the start of a sentence, as Gboard does. */
    fun setAutoShift(on: Boolean) {
        if (shift == SHIFT_LOCK) return
        val wanted = if (on) SHIFT_ONCE else SHIFT_OFF
        if (shift != wanted) {
            shift = wanted
            applyShiftToLabels()
        }
    }

    private fun idleHint(): String =
        if (translating) "Premium English · ${Prefs.tierName(tier)}" else "Translation suspended"

    // ------------------------------------------------------------------ status bar

    private fun buildStatusBar(): View {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(5), dp(8), dp(5))
            setBackgroundColor(color(R.color.status_background))
        }

        powerChip = chip("◆") { listener.onToggleTranslation() }
        bar.addView(powerChip)

        sourceView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(color(R.color.preview_text))
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.START
            setPadding(dp(8), 0, dp(8), 0)
        }
        bar.addView(sourceView)

        revertChip = chip("↺") { listener.onRevert() }
        revertChip.visibility = GONE
        bar.addView(revertChip)

        tierChip = chip(Prefs.tierName(tier)) { listener.onCycleTier() }
        bar.addView(tierChip)
        bar.addView(chip("🌐") { listener.onSwitchKeyboard() })
        bar.addView(chip("⚙") { listener.onOpenSettings() })
        return bar
    }

    private fun chip(text: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(color(R.color.accent))
            setPadding(dp(9), dp(4), dp(9), dp(4))
            setBackgroundResource(R.drawable.chip_background)
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(3) }
            setOnClickListener {
                feedback(it)
                onClick()
            }
        }

    // ------------------------------------------------------------------ layers

    private fun rowsFor(layer: Int): List<List<Key>> {
        val rows = ArrayList<List<Key>>()
        when (layer) {
            LAYER_SYMBOLS -> {
                rows.add(charRow("1234567890"))
                rows.add(charRow("@#\$%&-+()"))
                rows.add(
                    listOf(Key("=\\<", Kind.LAYER, weight = 1.5f, layer = LAYER_MORE)) +
                        charRow("*\"':;!?") +
                        listOf(Key("⌫", Kind.BACKSPACE, weight = 1.5f))
                )
                rows.add(bottomRow("ABC", LAYER_LETTERS))
            }
            LAYER_MORE -> {
                rows.add(charRow("~`|•√π÷×¶"))
                rows.add(charRow("£¢€¥^°={}"))
                rows.add(
                    listOf(Key("?123", Kind.LAYER, weight = 1.5f, layer = LAYER_SYMBOLS)) +
                        charRow("\\©®™[]") +
                        listOf(Key("⌫", Kind.BACKSPACE, weight = 1.5f))
                )
                rows.add(bottomRow("ABC", LAYER_LETTERS))
            }
            else -> {
                if (options.numberRow) rows.add(charRow("1234567890"))
                rows.add(charRow("qwertyuiop"))
                rows.add(charRow("asdfghjkl"))
                rows.add(
                    listOf(Key("⇧", Kind.SHIFT, weight = 1.5f)) +
                        charRow("zxcvbnm") +
                        listOf(Key("⌫", Kind.BACKSPACE, weight = 1.5f))
                )
                rows.add(bottomRow("?123", LAYER_SYMBOLS))
            }
        }
        return rows
    }

    private fun charRow(chars: String): List<Key> = chars.map {
        Key(it.toString(), Kind.CHAR, it.toString(), if (options.numberRow) null else SECONDARIES[it])
    }

    private fun bottomRow(layerLabel: String, target: Int): List<Key> = listOf(
        Key(layerLabel, Kind.LAYER, weight = 1.4f, layer = target),
        Key("☺", Kind.LAYER, weight = 1.1f, layer = LAYER_EMOJI),
        Key(",", Kind.CHAR, ",", "!"),
        Key("space", Kind.SPACE, " ", weight = 4.4f),
        Key(".", Kind.CHAR, ".", "?"),
        Key("↵", Kind.ENTER, weight = 1.4f)
    )

    private fun renderLayer() {
        keyRows.removeAllViews()
        letterKeys.clear()
        if (layer == LAYER_EMOJI) {
            keyRows.addView(buildEmojiLayer())
            return
        }
        for (row in rowsFor(layer)) {
            val rowView = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(3), dp(2), dp(3), dp(2))
            }
            for (key in row) rowView.addView(buildKey(key))
            keyRows.addView(rowView)
        }
        applyShiftToLabels()
    }

    private fun keyHeight(): Int = dp((BASE_KEY_HEIGHT_DP * options.sizeScale).toInt())

    private fun buildKey(key: Key): View {
        val view = TextView(context).apply {
            text = key.label
            gravity = Gravity.CENTER
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                (if (key.kind == Kind.CHAR) 19f else 15f) * (0.9f + 0.1f * options.sizeScale)
            )
            setTextColor(color(if (key.kind == Kind.ENTER) R.color.key_accent_text else R.color.key_text))
            setBackgroundResource(
                when (key.kind) {
                    Kind.CHAR -> R.drawable.key_background
                    Kind.ENTER -> R.drawable.key_background_accent
                    else -> R.drawable.key_background_modifier
                }
            )
            layoutParams = LinearLayout.LayoutParams(0, keyHeight(), key.weight).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
            isClickable = true
            // The secondary character, printed small in the corner.
            if (key.secondary != null) {
                text = key.label
                setPadding(0, dp(2), 0, 0)
            }
        }
        if (key.kind == Kind.CHAR && key.output.length == 1 && key.output[0].isLetter()) {
            letterKeys.add(view to key)
        }
        if (key.kind == Kind.BACKSPACE) attachRepeat(view) else attachPress(view, key)
        return view
    }

    private fun buildEmojiLayer(): View {
        val outer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val scroller = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, keyHeight() * 3
            )
        }
        val grid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        for (chunk in EMOJI.chunked(8)) {
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            for (emoji in chunk) {
                row.addView(TextView(context).apply {
                    text = emoji
                    gravity = Gravity.CENTER
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    setBackgroundResource(R.drawable.key_background)
                    layoutParams = LinearLayout.LayoutParams(0, keyHeight(), 1f).apply {
                        setMargins(dp(2), dp(2), dp(2), dp(2))
                    }
                    isClickable = true
                    setOnClickListener {
                        feedback(it)
                        listener.onText(emoji)
                    }
                })
            }
            grid.addView(row)
        }
        scroller.addView(grid)
        outer.addView(scroller)

        val footer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(3), dp(2), dp(3), dp(2))
        }
        footer.addView(buildKey(Key("ABC", Kind.LAYER, weight = 2f, layer = LAYER_LETTERS)))
        footer.addView(buildKey(Key("space", Kind.SPACE, " ", weight = 4f)))
        footer.addView(buildKey(Key("⌫", Kind.BACKSPACE, weight = 1.5f)))
        outer.addView(footer)
        return outer
    }

    // ------------------------------------------------------------------ touch

    /**
     * Press, long-press and the little bubble above the key, all from one touch
     * listener so that a long press never also types the primary character.
     */
    private fun attachPress(view: View, key: Key) {
        var longPressed = false
        val longPress = Runnable {
            longPressed = true
            key.secondary?.let {
                listener.onText(it)
                showBubble(view, it)
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    longPressed = false
                    v.isPressed = true
                    feedback(v)
                    if (key.kind == Kind.CHAR) showBubble(v, displayLabel(key))
                    if (key.secondary != null) handler.postDelayed(longPress, LONG_PRESS_MS)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    handler.removeCallbacks(longPress)
                    hideBubble()
                    if (!longPressed && inside(v, event)) press(key)
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    handler.removeCallbacks(longPress)
                    hideBubble()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!inside(v, event)) {
                        v.isPressed = false
                        handler.removeCallbacks(longPress)
                        hideBubble()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun inside(v: View, event: MotionEvent): Boolean {
        val slop = dp(12)
        return event.x >= -slop && event.y >= -slop &&
            event.x <= v.width + slop && event.y <= v.height + slop
    }

    private fun press(key: Key) {
        when (key.kind) {
            Kind.CHAR -> {
                val text = displayLabel(key)
                if (text.length == 1) listener.onChar(text[0]) else listener.onText(text)
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

    private fun displayLabel(key: Key): String {
        val out = key.output
        return if (shift != SHIFT_OFF && out.length == 1 && out[0].isLetter()) out.uppercase() else out
    }

    /** Hold to delete: one press, then a steady stream. */
    private fun attachRepeat(view: View) {
        val repeat = object : Runnable {
            override fun run() {
                listener.onBackspace()
                handler.postDelayed(this, REPEAT_RATE_MS)
            }
        }
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    feedback(v)
                    listener.onBackspace()
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

    // ------------------------------------------------------------------ bubble

    /**
     * The key preview lives inside this view rather than in a PopupWindow, so
     * it cannot outlive the keyboard or fail for want of a window token. For
     * the top row it is drawn over the status bar instead of above the panel.
     */
    private fun showBubble(key: View, text: String) {
        if (!options.keyPreview) return
        val keyLocation = IntArray(2)
        val selfLocation = IntArray(2)
        key.getLocationInWindow(keyLocation)
        getLocationInWindow(selfLocation)
        val x = keyLocation[0] - selfLocation[0]
        val y = keyLocation[1] - selfLocation[1]

        bubble.text = text
        val params = bubble.layoutParams as LayoutParams
        params.width = key.width
        params.height = key.height
        params.leftMargin = x
        params.topMargin = (y - key.height - dp(4)).coerceAtLeast(0)
        bubble.layoutParams = params
        bubble.visibility = VISIBLE
    }

    private fun hideBubble() {
        bubble.visibility = GONE
    }

    private fun feedback(view: View) {
        if (options.vibrate) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun applyShiftToLabels() {
        val upper = shift != SHIFT_OFF
        for ((view, key) in letterKeys) {
            view.text = if (upper) key.output.uppercase() else key.output
        }
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    // ------------------------------------------------------------------ helpers

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    @Suppress("DEPRECATION")
    private fun color(id: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) resources.getColor(id, null)
        else resources.getColor(id)
}
