package com.example.bluecatapp.ui.settings

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.preference.*
import com.example.bluecatapp.AppBlockForegroundService
import com.example.bluecatapp.R
import com.example.bluecatapp.data.LocationRepository
import com.example.bluecatapp.ui.location.LocationReminderForegroundService

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var preference: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val PERMISSION_ID = 270

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

        val locationReminderPreference= preferenceManager.findPreference<SwitchPreference>(getString(R.string.enable_location))
        locationReminderPreference?.setOnPreferenceChangeListener( object : Preference.OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {

                if(!locationReminderPreference.isChecked) {
                    if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        editor.putBoolean("Location Based Reminder", locationReminderPreference.isChecked)
                        editor.commit()
                        LocationReminderForegroundService.startService(requireContext())
                        Toast.makeText(
                            activity!!.applicationContext,
                            "Location Based Reminder enabled",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else {
                        Toast.makeText(
                            activity!!.applicationContext,
                            "Enable location permission!",
                            Toast.LENGTH_SHORT
                        ).show()
                        requestLocationPermission()
                    }
                }
                else {
                    editor.putBoolean("Location Based Reminder", locationReminderPreference.isChecked)
                    editor.commit()
                    Toast.makeText(
                        activity!!.applicationContext,
                        "Location Based Reminder disabled",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                val serviceIntent = Intent(context, LocationReminderForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireContext().startForegroundService(serviceIntent)
                } else requireContext().startService(serviceIntent)


                return true
            }
        })

        val preparationTimePreference= preferenceManager.findPreference<ListPreference>(getString(R.string.preparation_time))
        preparationTimePreference?.setOnPreferenceChangeListener( object : Preference.OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {

                editor.putString("Preparation_time", newValue.toString())
                editor.commit()

                Toast.makeText(
                    activity!!.applicationContext,
                    "Preparation time changed to ${newValue.toString().toInt()} minutes ",
                    Toast.LENGTH_SHORT
                ).show()

                return true
            }
        })

        val resetStatisticsPreference= preferenceManager.findPreference<Preference>(getString(R.string.reset_statistic))
        resetStatisticsPreference?.setOnPreferenceClickListener ( object: Preference.OnPreferenceClickListener {
            override fun onPreferenceClick(preference: Preference?): Boolean {
                LocationRepository(activity!!.application).resetStats()
                return true
            }
        })

        val hwModeTimePreference= preferenceManager.findPreference<ListPreference>(getString(R.string.hw_time_key))
        hwModeTimePreference?.setOnPreferenceChangeListener( object : Preference.OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {

                editor.putInt(getString(R.string.TIMER_LENGTH_ID),newValue.toString().toInt())
                editor.commit()

                Toast.makeText(
                    activity!!.applicationContext,
                    "HW mode time changed to ${newValue.toString()} minutes " +
//                            "${getString(R.string.hw_time_value).toInt()/1000/60}" +
                            "",
                    Toast.LENGTH_SHORT
                ).show()

                return true
            }
        })

        val hwModeShakeCountPreference= preferenceManager.findPreference<ListPreference>(getString(R.string.hw_shake_val_key))
        hwModeShakeCountPreference?.setOnPreferenceChangeListener( object : Preference.OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {

                editor.putInt(getString(R.string.hw_shake_value),newValue.toString().toInt())
                editor.commit()

                Toast.makeText(
                    activity!!.applicationContext,
                    "HW mode shake count changed to ${newValue.toString().toInt()} times " +
//                            "${getString(R.string.hw_shake_value)}" +
                            "",
                    Toast.LENGTH_SHORT
                ).show()

                return true
            }
        })

        val hwModePedometerBoolPreference = preferenceManager.findPreference<SwitchPreference>(getString(R.string.hw_pedometer_bool_key))
        hwModePedometerBoolPreference?.setOnPreferenceChangeListener( object : Preference.OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {

                // why is it backward...??
                editor.putBoolean(getString(R.string.hw_pedometer_bool),!hwModePedometerBoolPreference.isChecked)
                editor.commit()

                Log.d("SettingsFragment:hwModePedometerBoolPreference", "isEnabled : ${!hwModePedometerBoolPreference.isChecked}")
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

                editor.putInt(getString(R.string.hw_pedometer_value),newValue.toString().toInt())
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

    //Request for permission for location
    private fun requestLocationPermission() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
        if (shouldProvideRationale) {
            Log.d("Setting", "Displaying permission rationale")
            //-----------------------permission rationale not yet implemented -------------------------------

        }
        else {
            Log.d("Setting", "Requesting Permission")
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_ID)
        }
    }

    //Called after the user allows or denies our requested permission
    override fun onRequestPermissionsResult(requestCode: Int, permission: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                editor.putBoolean("Location Based Reminder", true)
                editor.apply()
                LocationReminderForegroundService.startService(requireContext())
                Toast.makeText(
                    activity!!.applicationContext,
                    "Location Based Reminder enabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else {
                //turn off location reminder
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
                val editor = sharedPreferences.edit()
                editor.putBoolean("Location Based Reminder", false)
                editor.apply()
            }
        }
    }
}