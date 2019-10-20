package com.example.bluecatapp.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceFragmentCompat
import com.example.bluecatapp.R
import com.example.bluecatapp.SettingsActivity

class SettingsFragment : PreferenceFragmentCompat() {

//    private lateinit var settingsViewModel: SettingsViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        startActivity(Intent(activity!!, SettingsActivity::class.java))
    }

//    override fun onCreateView(
////        inflater: LayoutInflater,
////        container: ViewGroup?,
////        savedInstanceState: Bundle?
////    ): View? {
////        settingsViewModel =
////            ViewModelProviders.of(this).get(SettingsViewModel::class.java)
////        val root = inflater.inflate(com.example.bluecatapp.R.layout.fragment_settings, container, false)
////        val textView: TextView = root.findViewById(com.example.bluecatapp.R.id.text_settings)
////        settingsViewModel.text.observe(this, Observer {
////            textView.text = it
////        })
////        return root
////    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}