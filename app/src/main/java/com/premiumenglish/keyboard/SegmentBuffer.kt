package com.premiumenglish.keyboard

/** Whatever is receiving the typing. The keyboard talks to the field through this. */
interface TextTarget {
    /**
     * Deletes [before] characters in front of the cursor and [after] characters
     * behind it, then inserts [text] at the cursor.
     */
    fun replace(before: Int, after: Int, text: String)

    /** Up to [count] characters immediately before the cursor. */
    fun textBeforeCursor(count: Int): CharSequence

    /** Up to [count] characters immediately after the cursor. */
    fun textAfterCursor(count: Int): CharSequence

    /** The highlighted text, or null when nothing is selected. */
    fun selectedText(): CharSequence?

    /** Sends a plain delete, for when the keyboard has nothing of its own to remove. */
    fun sendBackspace()
}

/** What the buffer needs to know at the moment a key is pressed. */
data class SegmentSettings(
    val options: PremiumOptions = PremiumOptions(),
    val translate: Boolean = true,
    /** Translate on every keystroke, rather than when the Translate key is pressed. */
    val live: Boolean = false,
    val doubleSpacePeriod: Boolean = true
)

/**
 * Holds what has been typed, in plain modern English, and turns it into Premium
 * English either as you type or when you ask.
 *
 * In its usual mode the text goes in as you typed it and stays that way until
 * the Translate key is pressed, at which point the whole message is replaced
 * at once. Live mode translates after every keystroke instead, which is a good
 * deal more startling to type on.
 *
 * Either way the typed original is kept, which is what makes backspace rewind
 * your own words rather than the ornate ones, lets a later word change an
 * earlier one — "i" is nothing on its own, but "i think" is "methinks" — and
 * allows a translation to be taken back afterwards.
 *
 * Deliberately separate from the input method service: all of the fiddly
 * bookkeeping, none of the Android plumbing, so it can be tested.
 */
class SegmentBuffer(
    private val target: TextTarget,
    private val settings: () -> SegmentSettings
) {

    private companion object {
        /** In live mode the segment is rewritten on every keystroke, so it is
         *  cut loose at a length that keeps that cheap. */
        const val MAX_LIVE = 240

        /** Waiting for the Translate key costs nothing per keystroke, so the
         *  buffer can hold a whole message. */
        const val MAX_TYPED = 4000
    }

    private val pending = StringBuilder()

    /** How many characters of the field currently belong to [pending]. */
    private var shown = 0

    /** What was typed before the last translation, so that it can be put back. */
    private var undoSource: String? = null
    private var undoLength = 0

    /** The plain modern text behind what the field is showing. */
    val source: String get() = pending.toString()

    val isEmpty: Boolean get() = pending.isEmpty()

    /** True when there is either typing to undo or a translation to take back. */
    val canRevert: Boolean get() = pending.isNotEmpty() || undoSource != null

    // ------------------------------------------------------------------ input

    fun type(c: Char) {
        val current = settings()
        if (c == ' ' && current.doubleSpacePeriod && applyDoubleSpacePeriod(current)) return
        if (!current.translate) {
            target.replace(0, 0, c.toString())
            return
        }
        pending.append(c)
        if (current.live) {
            render(current)
            if (c == '.' || c == '!' || c == '?' || pending.length >= MAX_LIVE) close()
        } else {
            echo(c.toString())
        }
    }

    /** Emoji, and the symbols reached by holding a key. */
    fun type(text: String) {
        val current = settings()
        if (!current.translate) {
            target.replace(0, 0, text)
            return
        }
        pending.append(text)
        if (current.live) {
            render(current)
            if (pending.length >= MAX_LIVE) close()
        } else {
            echo(text)
        }
    }

    fun backspace() {
        if (pending.isEmpty()) {
            target.sendBackspace()
            return
        }
        pending.setLength(pending.length - 1)
        val current = settings()
        if (current.live) {
            if (pending.isEmpty()) {
                target.replace(shown, 0, "")
                shown = 0
            } else {
                render(current)
            }
        } else {
            target.replace(1, 0, "")
            shown = (shown - 1).coerceAtLeast(0)
        }
    }

    // ------------------------------------------------------------------ translating

    /**
     * Translates the message: what has been typed since the last translation,
     * or the whole field if the keyboard has not been keeping track.
     *
     * @return false when there was nothing to translate.
     */
    fun translateNow(): Boolean {
        val current = settings()
        if (!current.translate) return false

        // A selection is what the user means, if there is one. It also has to
        // be handled separately: deleting "around the cursor" deletes around a
        // selection, which would throw the selected text away.
        val selected = target.selectedText()
        if (!selected.isNullOrBlank()) {
            val raw = selected.toString()
            val translated = PremiumEnglish.translate(raw, current.options, finished = true)
            // Committing with a selection live replaces it.
            target.replace(0, 0, translated)
            close()
            remember(raw, translated.length)
            return true
        }

        if (pending.isNotEmpty()) {
            val raw = pending.toString()
            val translated = PremiumEnglish.translate(raw, current.options, finished = true)
            target.replace(shown, 0, translated)
            remember(raw, translated.length)
            close()
            return true
        }

        // Already translated, and nothing typed since: pressing the key again
        // must not put the output through a second time.
        if (undoSource != null) return false

        // Nothing of ours in the field — translate what is already in it.
        val before = target.textBeforeCursor(MAX_TYPED).toString()
        val after = target.textAfterCursor(MAX_TYPED).toString()
        val whole = before + after
        if (whole.isBlank()) return false
        val translated = PremiumEnglish.translate(whole, current.options, finished = true)
        target.replace(before.length, after.length, translated)
        remember(whole, translated.length)
        return true
    }

    /**
     * Takes back the last translation, or in live mode puts back exactly what
     * was typed.
     */
    fun revert(): Boolean {
        val raw = undoSource
        if (raw != null) {
            target.replace(undoLength, 0, raw)
            forgetUndo()
            // Leave it ready to be translated again.
            pending.append(raw)
            shown = raw.length
            return true
        }
        if (pending.isEmpty()) return false
        write(pending.toString())
        close()
        return true
    }

    /** Redraws the segment, for when the tier changes mid-sentence. */
    fun retranslate() {
        val current = settings()
        if (pending.isEmpty() || !current.live) return
        render(current)
    }

    // ------------------------------------------------------------------ state

    /** Finishes the current thought: the text stays, the buffer starts over. */
    fun close() {
        pending.setLength(0)
        shown = 0
    }

    /** Lets go of everything, including the chance to undo. */
    fun forget() {
        close()
        forgetUndo()
    }

    /**
     * Exactly what [translateNow] would produce, for the status bar to show
     * while you type. It has to be the same translation and not a cheaper
     * approximation, or the bar would be promising something the key does not
     * deliver.
     */
    fun preview(): String {
        val current = settings()
        if (!current.translate || pending.isEmpty()) return ""
        return PremiumEnglish.translate(pending.toString(), current.options, finished = true)
    }

    /** True at the very start of a field, or just after a finished sentence. */
    fun atSentenceStart(): Boolean =
        if (pending.isNotEmpty()) endsSentence(pending) else endsSentence(target.textBeforeCursor(4))

    // ------------------------------------------------------------------ internals

    private fun echo(text: String) {
        target.replace(0, 0, text)
        shown += text.length
        if (pending.length >= MAX_TYPED) close()
    }

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
            if (current.live) {
                render(current)
                close()
            } else {
                target.replace(1, 0, ". ")
                shown += 1
            }
            return true
        }
        val before = target.textBeforeCursor(2)
        if (before.length < 2 || before[1] != ' ' || !before[0].isLetterOrDigit()) return false
        target.replace(1, 0, ". ")
        return true
    }

    private fun render(current: SegmentSettings) {
        write(PremiumEnglish.translateLive(pending.toString(), current.options))
    }

    private fun write(text: String) {
        target.replace(shown, 0, text)
        shown = text.length
    }

    private fun remember(raw: String, length: Int) {
        undoSource = raw
        undoLength = length
    }

    private fun forgetUndo() {
        undoSource = null
        undoLength = 0
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
