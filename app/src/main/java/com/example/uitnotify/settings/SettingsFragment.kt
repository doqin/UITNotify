package com.example.uitnotify.settings

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.example.uitnotify.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        val intervalPreference: EditTextPreference? = findPreference("interval_preference")
        intervalPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
    }
}