package com.premiumenglish.keyboard

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/**
 * Setup and preferences.
 *
 * The keyboard at the bottom is the real [KeyboardPanel], wired to the sample
 * box, so the size setting can be judged by using it rather than by imagining
 * it. The translation shown underneath is the same engine the keyboard runs.
 */
class SettingsActivity : Activity(), KeyboardPanel.Listener {

    private lateinit var prefs: Prefs
    private lateinit var sample: EditText
    private lateinit var sampleOutput: TextView
    private lateinit var sizeLabel: TextView
    private var preview: KeyboardPanel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        setContentView(R.layout.activity_settings)

        findViewById<Button>(R.id.enable_keyboard).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        findViewById<Button>(R.id.choose_keyboard).setOnClickListener {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }

        setUpTiers()
        setUpSwitches()
        setUpSize()
        setUpSample()
    }

    // ------------------------------------------------------------------ sections

    private fun setUpTiers() {
        val tiers = findViewById<RadioGroup>(R.id.tier_group)
        tiers.check(
            when (prefs.tier) {
                PremiumEnglish.TIER_REFINED -> R.id.tier_refined
                PremiumEnglish.TIER_SOVEREIGN -> R.id.tier_sovereign
                else -> R.id.tier_courtly
            }
        )
        tiers.setOnCheckedChangeListener { _, checkedId ->
            prefs.tier = when (checkedId) {
                R.id.tier_refined -> PremiumEnglish.TIER_REFINED
                R.id.tier_sovereign -> PremiumEnglish.TIER_SOVEREIGN
                else -> PremiumEnglish.TIER_COURTLY
            }
            preview?.setTier(prefs.tier)
            refreshSample()
        }
    }

    private fun setUpSwitches() {
        bindSwitch(R.id.switch_auto, prefs.autoTranslate) { prefs.autoTranslate = it }
        bindSwitch(R.id.switch_flourish, prefs.flourish) { prefs.flourish = it }
        bindSwitch(R.id.switch_olde, prefs.oldeSpelling) { prefs.oldeSpelling = it }
        bindSwitch(R.id.switch_number_row, prefs.numberRow) {
            prefs.numberRow = it
            preview?.setLayoutOptions(prefs.layout())
        }
        bindSwitch(R.id.switch_vibrate, prefs.vibrate) {
            prefs.vibrate = it
            preview?.setLayoutOptions(prefs.layout())
        }
        bindSwitch(R.id.switch_key_preview, prefs.keyPreview) {
            prefs.keyPreview = it
            preview?.setLayoutOptions(prefs.layout())
        }
        bindSwitch(R.id.switch_auto_caps, prefs.autoCapitalize) { prefs.autoCapitalize = it }
        bindSwitch(R.id.switch_double_space, prefs.doubleSpacePeriod) { prefs.doubleSpacePeriod = it }
    }

    private fun setUpSize() {
        sizeLabel = findViewById(R.id.size_label)
        val bar = findViewById<SeekBar>(R.id.size_bar)
        bar.max = Prefs.SIZE_SCALES.size - 1
        bar.progress = prefs.sizeStep
        showSizeLabel()
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                prefs.sizeStep = progress
                showSizeLabel()
                preview?.setLayoutOptions(prefs.layout())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })
    }

    private fun showSizeLabel() {
        sizeLabel.text = getString(R.string.size_value, Prefs.SIZE_NAMES[prefs.sizeStep])
    }

    private fun setUpSample() {
        sample = findViewById(R.id.sample_input)
        sampleOutput = findViewById(R.id.sample_output)

        // The preview keyboard below does the typing, so keep the system one away.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            sample.showSoftInputOnFocus = false
        }
        sample.setOnClickListener { sample.requestFocus() }

        sample.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = refreshSample()
        })

        val holder = findViewById<FrameLayout>(R.id.preview_keyboard)
        val panel = KeyboardPanel(this, this)
        panel.setLayoutOptions(prefs.layout())
        panel.setTier(prefs.tier)
        panel.setTranslating(prefs.autoTranslate)
        holder.addView(panel)
        preview = panel

        refreshSample()
    }

    private fun bindSwitch(id: Int, initial: Boolean, onChange: (Boolean) -> Unit) {
        val view = findViewById<Switch>(id)
        view.isChecked = initial
        view.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            onChange(checked)
            refreshSample()
        }
    }

    private fun refreshSample() {
        val raw = sample.text?.toString().orEmpty()
        val text = if (raw.isBlank()) getString(R.string.sample_default) else raw
        sampleOutput.text = PremiumEnglish.translate(text, prefs.options(), finished = true)
        preview?.setSource(if (raw.isBlank()) "" else raw)
    }

    // ------------------------------------------------------------------ preview keyboard

    private fun insert(text: String) {
        val start = sample.selectionStart.coerceAtLeast(0)
        val end = sample.selectionEnd.coerceAtLeast(0)
        sample.text.replace(minOf(start, end), maxOf(start, end), text)
    }

    override fun onChar(c: Char) = insert(c.toString())

    override fun onText(text: String) = insert(text)

    override fun onBackspace() {
        val start = sample.selectionStart
        val end = sample.selectionEnd
        if (start != end) {
            sample.text.delete(minOf(start, end), maxOf(start, end))
        } else if (start > 0) {
            sample.text.delete(start - 1, start)
        }
    }

    override fun onEnter() = insert("\n")

    override fun onCycleTier() {
        prefs.tier = if (prefs.tier >= PremiumEnglish.TIER_SOVEREIGN) {
            PremiumEnglish.TIER_REFINED
        } else {
            prefs.tier + 1
        }
        findViewById<RadioGroup>(R.id.tier_group).check(
            when (prefs.tier) {
                PremiumEnglish.TIER_REFINED -> R.id.tier_refined
                PremiumEnglish.TIER_SOVEREIGN -> R.id.tier_sovereign
                else -> R.id.tier_courtly
            }
        )
        preview?.setTier(prefs.tier)
        refreshSample()
    }

    override fun onToggleTranslation() {
        prefs.autoTranslate = !prefs.autoTranslate
        findViewById<Switch>(R.id.switch_auto).isChecked = prefs.autoTranslate
        preview?.setTranslating(prefs.autoTranslate)
    }

    override fun onOpenSettings() = Unit

    override fun onSwitchKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
    }

    override fun onRevert() {
        sample.setText("")
    }
}
