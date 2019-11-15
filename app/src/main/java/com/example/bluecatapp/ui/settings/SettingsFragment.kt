package com.example.bluecatapp.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.preference.*
import com.example.bluecatapp.AppBlockForegroundService
import com.example.bluecatapp.R

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var preference: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preference = PreferenceManager.getDefaultSharedPreferences(requireContext())
        editor = preference.edit()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val profilePreference = findPreference<EditTextPreference>(getString(R.string.profile))
        profilePreference?.summary = "Display Name"

        val appBlockPreference = preferenceManager.findPreference<SwitchPreference>(getString(R.string.appblock))
        //app block monitoring starts automatically when toggled on
        appBlockPreference?.setOnPreferenceChangeListener( object : Preference.OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
                if(!appBlockPreference.isChecked){
                    //toggle on: app blocking enabled
                    AppBlockForegroundService.startService(context!!, "Monitoring.. ")

                    Toast.makeText(
                        activity!!.applicationContext,
                        "App blocking enabled",
                        Toast.LENGTH_SHORT
                    ).show()
                } else{
                    //toggle off: app blocking disabled
                    AppBlockForegroundService.stopService(context!!)
                    Toast.makeText(
                        activity!!.applicationContext,
                        "App blocking disabled",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return true
            }
        })

        val hwModeTimePreference= preferenceManager.findPreference<ListPreference>(getString(R.string.hw_time_key))
        hwModeTimePreference?.setOnPreferenceChangeListener( object : Preference.OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {

                editor.putString(getString(R.string.hw_time_value),newValue.toString())
                editor.commit()

                Toast.makeText(
                    activity!!.applicationContext,
                    "HW mode time changed to ${newValue.toString().toInt()/1000/60} minutes " +
//                            "${getString(R.string.hw_time_value).toInt()/1000/60}" +
                            "",
                    Toast.LENGTH_SHORT
                ).show()

                return true
            }
        })

        val hwModePedometerBoolPreference = preferenceManager.findPreference<SwitchPreference>(getString(R.string.hw_pedometer_bool_key))
        hwModePedometerBoolPreference?.setOnPreferenceChangeListener( object : Preference.OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {

                editor.putBoolean(R.bool.hw_pedometer_bool.toString(),hwModePedometerBoolPreference.isChecked)
                editor.commit()

                if(!hwModePedometerBoolPreference.isChecked){

                    Toast.makeText(
                        activity!!.applicationContext,
                        "Pedometer on HW mode enabled " +
//                                "${R.bool.hw_pedometer_bool.toString()}" +
                                "",
                        Toast.LENGTH_SHORT
                    ).show()
                } else{
                    Toast.makeText(
                        activity!!.applicationContext,
                        "Pedometer on HW mode disabled " +
//                                "${R.bool.hw_pedometer_bool.toString()}" +
                                "",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return true
            }
        })

        val hwModePedometerValPreference= preferenceManager.findPreference<ListPreference>(getString(R.string.hw_pedometer_val_key))
        hwModePedometerValPreference?.setOnPreferenceChangeListener( object : Preference.OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {

                editor.putString(getString(R.string.hw_pedometer_value),newValue.toString())
                editor.commit()

                Toast.makeText(
                    activity!!.applicationContext,
                    "HW mode pedometer settings changed to $newValue steps " +
//                            "${getString(R.string.hw_pedometer_value)}"+
                            ""
                    ,
                    Toast.LENGTH_SHORT
                ).show()

                return true
            }
        })
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