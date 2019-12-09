package com.example.bluecatapp.ui.settings

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.text.bold
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
        checkIfActiveAppBlock()

        editor = preference.edit()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val profilePreference = findPreference<EditTextPreference>(getString(R.string.profile))
        profilePreference?.summary = "Display Name"

        val appBlockPreference =
            preferenceManager.findPreference<SwitchPreference>(getString(R.string.appblock))
        // App block monitoring starts automatically when toggled on
        appBlockPreference?.setOnPreferenceChangeListener { preference, newValue ->
            if (!appBlockPreference.isChecked) {
                // Toggle on: App blocking enabled
                AppBlockForegroundService.startService(context!!, "Monitoring.. ")

                Toast.makeText(
                    activity!!.applicationContext,
                    "App blocking enabled",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Toggle off: App blocking disabled
                AppBlockForegroundService.stopService(context!!)
                Toast.makeText(
                    activity!!.applicationContext,
                    "App blocking disabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
            true
        }

        val locationReminderPreference =
            preferenceManager.findPreference<SwitchPreference>(getString(R.string.enable_location))
        locationReminderPreference?.setOnPreferenceChangeListener { preference, newValue ->
            if (!locationReminderPreference.isChecked) {

                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    editor.putBoolean("Location Reminder", true)
                    editor.commit()
                    LocationReminderForegroundService.startService(context!!)
                    Toast.makeText(
                        activity!!.applicationContext,
                        "Location Based Reminder enabled",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        activity!!.applicationContext,
                        "For location reminder mode to work, we need access to use your permission.",
                        Toast.LENGTH_LONG
                    ).show()
                    requestLocationPermission()
                }
            } else {
                editor.putBoolean("Location Reminder", false)
                editor.commit()
                LocationReminderForegroundService.stopService(context!!)
                Toast.makeText(
                    activity!!.applicationContext,
                    "Location Based Reminder disabled",
                    Toast.LENGTH_SHORT
                ).show()
            }

            true
        }

        val preparationTimePreference =
            preferenceManager.findPreference<ListPreference>(getString(R.string.preparation_time))
        preparationTimePreference?.setOnPreferenceChangeListener { preference, newValue ->
            editor.putString("Preparation_time", newValue.toString())
            editor.commit()

            Toast.makeText(
                activity!!.applicationContext,
                "Preparation time changed to ${newValue.toString().toInt()} minutes ",
                Toast.LENGTH_SHORT
            ).show()

            true
        }

        val resetStatisticsPreference =
            preferenceManager.findPreference<Preference>(getString(R.string.reset_statistic))
        resetStatisticsPreference?.setOnPreferenceClickListener {
            // Dialogue to confirm
            val message = SpannableStringBuilder()
                .append("Are you sure you want to reset?\n")
                .bold { append("This cannot be undone.") }
            AlertDialog.Builder(requireContext())
                .setTitle("Reset location reminder statistics")
                .setMessage(message)
                .setPositiveButton("RESET STATISTICS") { dialogInterface, i ->
                    LocationRepository(activity!!.application).resetStats()
                    Toast.makeText(
                        activity!!.applicationContext,
                        "Reset location reminder statistics",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton(android.R.string.no) { dialogInterface, i ->
                }
                .show()

            true
        }

        val hwModeTimePreference =
            preferenceManager.findPreference<ListPreference>(getString(R.string.hw_time_key))
        hwModeTimePreference?.setOnPreferenceChangeListener { preference, newValue ->
            editor.putInt(getString(R.string.TIMER_LENGTH_ID), newValue.toString().toInt())
            editor.commit()
            true
        }

        val hwModeShakeCountPreference =
            preferenceManager.findPreference<ListPreference>(getString(R.string.hw_shake_val_key))
        hwModeShakeCountPreference?.setOnPreferenceChangeListener { preference, newValue ->
            editor.putInt(getString(R.string.hw_shake_value), newValue.toString().toInt())
            editor.commit()
            true
        }

        val hwModePedometerBoolPreference =
            preferenceManager.findPreference<SwitchPreference>(getString(R.string.hw_pedometer_bool_key))
        hwModePedometerBoolPreference?.setOnPreferenceChangeListener { preference, newValue ->
            // why is it backward...??
            editor.putBoolean(
                getString(R.string.hw_pedometer_bool),
                !hwModePedometerBoolPreference.isChecked
            )
            editor.commit()

            Log.d(
                "SettingsFragment:hwModePedometerBoolPreference",
                "isEnabled: ${!hwModePedometerBoolPreference.isChecked}"
            )
            true
        }

        val hwModePedometerValPreference =
            preferenceManager.findPreference<ListPreference>(getString(R.string.hw_pedometer_val_key))
        hwModePedometerValPreference?.setOnPreferenceChangeListener { preference, newValue ->
            editor.putInt(getString(R.string.hw_pedometer_value), newValue.toString().toInt())
            editor.commit()
            true
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (preference is RestrictAppsPreference) {
            val dialogFragment: DialogFragment =
                RestrictAppsPreferenceFragmentCompat.newInstance(preference.key)
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(fragmentManager!!, null)
        } else super.onDisplayPreferenceDialog(preference)
    }

    // Request for permission for location
    private fun requestLocationPermission() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            requireActivity(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (shouldProvideRationale) {
            Log.d("Setting Fragment", "Displaying permission rationale")
            Toast.makeText(
                requireContext(),
                "We need permission to use your location to enable location reminder",
                Toast.LENGTH_LONG
            ).show()
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), PERMISSION_ID
            )

        } else {
            Log.d("Setting Fragment", "Requesting Location Permission")
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), PERMISSION_ID
            )
        }
    }

    // Check if the location tracker is enabled in the setting
    private fun isLocationTrackerEnabled(): Boolean {
        val locationManager: LocationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    // Called after the user allows or denies our requested permission
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permission: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_ID && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (!isLocationTrackerEnabled()) {
                Toast.makeText(
                    requireActivity(),
                    "Turn on location mode in the settings",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            } else {
                editor.putBoolean("Location Reminder", true)
                editor.apply()
                LocationReminderForegroundService.startService(requireContext())
            }
        } else {
            // Turn off location reminder
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val editor = sharedPreferences.edit()
            editor.putBoolean("Location Based Reminder", false)
            editor.apply()
        }
    }

    private fun checkIfActiveAppBlock() {
        val appBlockingSetting =
            findPreference<SwitchPreference>(getString(R.string.appblock))
        val appBlockingPedometerSetting =
            findPreference<SwitchPreference>("pedometer")

        val blockedAppsJson = preference.getString("currentlyBlockedApps", "{}")
        if (blockedAppsJson!! != "{}") {
            appBlockingSetting!!.isEnabled = false
            appBlockingSetting.summaryOn =
                "App blocking enabled. Cannot disable when there is an active app block."

            if (appBlockingPedometerSetting!!.isChecked) {
                appBlockingPedometerSetting.isEnabled = false
                appBlockingPedometerSetting.summaryOn =
                    "Step count feature enabled during app blocking. Cannot disable when there is an active app block."
                val appBlockingPedometerStepCountSetting =
                    findPreference<ListPreference>("pedometer_count")
                appBlockingPedometerStepCountSetting!!.isEnabled = false
            }
        }
    }
}