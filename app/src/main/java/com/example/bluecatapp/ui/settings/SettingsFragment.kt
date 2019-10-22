package com.example.bluecatapp.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.Preference
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.example.bluecatapp.MainActivity
import com.example.bluecatapp.R

@Suppress("DEPRECATION")
class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var settingsViewModel: SettingsViewModel

//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        settingsViewModel =
//            ViewModelProviders.of(this).get(SettingsViewModel::class.java)
//        val root = inflater.inflate(com.example.bluecatapp.R.layout.fragment_settings, container, false)
//        val textView: TextView = root.findViewById(com.example.bluecatapp.R.id.settings)
//        settingsViewModel.text.observe(this, Observer {
//            textView.text = it
//        })
//        return root
//    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val sharedPref = preferenceManager.sharedPreferences

        val profilePreference = findPreference<EditTextPreference>(getString(R.string.profile))
        profilePreference?.summary = "Display Name"
    }

}