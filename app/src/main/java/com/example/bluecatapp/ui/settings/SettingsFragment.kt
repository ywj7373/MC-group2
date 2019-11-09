package com.example.bluecatapp.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.*
import com.example.bluecatapp.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val sharedPref = preferenceManager.sharedPreferences

        val profilePreference = findPreference<EditTextPreference>(getString(R.string.profile))
        profilePreference?.summary = "Display Name"

        //set default pedometer preference to "true"
        PreferenceManager.setDefaultValues(this.context, R.xml.root_preferences, true);
        val pedometerPreference = preferenceManager.findPreference<SwitchPreference>(getString(R.string.pedometer))
        pedometerPreference?.setSwitchTextOn()
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (preference is RestrictAppsPreference) {
            val dialogFragment: DialogFragment =
                RestrictAppsPreferenceFragmentCompat.newInstance(preference.key)
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(fragmentManager!!, null)
        } else super.onDisplayPreferenceDialog(preference)
    }
}