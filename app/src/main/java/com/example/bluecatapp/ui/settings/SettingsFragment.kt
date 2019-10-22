package com.example.bluecatapp.ui.settings

import android.app.AlertDialog
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

            appList = arrayOf(
                "com.android.chrome",
                "com.google.android.youtube",
                "com.google.android.gm",
                "com.google.android.apps.maps"
            )

            val type = object : TypeToken<Array<String>>() {}.type

            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            var selectedAppNames: Array<String> =
                if (sharedPrefs.getString("restrictedApps", null) !== null)
                    MainActivity.gson.fromJson(
                        sharedPrefs.getString("restrictedApps", null),
                        type
                    )
                else emptyArray()
            selectedApps =
                appList.map { appName -> selectedAppNames.contains(appName) }.toBooleanArray()
            builder.setMultiChoiceItems(appList, selectedApps) { dialog, which, isChecked ->
                // Update the current focused item's checked status
                selectedApps[which] = isChecked
                // Get the current focused item
                val currentItem = appList[which]
                // Notify the current action
                Toast.makeText(context, "$currentItem $isChecked", Toast.LENGTH_SHORT)
                    .show()

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
                Log.d("bcat", "Save:" + selectedAppNames.joinToString(", "))
            }

            // Display a negative button on alert dialog
            builder.setNeutralButton("Cancel") { dialog, which ->
                Toast.makeText(
                    context,
                    "Permission request denied.",
                    Toast.LENGTH_SHORT
                ).show()
                activity!!.onBackPressed()
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
}

