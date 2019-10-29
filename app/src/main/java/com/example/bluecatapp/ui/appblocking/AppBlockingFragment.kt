package com.example.bluecatapp.ui.appblocking

import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
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
import kotlinx.android.synthetic.main.fragment_appblocking.*

class AppBlockingFragment : Fragment() {
    private lateinit var appOps: AppOpsManager
    private lateinit var appBlockingViewModel: AppBlockingViewModel
    private lateinit var usage: UsageStatsManager
    private lateinit var packageManager: PackageManager
    private lateinit var countDownText: TextView

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
        val countDownDisplay: ProgressBar = root.findViewById(R.id.countdownProgressBar)
        countDownText = root.findViewById(R.id.countdown_number)

        startButton.setOnClickListener {
            AppBlockForegroundService.startService(context!!, "Monitoring.. ")
        }
        stopButton.setOnClickListener {
            AppBlockForegroundService.stopService(context!!)
        }
        setTimer(60000, 1000)
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

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        // TODO: determine algorithm for querying app block duration
        // Retrieve blocking duration value in milliseconds
        val blockDuration: Long = (sharedPrefs.getString("block_duration", null)?.toLong()!!)
        // Deactivated if blocking duration is negative
        Toast.makeText(
            activity!!.applicationContext,
            "Blocking Duration is $blockDuration ms",
            Toast.LENGTH_SHORT
        ).show()

        // TODO: create app list with block duration based on settings configuration
        val currentlyBlockedApps: MutableMap<String, Long> = mutableMapOf(
            "com.android.chrome" to blockDuration,
            "com.google.android.youtube" to blockDuration
        )
        with(sharedPrefs.edit()) {
            putString("currentlyBlockedApps", MainActivity.gson.toJson(currentlyBlockedApps))
            commit()
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
        val titleMessage = SpannableStringBuilder()
            .append("Allow ")
            .bold { append("Bluecat ") }
            .append("to access usage data?")

        alert.setTitle(titleMessage)
        alert.setMessage("In order to use the App Blocking feature, please enable \"Usage Access Permission\" on your device.")

        alert.setPositiveButton("OK") { dialog, which ->
            // Redirect to settings to enable usage access permission
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        alert.setNegativeButton("Cancel") { dialog, which ->
            Toast.makeText(
                activity!!.applicationContext,
                "Permission request denied.",
                Toast.LENGTH_SHORT
            ).show()
            activity!!.onBackPressed();
        }

        val dialog: AlertDialog = alert.create()
        dialog.show() // Display the alert dialog on app interface
    }

    private fun setTimer(countDownFromTime: Long, countDownInterval: Long){
        var count=0

        val countDownTimer = object: CountDownTimer(countDownFromTime, countDownInterval){
            override fun onTick(millisUntilFinished: Long) {
                count += 1
                val progressValue = (count * 100 / (countDownFromTime/countDownInterval)).toInt()
                countDownText.setText(count)
                countdownProgressBar.setProgress(progressValue)
            }

            override fun onFinish() {
                countdownProgressBar.setProgress(100)
            }
        }
        countDownTimer.start()
    }

    private fun blockApp() {
        // TODO: Use this function for showing user info about blocked app
    }
}


