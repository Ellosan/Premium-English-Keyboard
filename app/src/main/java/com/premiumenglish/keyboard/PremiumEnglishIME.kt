package com.premiumenglish.keyboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo

/**
 * The keyboard service.
 *
 * Everything typed since the last full stop is held in [pending] in plain
 * modern English. After each keystroke that segment is translated and written
 * back over itself, so the field always shows Premium English while the buffer
 * remembers what was actually typed. Keeping the original around is what makes
 * backspace, and translations that depend on later words, work at all: "i" is
 * nothing on its own, "i think" is "methinks".
 */
class PremiumEnglishIME : InputMethodService(), KeyboardPanel.Listener {

    private companion object {
        /** Past this many characters the segment is cut loose, so that a long
         *  message does not mean rewriting a paragraph on every keystroke. */
        const val MAX_SEGMENT = 240
    }

    private lateinit var prefs: Prefs
    private var panel: KeyboardPanel? = null

    private val pending = StringBuilder()

    /** How many characters of the field currently belong to [pending]. */
    private var shownLength = 0

    private var cursor = 0
    private var expectedCursor = -1

    /** False in password, email and URL fields, where nobody wants ceremony. */
    private var fieldAllowsTranslation = true

    // ------------------------------------------------------------------ lifecycle

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
    }

    override fun onCreateInputView(): View {
        val view = KeyboardPanel(this, this)
        panel = view
        return view
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        resetSegment()
        fieldAllowsTranslation = info == null || isTranslatableField(info)
        cursor = info?.initialSelEnd ?: 0
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        panel?.apply {
            setTier(prefs.tier)
            setTranslating(translationActive())
            setPreview("")
        }
    }

    override fun onFinishInput() {
        super.onFinishInput()
        resetSegment()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        // The caret moved somewhere we did not put it: another app, an
        // autocomplete, or the user tapping elsewhere. Our buffer no longer
        // describes the text in front of the cursor, so let it go.
        if (expectedCursor >= 0 && newSelEnd != expectedCursor) resetSegment()
        cursor = newSelEnd
        expectedCursor = -1
    }

    private fun isTranslatableField(info: EditorInfo): Boolean {
        val klass = info.inputType and InputType.TYPE_MASK_CLASS
        if (klass != InputType.TYPE_CLASS_TEXT) return false
        return when (info.inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_FILTER -> false
            else -> true
        }
    }

    private fun translationActive(): Boolean = prefs.autoTranslate && fieldAllowsTranslation

    // ------------------------------------------------------------------ keys

    override fun onChar(c: Char) {
        if (!translationActive()) {
            commitPlain(c.toString())
            return
        }
        pending.append(c)
        renderSegment()
        if (c == '.' || c == '!' || c == '?' || pending.length >= MAX_SEGMENT) closeSegment()
    }

    override fun onBackspace() {
        if (pending.isEmpty()) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            return
        }
        pending.setLength(pending.length - 1)
        if (pending.isEmpty()) {
            writeSegment("")
            resetSegment()
        } else {
            renderSegment()
        }
    }

    override fun onEnter() {
        closeSegment()
        val editor = currentInputEditorInfo
        val action = editor?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val suppressed = (editor?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED && !suppressed) {
            currentInputConnection?.performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }

    // ------------------------------------------------------------------ status bar

    override fun onCycleTier() {
        prefs.tier = if (prefs.tier >= PremiumEnglish.TIER_SOVEREIGN) {
            PremiumEnglish.TIER_REFINED
        } else {
            prefs.tier + 1
        }
        panel?.setTier(prefs.tier)
        // Re-translate what is already on screen at the new tier.
        if (pending.isNotEmpty()) renderSegment()
    }

    override fun onToggleTranslation() {
        prefs.autoTranslate = !prefs.autoTranslate
        if (!prefs.autoTranslate) {
            // Leave the premium text standing, but stop tracking it.
            resetSegment()
        }
        panel?.setTranslating(translationActive())
    }

    override fun onOpenSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    // ------------------------------------------------------------------ the segment

    private fun renderSegment() {
        val translated = PremiumEnglish.translateLive(pending.toString(), prefs.options())
        writeSegment(translated)
        panel?.setPreview(translated.trim())
    }

    /** Swaps the on-screen segment for [text] in a single edit. */
    private fun writeSegment(text: String) {
        val ic = currentInputConnection ?: return
        ic.beginBatchEdit()
        if (shownLength > 0) ic.deleteSurroundingText(shownLength, 0)
        if (text.isNotEmpty()) ic.commitText(text, 1)
        ic.endBatchEdit()
        expectedCursor = cursor - shownLength + text.length
        cursor = expectedCursor
        shownLength = text.length
    }

    private fun commitPlain(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        cursor += text.length
        expectedCursor = cursor
    }

    /** Finishes the current thought: the text stays, the buffer starts over. */
    private fun closeSegment() {
        pending.setLength(0)
        shownLength = 0
        panel?.setPreview("")
    }

    private fun resetSegment() {
        pending.setLength(0)
        shownLength = 0
        expectedCursor = -1
        panel?.setPreview("")
    }
}
