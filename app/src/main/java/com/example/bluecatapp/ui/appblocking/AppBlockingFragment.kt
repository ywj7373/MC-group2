package com.example.bluecatapp.ui.appblocking

import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.bluecatapp.R
import android.util.Log
import android.provider.Settings
import android.content.Context
import android.content.Intent
import android.text.SpannableStringBuilder
import android.widget.Toast
import androidx.core.text.bold
import android.content.pm.PackageManager
import com.example.bluecatapp.FragmentToLoad
import com.example.bluecatapp.MainActivity

class AppBlockingFragment : Fragment() {
    private lateinit var appOps: AppOpsManager
    private lateinit var appBlockingViewModel: AppBlockingViewModel
    private lateinit var usage: UsageStatsManager
    private lateinit var packageManager: PackageManager
    private lateinit var appsToBlock: MutableList<String>

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        appOps = context?.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        packageManager = context.packageManager

        // TODO: Implement some kind of background service that will check repeatedly if the app in the foreground should be blocked.
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        appBlockingViewModel =
            ViewModelProviders.of(this).get(AppBlockingViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_appblocking, container, false)
        val textView: TextView = root.findViewById(R.id.text_appblocking)
        appBlockingViewModel.text.observe(this, Observer {
            textView.text = it
        })

        return root

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (hasUsageDataAccessPermission()) {
            Log.d("bcat", "Permissions all good")
        } else {
            // Permission is not granted, show alert dialog to request for permission
            showAlertDialog()
        }
        appsToBlock = mutableListOf("com.android.chrome", "com.google.android.youtube")
        if (hasUsageDataAccessPermission()) {

            var currentApp: UsageStats? = null
            val currentTime = System.currentTimeMillis()
            val events: MutableList<UsageStats> =
                usage.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    currentTime - 1000 * 100,
                    currentTime
                )
            events.sortBy { currentTime - it.lastTimeStamp }
            val filteredUsageStats = events.filter {
                appsToBlock.contains(it.packageName)
            }
            filteredUsageStats.forEach {
                // Debug logging
                Log.d(
                    "bcat", "${it.packageName} ${currentTime - it.lastTimeStamp}"
                )
            }
            currentApp = filteredUsageStats[0]

            val blockedAppNameView: TextView = activity!!.findViewById(R.id.text_appblocking_data)
            blockedAppNameView.text = packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(
                    currentApp.packageName,
                    0
                )
            )
        }
    }

    private fun hasUsageDataAccessPermission(): Boolean {
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context!!.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun showAlertDialog() {
        val alert = AlertDialog.Builder(activity!!)

        // Set the alert dialog title
        val titleMessage = SpannableStringBuilder()
            .append("Allow ")
            .bold { append("Bluecat ") }
            .append("to access usage data?")
        alert.setTitle(titleMessage)

        // Display a message on alert dialog
        alert.setMessage("In order to use the App Blocking feature, please enable \"Usage Access Permission\" on your device.")

        // Set a positive button and its click listener on alert dialog
        alert.setPositiveButton("OK") { dialog, which ->
            // Redirect to settings to enable usage access permission
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        // Display a negative button on alert dialog
        alert.setNegativeButton("Cancel") { dialog, which ->
            Toast.makeText(
                activity!!.applicationContext,
                "Permission request denied.",
                Toast.LENGTH_SHORT
            ).show()
            activity!!.onBackPressed();
        }

        // create alert dialog using builder
        val dialog: AlertDialog = alert.create()

        // Display the alert dialog on app interface
        dialog.show()
    }

    private fun blockApp() {
        // TODO: Use this function for blocking app through background service.
        startActivity(
            Intent(context, MainActivity::class.java).putExtra(
                "frgToLoad",
                FragmentToLoad.APPBLOCK
            )
        )
    }
}


