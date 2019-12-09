package com.example.goldenpegasus.ui.settings

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.preference.PreferenceDialogFragmentCompat
import com.example.goldenpegasus.R


class RestrictAppsPreferenceFragmentCompat : PreferenceDialogFragmentCompat() {

    private var newValues: MutableSet<String> = mutableSetOf()
    private lateinit var appListEntries: Array<String>
    private lateinit var appListValues: List<String>
    private var preferenceChanged: Boolean = false

    companion object {
        fun newInstance(key: String): RestrictAppsPreferenceFragmentCompat {
            val fragment = RestrictAppsPreferenceFragmentCompat()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b

            return fragment
        }
    }

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view?.findViewById(R.id.pref_dialog_restricted_apps))

        val restrictAppsView =
            view?.findViewById(R.id.pref_dialog_restricted_apps) as ConstraintLayout

        checkNotNull(restrictAppsView) { "Dialog view must contain a LinearLayout with id 'pref_restricted_apps'" }
        Log.d("bcat", "App restriction dialog has been binded")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pref: RestrictAppsPreference = getRestrictAppsPreference()

        pref.setAppListEntries()
        appListEntries = pref.appListEntries.toTypedArray()
        pref.setAppListValues()
        appListValues = pref.appListValues

        newValues.clear()
        newValues.addAll(pref.getSelectedApps())

        preferenceChanged = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val pref: RestrictAppsPreference = getRestrictAppsPreference()
        val checkedItems: BooleanArray = pref.getSelectedItems()
        val builder = AlertDialog.Builder(activity)

        builder.setTitle(R.string.restricted_apps_title)
            .setMultiChoiceItems(
                appListEntries.sortedArray(), checkedItems
            ) { _, which, isChecked ->
                preferenceChanged = if (isChecked) {
                    preferenceChanged or newValues.add(
                        appListValues[which]
                    )
                } else {
                    preferenceChanged or newValues.remove(
                        appListValues[which]
                    )
                }
            }
            .setPositiveButton("Save") { dialog, which ->
                pref.setSelectedApps(newValues)

            }
            .setNegativeButton("Cancel") { dialog, which ->
            }
        return builder.create()
    }


    private fun getRestrictAppsPreference(): RestrictAppsPreference {
        return preference as RestrictAppsPreference
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && preferenceChanged) {
            val pref: RestrictAppsPreference = getRestrictAppsPreference()
            if (pref.callChangeListener(newValues)) {
                pref.setSelectedApps(newValues)
            }
        }
        preferenceChanged = false
    }

}