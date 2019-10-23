package com.example.bluecatapp.ui.settings

import android.app.AlertDialog
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import com.example.bluecatapp.MainActivity
import com.example.bluecatapp.R
import com.google.gson.reflect.TypeToken

class SettingsFragment : Fragment() {

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var appList: Array<String>
    private lateinit var selectedApps: BooleanArray

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        settingsViewModel =
            ViewModelProviders.of(this).get(SettingsViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_settings, container, false)
        val textView: TextView = root.findViewById(R.id.text_settings)
        settingsViewModel.text.observe(this, Observer {
            textView.text = it
        })

        val appRestrictButton: Button = root.findViewById(R.id.settings_select_apps)
        appRestrictButton.setOnClickListener {

            val builder = AlertDialog.Builder(context)
            builder.setTitle("Select apps to restrict")

            val type = object : TypeToken<Array<String>>() {}.type

            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            var selectedAppNames: Array<String> =
                if (sharedPrefs.getString("restrictedApps", null) !== null) {
                    MainActivity.gson.fromJson(
                        sharedPrefs.getString("restrictedApps", null),
                        type
                    )
                } else emptyArray()
            val pm = context!!.packageManager
            appList = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { appInfo -> isNotSystemPackage(appInfo) }
                .map { appInfo ->
                    Log.d("bcat *****", appInfo.packageName)
                    appInfo.loadLabel(pm).toString()
                }.toTypedArray().sortedArray()
            selectedApps =
                appList.map { appName -> selectedAppNames.contains(appName) }
                    .toBooleanArray()
            builder.setMultiChoiceItems(
                appList,
                selectedApps
            ) { dialog, which, isChecked ->
                // Update the current focused item's checked status
                selectedApps[which] = isChecked
                selectedAppNames = getCheckedItems(appList, selectedApps)
            }
            // Set a positive button and its click listener on alert dialog
            builder.setPositiveButton("SAVE") { dialog, which ->
                Log.d("bcat", "Save:" + selectedAppNames.joinToString(", "))
                with(sharedPrefs.edit()) {
                    putString(
                        "restrictedApps",
                        MainActivity.gson.toJson(selectedAppNames)
                    )
                    commit()
                }
                Log.d("bcat", "Save restricted apps: " + selectedAppNames.joinToString(", "))
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT)
                    .show()
            }

            // Display a negative button on alert dialog
            builder.setNeutralButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            // Display the alert dialog on interface
            dialog.show()
        }

        return root
    }

    private fun getCheckedItems(array1: Array<String>, array2: BooleanArray): Array<String> {
        return array1.filterIndexed { index, _ -> array2[index] }.toTypedArray()
    }

    private fun isNotSystemPackage(applicationInfo: ApplicationInfo): Boolean {
        val pm = context!!.packageManager

        if (pm.getLaunchIntentForPackage(applicationInfo.packageName) == null) {
            return false
        }

        Log.d("bcat", "Has launch intent ${applicationInfo.loadLabel(pm)}")
        // This is an app you can launch (this excludes most system apps, services)
        if (((applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
            or ((applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
        ) {
            // This is a system app (but Gmail, Chrome, etc are also system apps but we want them)
            // Some system apps can be launched but we are not interested in them.
            // Those apps (empirically) contain the word "System", so we can exclude those apps.
            if (applicationInfo.loadLabel(pm).contains("System")) {
                return false
            }
            Log.d(
                "bcat",
                "Is wanted system app ${applicationInfo.loadLabel(pm)}, ${applicationInfo.packageName}"
            )
            return true
        } else {
            // These are the apps the user installed by themselves
            Log.d("bcat", "Is user installed app ${applicationInfo.loadLabel(pm)}")
            return true

        }
    }
}


