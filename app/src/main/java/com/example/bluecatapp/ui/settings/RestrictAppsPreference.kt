package com.example.bluecatapp.ui.settings

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.Log
import androidx.preference.DialogPreference
import com.example.bluecatapp.R


class RestrictAppsPreference : DialogPreference {

    val dialogLayoutResId = R.layout.pref_dialog_restricted_apps
    lateinit var appListEntries: MutableList<String> // Pretty app names
    lateinit var appListValues: MutableList<String> // Package names
    private lateinit var selectedApps: Set<String>  // Selection package names

    constructor(context: Context?) : super(context, null) {
        isPersistent = false
    }

    constructor(context: Context?, attrs: AttributeSet) : super(
        context,
        attrs,
        R.attr.dialogPreferenceStyle
    )

    constructor(context: Context?, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleAttr
    )

    constructor(
        context: Context?,
        attrs: AttributeSet,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)


    fun setAppListEntries() {
        appListEntries = getAppList(usePrettyName = true)
    }

    fun setAppListValues() {
        appListValues = getAppList(usePrettyName = false)
    }

    fun getSelectedApps(): Set<String> {
        return getPersistedStringSet(mutableSetOf())
    }

    fun setSelectedApps(input: Set<String>) {
        selectedApps = input
        persistStringSet(input)
        Log.d("bcat", "Saved restricted apps: " + selectedApps.joinToString(", "))
    }

    fun getSelectedItems(): BooleanArray {
        var result = BooleanArray(appListValues.size)

        for (i in appListValues.indices) {
            result[i] = selectedApps.contains(appListValues[i])
        }
        return result
    }

    override fun getDialogLayoutResource(): Int {
        return dialogLayoutResId
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Set<String> {
        val defaultValues = a.getTextArray(index)
        val result = mutableSetOf<String>()

        for (defaultValue in defaultValues) {
            result.add(defaultValue.toString())
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    override fun onSetInitialValue(defaultValue: Any?) {
        // Read the value. Use the default value if it is not possible.
        setSelectedApps(getPersistedStringSet(defaultValue as Set<String>?))
    }

    private fun getAppList(usePrettyName: Boolean = false): MutableList<String> {
        // Returns list of package names
        val packageManager = context.packageManager

        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo ->
                isNotSystemPackage(appInfo)
                        // BlueCat app should be excluded from app list
                        && appInfo.packageName != ("com.example.bluecatapp")
            }
            .sortedWith(compareBy { it.loadLabel(packageManager).toString() })
            .map { appInfo ->
                if (usePrettyName) appInfo.loadLabel(packageManager).toString() else appInfo.packageName
            }.toMutableList()

    }

    private fun isNotSystemPackage(applicationInfo: ApplicationInfo): Boolean {
        val pm = context!!.packageManager

        if (pm.getLaunchIntentForPackage(applicationInfo.packageName) == null) {
            return false
        }
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
            return true
        } else {
            // These are the apps the user installed by themselves
            return true

        }
    }

}