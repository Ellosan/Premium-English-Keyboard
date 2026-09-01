package com.premiumenglish.keyboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.view.inputmethod.InputMethodManager

/**
 * Setup and preferences, and a place to try the translation out without having
 * to switch keyboards first.
 */
class SettingsActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var sample: EditText
    private lateinit var sampleOutput: TextView

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
            refreshSample()
        }

        bindSwitch(R.id.switch_auto, prefs.autoTranslate) { prefs.autoTranslate = it }
        bindSwitch(R.id.switch_flourish, prefs.flourish) { prefs.flourish = it }
        bindSwitch(R.id.switch_olde, prefs.oldeSpelling) { prefs.oldeSpelling = it }

        sample = findViewById(R.id.sample_input)
        sampleOutput = findViewById(R.id.sample_output)
        sample.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = refreshSample()
        })
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
    }
}
