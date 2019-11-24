package com.example.bluecatapp.ui.appblocking

import android.Manifest.permission.ACTIVITY_RECOGNITION
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.text.bold
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluecatapp.AppBlockForegroundService
import com.example.bluecatapp.MainActivity
import com.example.bluecatapp.R
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.fragment_appblocking.*


class AppBlockingFragment : Fragment() {
    private lateinit var appOps: AppOpsManager
    private lateinit var appBlockingViewModel: AppBlockingViewModel
    private lateinit var usage: UsageStatsManager
    private lateinit var packageManager: PackageManager
    private lateinit var currentlyBlockedApps: MutableMap<String, Long>
    private lateinit var appStepCounters: MutableMap<String, Int>
    private lateinit var sharedPrefs: SharedPreferences

    //Appblock variables
    private lateinit var blockedAppName: TextView
    private lateinit var chrono: Chronometer
    private lateinit var blockTimeLabel: TextView
    private lateinit var appIcon: ImageView

    //Pedometer variables
    private lateinit var sensorManager: SensorManager
    private lateinit var pedometerTitle: TextView
    private lateinit var pedometerLabel: TextView
    private lateinit var pedometerValue: TextView
    private var pedometerSensor: Sensor? = null
    private var maxStepCount: Int = 10


    override fun onAttach(context: Context) {
        super.onAttach(context)
        appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        packageManager = context.packageManager
        currentlyBlockedApps = getCurrentlyBlockedApps()
        appStepCounters = getAppStepCounters()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPrefs = getDefaultSharedPreferences(this.context)
    }

    override fun onResume() {
        super.onResume()
        Toast.makeText(
            activity!!.applicationContext,
            "PEDOMETER RESUMED",
            Toast.LENGTH_SHORT
        ).show()
        startActivityForResult(
            Intent(requireContext(), Pedometer::class.java), 200
        )
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

        // Initialize app block views
        blockedAppName = root.findViewById(R.id.currently_blocked_app)
        chrono = root.findViewById(R.id.view_timer)
        blockTimeLabel = root.findViewById(R.id.block_explanation)
        appIcon = root.findViewById(R.id.appIcon)

        // Initialize pedometer views
        pedometerTitle = root.findViewById(R.id.step_title)
        pedometerLabel = root.findViewById(R.id.step_explanation)
        pedometerValue = root.findViewById(R.id.step_count)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(ACTIVITY_RECOGNITION),
                200
            )
        }
        sensorManager = activity!!.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)?.let {
            pedometerSensor = it
        }
        sharedPrefs = getDefaultSharedPreferences(this.context)
        val pedometerEnabled = sharedPrefs.getBoolean("Pedometer", false)
        Log.d("bcat", "behehehehheeh " + pedometerEnabled)
        if (pedometerEnabled) {
            if (pedometerSensor != null) {
                sensorManager.registerListener(
                    stepCounter(),
                    pedometerSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                Toast.makeText(
                    activity!!.applicationContext,
                    "PEDOMETER CREATED",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    activity!!.applicationContext,
                    "PEDOMETER NOT FOUND",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Retrieve makeStepCount from sharedPreferences
        maxStepCount = sharedPrefs.getString("pedometer_count", "0")!!.toInt()

        // Check if app blocking enabled in Settings
        val isEnabled = sharedPrefs.getBoolean(getString(R.string.appblock), false)
        if (isEnabled) {
            AppBlockForegroundService.startService(context!!, "Monitoring...")
        } else {
            AppBlockForegroundService.stopService(context!!)
        }

        if (currentlyBlockedApps.entries.count() == 0) {
            hideViews()
        } else {
            currentlyBlockedApps.forEach { (appPackageName, finishTimeStamp) ->
                blockedAppName.setText(getAppNameFromPackage(appPackageName, context!!))
                pedometerValue.setText("${appStepCounters[appPackageName]} / $maxStepCount")
                appIcon.setImageDrawable(getAppIcon(appPackageName))
                if (System.currentTimeMillis() < finishTimeStamp) {
                    getBlockCountdown(
                        finishTimeStamp,
                        chrono
                    ).start()
                }
//                if (appStepCounters[appPackageName]!=null && appStepCounters[appPackageName]!! < maxStepCount) {
//                    // Start pedometer simulation
//                    simulatePedometer(appPackageName, maxStepCount)
//                }
            }
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appblocking_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = AppBlockingAdapter(getAdapterList(), maxStepCount)
        }
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

    private fun getBlockCountdown(countDownFromTime: Long, chrono: Chronometer): CountDownTimer {
        val msToFinish = countDownFromTime - System.currentTimeMillis()
        chrono.base = SystemClock.elapsedRealtime() + msToFinish
        chrono.start()
        return object : CountDownTimer(msToFinish, 1000) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                chrono.stop()
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

    private fun getAppIcon(packageName: String): Drawable? {
        var icon: Drawable? = null
        try {
            icon = packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return icon
    }

    private fun getCurrentlyBlockedApps(): MutableMap<String, Long> {
        val sharedPrefs = getDefaultSharedPreferences(context)
        val type = object : TypeToken<MutableMap<String, Long>>() {}.type
        val blockedAppsJson = sharedPrefs.getString("currentlyBlockedApps", null)

        currentlyBlockedApps =
            if (blockedAppsJson !== null) MainActivity.gson.fromJson(
                blockedAppsJson,
                type
            ) else mutableMapOf()
        return currentlyBlockedApps
    }

    private fun getAppStepCounters(): MutableMap<String, Int> {
        val sharedPrefs = getDefaultSharedPreferences(context)
        val type = object : TypeToken<MutableMap<String, Int>>() {}.type
        val appStepCountersJson = sharedPrefs.getString("appStepCounters", null)

        appStepCounters =
            if (appStepCountersJson !== null) MainActivity.gson.fromJson(
                appStepCountersJson,
                type
            ) else mutableMapOf()
        return appStepCounters
    }

    // FIXME: Return list of blocked app names with respective finish time stamps
    private fun getAdapterList(): List<List<Any?>> {
        var blockedAppList: MutableList<List<Any?>> = arrayListOf()
        currentlyBlockedApps.forEach { (appPackageName, finishTimeStamp) ->
            blockedAppList.add(
                listOf(
                    getAppNameFromPackage(appPackageName, context!!),
                    finishTimeStamp, appStepCounters[appPackageName], getAppIcon(appPackageName)
                )
            )
        }
        return blockedAppList
    }

    private fun stepCounter(): SensorEventListener {
        return object : SensorEventListener {

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

            override fun onSensorChanged(event: SensorEvent?) {
                val currentStepCount = event!!.values[0].toInt()
                Toast.makeText(
                    activity!!.applicationContext,
                    "YOU MOVED $currentStepCount STEPS",
                    Toast.LENGTH_SHORT
                ).show()
                pedometerValue.setText("$currentStepCount / $maxStepCount")

//                if(currentStepCount==maxStepCount){
//                    pedometerTitle.setText("All steps completed")
//                    pedometerLabel.setText("Great job!")
//                }
            }
        }
    }

    /**Function to simulate pedometer
     * Increments step count every 2s
     */
    private fun simulatePedometer(appName: String, numberOfSteps: Int) {
        val countDownFromTime = ((numberOfSteps - appStepCounters[appName]!!) * 2000).toLong()

        object : CountDownTimer(countDownFromTime, 2000) {
            override fun onTick(millisUntilFinished: Long) {
                appStepCounters[appName] = appStepCounters[appName]!! + 1
                with(sharedPrefs.edit()) {
                    // update changed values
                    putString("appStepCounters", MainActivity.gson.toJson(appStepCounters))
                    commit()
                }
                pedometerValue.setText("${appStepCounters[appName]} / $numberOfSteps")
            }

            override fun onFinish() {}
        }.start()
    }

    // Check if device has built-in system feature
    private fun checkSensorFeatures(feature: String) {
        if (packageManager.hasSystemFeature(feature)) {
            Toast.makeText(
                activity!!.applicationContext,
                "$feature feature found",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                activity!!.applicationContext,
                "$feature feature not found",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun hideViews() {
        //Hide app blocking countdown and pedometer views if no currently blocked apps
        blockedAppName.text = "No blocked apps at the moment"
        appIcon.visibility = View.INVISIBLE
        chrono.visibility = View.INVISIBLE
        blockTimeLabel.visibility = View.INVISIBLE
        pedometerTitle.visibility = View.INVISIBLE
        pedometerLabel.visibility = View.INVISIBLE
        pedometerValue.visibility = View.INVISIBLE
    }
}



