package com.premiumenglish.keyboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager

/**
 * The keyboard service: the Android half of the keyboard.
 *
 * The typing itself lives in [SegmentBuffer]. This class connects that buffer
 * to the text field, keeps track of where the cursor is, and drives the keys.
 */
class PremiumEnglishIME : InputMethodService(), KeyboardPanel.Listener {

    private lateinit var prefs: Prefs
    private var panel: KeyboardPanel? = null

    private var cursor = 0
    private var expectedCursor = -1

    /** False in password, email and URL fields, where nobody wants ceremony. */
    private var fieldAllowsTranslation = true

    /** Writes the buffer's edits into the field, keeping the cursor in step. */
    private val target = object : TextTarget {
        override fun replace(before: Int, text: String) {
            val ic = currentInputConnection ?: return
            ic.beginBatchEdit()
            if (before > 0) ic.deleteSurroundingText(before, 0)
            if (text.isNotEmpty()) ic.commitText(text, 1)
            ic.endBatchEdit()
            cursor = cursor - before + text.length
            expectedCursor = cursor
        }

        override fun textBeforeCursor(count: Int): CharSequence =
            currentInputConnection?.getTextBeforeCursor(count, 0) ?: ""

        override fun sendBackspace() {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        }
    }

    private val buffer = SegmentBuffer(target) {
        SegmentSettings(prefs.options(), translationActive(), prefs.doubleSpacePeriod)
    }

    // ------------------------------------------------------------------ lifecycle

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
    }

    override fun onCreateInputView(): View {
        val view = KeyboardPanel(this, this)
        view.setLayoutOptions(prefs.layout())
        panel = view
        return view
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        resetSegment()
        fieldAllowsTranslation = info == null || isTranslatableField(info)
        cursor = info?.initialSelEnd?.coerceAtLeast(0) ?: 0
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Settings may have changed while the keyboard was away.
        panel?.apply {
            setLayoutOptions(prefs.layout())
            setTier(prefs.tier)
            setTranslating(translationActive())
            setSource("")
        }
        updateAutoShift()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        resetSegment()
    }

    /** Never take over the whole screen in landscape; the field stays visible. */
    override fun onEvaluateFullscreenMode(): Boolean = false

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
        updateAutoShift()
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
        buffer.type(c)
        afterInput()
    }

    override fun onText(text: String) {
        buffer.type(text)
        afterInput()
    }

    override fun onBackspace() {
        buffer.backspace()
        afterInput()
    }

    override fun onEnter() {
        buffer.close()
        val editor = currentInputEditorInfo
        val action = editor?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val suppressed = (editor?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED && !suppressed) {
            currentInputConnection?.performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
        afterInput()
    }

    private fun afterInput() {
        panel?.setSource(buffer.source)
        updateAutoShift()
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
        if (!buffer.isEmpty) {
            buffer.retranslate()
            panel?.setSource(buffer.source)
        }
    }

    override fun onToggleTranslation() {
        prefs.autoTranslate = !prefs.autoTranslate
        // Leave the premium text standing, but stop tracking it.
        if (!prefs.autoTranslate) resetSegment()
        panel?.setTranslating(translationActive())
    }

    override fun onRevert() {
        buffer.revert()
        panel?.setSource("")
    }

    override fun onOpenSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    override fun onSwitchKeyboard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && switchToNextInputMethod(false)) return
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
    }

    // ------------------------------------------------------------------ helpers

    private fun resetSegment() {
        buffer.close()
        expectedCursor = -1
        panel?.setSource("")
    }

    private fun updateAutoShift() {
        panel?.setAutoShift(prefs.autoCapitalize && buffer.atSentenceStart())
    }
}
