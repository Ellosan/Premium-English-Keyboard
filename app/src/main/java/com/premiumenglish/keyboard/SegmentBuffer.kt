package com.premiumenglish.keyboard

/** Whatever is receiving the typing. The keyboard talks to the field through this. */
interface TextTarget {
    /** Deletes [before] characters in front of the cursor, then inserts [text]. */
    fun replace(before: Int, text: String)

    /** Up to [count] characters immediately before the cursor. */
    fun textBeforeCursor(count: Int): CharSequence

    /** Sends a plain delete, for when the keyboard has nothing of its own to remove. */
    fun sendBackspace()
}

/** What the buffer needs to know at the moment a key is pressed. */
data class SegmentSettings(
    val options: PremiumOptions = PremiumOptions(),
    val translate: Boolean = true,
    val doubleSpacePeriod: Boolean = true
)

/**
 * Holds the sentence being typed, in plain modern English, and keeps the text
 * field showing the translated version of it.
 *
 * After every keystroke the whole segment is translated and written back over
 * itself. Keeping the typed original is what makes backspace rewind your own
 * words rather than the ornate ones, and what lets a later word change an
 * earlier one — "i" is nothing on its own, but "i think" is "methinks".
 *
 * This is deliberately separate from the input method service: it holds all of
 * the fiddly bookkeeping and none of the Android plumbing, so it can be tested.
 */
class SegmentBuffer(
    private val target: TextTarget,
    private val settings: () -> SegmentSettings
) {

    private companion object {
        /** Past this many characters the segment is cut loose, so that a long
         *  message does not mean rewriting a paragraph on every keystroke. */
        const val MAX_SEGMENT = 240
    }

    private val pending = StringBuilder()

    /** How many characters of the field currently belong to [pending]. */
    private var shown = 0

    /** The plain modern text behind what the field is showing. */
    val source: String get() = pending.toString()

    val isEmpty: Boolean get() = pending.isEmpty()

    // ------------------------------------------------------------------ input

    fun type(c: Char) {
        val current = settings()
        if (c == ' ' && current.doubleSpacePeriod && applyDoubleSpacePeriod(current)) return
        if (!current.translate) {
            target.replace(0, c.toString())
            return
        }
        pending.append(c)
        render(current)
        if (c == '.' || c == '!' || c == '?' || pending.length >= MAX_SEGMENT) close()
    }

    /** Emoji, and the symbols reached by holding a key. */
    fun type(text: String) {
        val current = settings()
        if (!current.translate) {
            target.replace(0, text)
            return
        }
        pending.append(text)
        render(current)
        if (pending.length >= MAX_SEGMENT) close()
    }

    fun backspace() {
        if (pending.isEmpty()) {
            target.sendBackspace()
            return
        }
        pending.setLength(pending.length - 1)
        if (pending.isEmpty()) {
            target.replace(shown, "")
            shown = 0
        } else {
            render(settings())
        }
    }

    /** Puts back exactly what was typed, for when the ceremony is unwelcome. */
    fun revert() {
        if (pending.isEmpty()) return
        write(pending.toString())
        close()
    }

    /** Redraws the segment, for when the tier changes mid-sentence. */
    fun retranslate() {
        if (pending.isEmpty()) return
        render(settings())
    }

    /** Finishes the current thought: the text stays, the buffer starts over. */
    fun close() {
        pending.setLength(0)
        shown = 0
    }

    // ------------------------------------------------------------------ queries

    /** True at the very start of a field, or just after a finished sentence. */
    fun atSentenceStart(): Boolean =
        if (pending.isNotEmpty()) endsSentence(pending) else endsSentence(target.textBeforeCursor(4))

    // ------------------------------------------------------------------ internals

    /**
     * Two spaces after a word become a full stop, as on Gboard.
     *
     * @return true if the space was consumed.
     */
    private fun applyDoubleSpacePeriod(current: SegmentSettings): Boolean {
        if (current.translate) {
            if (pending.length < 2) return false
            if (pending[pending.length - 1] != ' ') return false
            if (!pending[pending.length - 2].isLetterOrDigit()) return false
            pending.setLength(pending.length - 1)
            pending.append(". ")
            render(current)
            close()
            return true
        }
        val before = target.textBeforeCursor(2)
        if (before.length < 2 || before[1] != ' ' || !before[0].isLetterOrDigit()) return false
        target.replace(1, ". ")
        return true
    }

    private fun render(current: SegmentSettings) {
        write(PremiumEnglish.translateLive(pending.toString(), current.options))
    }

    private fun write(text: String) {
        target.replace(shown, text)
        shown = text.length
    }

    private fun endsSentence(text: CharSequence): Boolean {
        var i = text.length - 1
        var sawGap = false
        while (i >= 0 && (text[i] == ' ' || text[i] == '\n')) {
            sawGap = true
            i--
        }
        if (i < 0) return true
        if (!sawGap) return false
        val c = text[i]
        return c == '.' || c == '!' || c == '?'
    }
}
