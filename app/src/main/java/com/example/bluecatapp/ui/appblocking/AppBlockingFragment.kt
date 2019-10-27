package com.example.bluecatapp.ui.appblocking

import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.bold
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import com.example.bluecatapp.AppBlockForegroundService
import com.example.bluecatapp.MainActivity
import com.example.bluecatapp.R

class AppBlockingFragment : Fragment() {
    private lateinit var appOps: AppOpsManager
    private lateinit var appBlockingViewModel: AppBlockingViewModel
    private lateinit var usage: UsageStatsManager
    private lateinit var packageManager: PackageManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        packageManager = context.packageManager
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
        val startButton: Button = root.findViewById(R.id.start_foreground_service)
        val stopButton: Button = root.findViewById(R.id.stop_foreground_service)

        startButton.setOnClickListener {
            AppBlockForegroundService.startService(context!!, "Monitoring.. ")
        }
        stopButton.setOnClickListener {
            AppBlockForegroundService.stopService(context!!)
        }

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
//        val blockDuration: Long = Calendar.getInstance().timeInMillis + 1200000
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        // Retrieve blocking duration value in milliseconds
        val blockDuration: Long = (sharedPrefs.getString("time_limit", null)?.toLong()!!)
        // Deactivated if blocking duration is negative
        if(blockDuration<0){
            Toast.makeText(
                activity!!.applicationContext,
                "Blocking Deactivated",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                activity!!.applicationContext,
                "Blocking Duration is $blockDuration ms",
                Toast.LENGTH_SHORT
            ).show()
            val currentlyBlockedApps: MutableMap<String, Long> = mutableMapOf(
                "com.android.chrome" to blockDuration,
                "com.google.android.youtube" to blockDuration
            )
            with(sharedPrefs.edit()) {
                putString("currentlyBlockedApps", MainActivity.gson.toJson(currentlyBlockedApps))
                commit()
            }
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
        // TODO: Use this function for showing user info about blocked app
    }
}


