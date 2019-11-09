package com.example.bluecatapp.ui.appblocking

import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Chronometer
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.text.bold
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import com.example.bluecatapp.AppBlockForegroundService
import com.example.bluecatapp.MainActivity
import com.example.bluecatapp.R
import com.google.gson.reflect.TypeToken
import android.hardware.SensorManager as SensorManager1


class AppBlockingFragment : Fragment() {
    private lateinit var appOps: AppOpsManager
    private lateinit var appBlockingViewModel: AppBlockingViewModel
    private lateinit var usage: UsageStatsManager
    private lateinit var packageManager: PackageManager
    private lateinit var currentlyBlockedApps: MutableMap<String, Long>
    private lateinit var sensorManager: SensorManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        packageManager = context.packageManager
        currentlyBlockedApps = getCurrentlyBlockedApps()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        appBlockingViewModel =
            ViewModelProviders.of(this).get(AppBlockingViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_appblocking, container, false)
//        val textView: TextView = root.findViewById(R.id.text_appblocking)
//        appBlockingViewModel.text.observe(this, Observer {
//            textView.text = it
//        })
        val startButton: Button = root.findViewById(R.id.start_foreground_service)
        val stopButton: Button = root.findViewById(R.id.stop_foreground_service)
        val blockedAppName: TextView = root.findViewById(R.id.currently_blocked_app)
        val chrono: Chronometer = root.findViewById(R.id.view_timer)
        val blockTimeLabel: TextView = root.findViewById(R.id.block_explanation)
        //Pedometer views
        val pedometerTitle: TextView = root.findViewById(R.id.step_title)
        val pedometerLabel: TextView = root.findViewById(R.id.step_explanation)
        val pedometerValue: TextView = root.findViewById(R.id.step_count)

        startButton.setOnClickListener {
            AppBlockForegroundService.startService(context!!, "Monitoring.. ")
        }
        stopButton.setOnClickListener {
            AppBlockForegroundService.stopService(context!!)
        }
        if (currentlyBlockedApps.entries.count() == 0) {
            blockedAppName.text = "No blocked apps at the moment"
            chrono.visibility = View.INVISIBLE
            blockTimeLabel.visibility = View.INVISIBLE
            pedometerTitle.visibility = View.INVISIBLE
            pedometerLabel.visibility = View.INVISIBLE
            pedometerValue.visibility = View.INVISIBLE
        } else {
            currentlyBlockedApps.forEach { (appPackageName, finishTimeStamp) ->
                blockedAppName.setText(getAppNameFromPackage(appPackageName, context!!))
                getBlockCountdown(
                    finishTimeStamp,
                    chrono,
                    blockedAppName,
                    blockTimeLabel
                ).start()
                //start pedometer
                stepCounter(pedometerValue)
            }
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
            activity!!.onBackPressed()
        }

        val dialog: AlertDialog = alert.create()
        dialog.show() // Display the alert dialog on app interface
    }

    private fun getBlockCountdown(
        countDownFromTime: Long, chrono: Chronometer,
        blockedAppName: TextView, blockedAppCountdownLabel: TextView

    ): CountDownTimer {
        val msToFinish = countDownFromTime - System.currentTimeMillis()
        chrono.base = SystemClock.elapsedRealtime() + msToFinish
        chrono.start()
        return object : CountDownTimer(msToFinish, 1000) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                chrono.stop()
                chrono.visibility = View.INVISIBLE
                blockedAppCountdownLabel.visibility = View.INVISIBLE
                blockedAppName.setText("No blocked apps at the moment")
            }
        }
    }

    private fun getAppNameFromPackage(packageName: String, context: Context): String? {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val pkgAppsList = context.packageManager
            .queryIntentActivities(mainIntent, 0)

        for (app in pkgAppsList) {
            if (app.activityInfo.packageName == packageName) {
                return app.activityInfo.loadLabel(context.packageManager).toString()
            }
        }
        return null
    }

    private fun getCurrentlyBlockedApps(): MutableMap<String, Long> {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val type = object : TypeToken<MutableMap<String, Long>>() {}.type
        val blockedAppsJson = sharedPrefs.getString("currentlyBlockedApps", null)

        currentlyBlockedApps =
            if (blockedAppsJson !== null) MainActivity.gson.fromJson(
                blockedAppsJson,
                type
            ) else mutableMapOf()
        return currentlyBlockedApps
    }

    private fun stepCounter(stepCountValue: TextView): SensorEventListener {
        val sensorManager = context!!.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val totalCount = 20 //total number of steps to be completed
        stepCountValue.setText("0 / $totalCount") //initialize value

        return object : SensorEventListener{
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSensorChanged(event: SensorEvent?) {
                stepCountValue.setText("${event!!.values[0]} / $totalCount")
            }
        }
    }
}


