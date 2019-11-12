package com.example.bluecatapp.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.*
import com.example.bluecatapp.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val profilePreference = findPreference<EditTextPreference>(getString(R.string.profile))
        profilePreference?.summary = "Display Name"

        //set default pedometer preference to "true"
        val pedometerPreference = preferenceManager.findPreference<SwitchPreference>(getString(R.string.pedometer))
        pedometerPreference?.setChecked(true)

        //set default app blocking preference to "true"
        val appBlockPreference = preferenceManager.findPreference<SwitchPreference>(getString(R.string.appblock))
        appBlockPreference?.setChecked(true)
        //TODO: replace START and STOP buttons in app blocking screen
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