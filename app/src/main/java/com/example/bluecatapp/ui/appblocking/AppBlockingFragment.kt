package com.example.bluecatapp.ui.appblocking

import android.app.Activity
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
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
import androidx.core.text.bold
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluecatapp.AppBlockForegroundService
import com.example.bluecatapp.MainActivity
import com.example.bluecatapp.pedometer.Pedometer
import com.example.bluecatapp.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.fragment_appblocking.*
import java.util.*
import java.util.concurrent.TimeUnit

class AppDisplayListItem(
    val displayName: String?,
    val blockTimeStamp: Long?,
    var blockSteps: Int?,
    var icon: Drawable?,
    var todayUsageString: String
)

class AppBlockingFragment : Fragment() {
    private lateinit var appOps: AppOpsManager
    private lateinit var appBlockingViewModel: AppBlockingViewModel
    private lateinit var usage: UsageStatsManager
    private lateinit var packageManager: PackageManager
    private lateinit var currentlyBlockedApps: MutableMap<String, Long>
    private lateinit var appStepCounters: MutableMap<String, Int>
    private lateinit var sharedPrefs: SharedPreferences

    //Appblock variables
    private lateinit var blockTitle: TextView
    private lateinit var blockedAppName: TextView
    private lateinit var chrono: Chronometer
    private lateinit var blockTimeLabel: TextView
    private lateinit var appIcon: ImageView
    private lateinit var appUsageTime: TextView
    private lateinit var appBlockListTitle: TextView
    private lateinit var divider: View
    private lateinit var divider2: View


    //Pedometer variables
    private lateinit var pedometer: Pedometer
    private var pedometerEnabled: Boolean = false
    private lateinit var sensorManager: SensorManager
    private lateinit var pedometerTitle: TextView
    private lateinit var pedometerLabel: TextView
    private lateinit var pedometerValue: TextView
    private lateinit var pedometerMaxValue: TextView
    private lateinit var motivationalText: TextView
    private var maxStepCount: Int = 10

    private val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 150


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

//    override fun onResume() {
//        super.onResume()
//        Toast.makeText(
//            activity!!.applicationContext,
//            "PEDOMETER RESUMED",
//            Toast.LENGTH_SHORT
//        ).show()
//    }

//    override fun onDestroy() {
//        super.onDestroy()
//        sensorManager.unregisterListener(pedometer)
//    }

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
        blockTitle = root.findViewById(R.id.block_title)
        blockedAppName = root.findViewById(R.id.blocked_app_name)
        appIcon = root.findViewById(R.id.app_icon)
        appUsageTime = root.findViewById(R.id.app_usage_time)
        chrono = root.findViewById(R.id.view_timer)
        blockTimeLabel = root.findViewById(R.id.block_explanation)
        appBlockListTitle = root.findViewById(R.id.app_list_title)
        divider = root.findViewById(R.id.app_divider)

        // Initialize pedometer views
        pedometerTitle = root.findViewById(R.id.step_title)
        pedometerLabel = root.findViewById(R.id.step_explanation)
        pedometerValue = root.findViewById(R.id.step_count)
        pedometerMaxValue = root.findViewById(R.id.max_step_count)
        motivationalText = root.findViewById(R.id.motivational_text)
        divider2 = root.findViewById(R.id.app_divider2)

        // Check if pedometer enabled in preferences
        pedometerEnabled = sharedPrefs.getBoolean("pedometer", false)
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
            blockTitle.setText("ACTIVE APP BLOCK")

            currentlyBlockedApps.forEach { (appPackageName, finishTimeStamp) ->
                blockedAppName.setText(getAppNameFromPackage(appPackageName, context!!))
                pedometerValue.setText("${appStepCounters[appPackageName]} / $maxStepCount")
                motivationalText.visibility = View.VISIBLE
                appIcon.setImageDrawable(getAppIcon(appPackageName))
                if (System.currentTimeMillis() < finishTimeStamp) {
                    getBlockCountdown(
                        finishTimeStamp,
                        chrono
                    ).start()
                } else if (finishTimeStamp <= System.currentTimeMillis()) {
                    chrono.setText("00:00")
                    chrono.setTextColor(Color.parseColor("#8bc34a"))
                }
                if (!pedometerEnabled) {
                    hidePedometerViews()
                } else if (appStepCounters[appPackageName] != null && appStepCounters[appPackageName]!! >= maxStepCount) {
                    pedometerValue.setTextColor(Color.parseColor("#8bc34a"))
                } else if (appStepCounters[appPackageName] != null
                    && appStepCounters[appPackageName]!! < maxStepCount
                ) {
                    // Activate pedometer sensor
//                    checkGoogleFitPermissions()
//                        startActivity(Intent(requireContext(), StepCounter::class.java))
                    startPedometer(appPackageName, maxStepCount)
                }
            }
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appblocking_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = AppBlockingAdapter(getAdapterList(), maxStepCount, pedometerEnabled)
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



        // Check permissions
        private fun checkGoogleFitPermissions(){
            // Declare FitAPI data types
            val fitnessOptions =
                FitnessOptions.builder()
                    .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                    .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                    .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                    .build()

            if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(context), fitnessOptions)) {
                GoogleSignIn.requestPermissions(
                    this, // your activity
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(context),
                    fitnessOptions)
            } else {
                accessGoogleFit()
                Toast.makeText(
                    activity!!.applicationContext,
                    "Google fit api permission already exists",
                    Toast.LENGTH_SHORT
                ).show()

            }
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
                Toast.makeText(
                    activity!!.applicationContext,
                    "Google Fit api permission granted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Access GoogleAPI client
    /** Records step data by requesting a subscription to background step data. */
    fun subscribe() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        Fitness.getRecordingClient(context!!, GoogleSignIn.getLastSignedInAccount(context)!!)
            .subscribe(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnCompleteListener { task ->
                if (task.isSuccessful()) {
                    Log.i("GOOGLE FIT", "Successfully subscribed!")
                } else {
                    Log.w("GOOGLE FIT", "There was a problem subscribing.", task.getException())
                }
            }
    }

    /**
     * Reads the current daily step total, computed from midnight of the current day on the device's
     * current timezone.
     */
    private fun readData(): Int {
        var total = 0
        Fitness.getHistoryClient(context!!, GoogleSignIn.getLastSignedInAccount(context)!!)
            .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener { dataSet ->
                if (!dataSet.isEmpty) {
                    total = dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt()
                    Log.i("GOOGLE FIT", "Total steps: $total")
                }
                Toast.makeText(
                    activity!!.applicationContext,
                    "Google fit: $total steps",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e -> Log.w(
                "GOOGLE FIT",
                "There was a problem getting the step count."
            )}
        return total
    }

    private fun accessGoogleFit() {
        val googleSignInAccount: GoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(context)!!

        val cal: Calendar = Calendar.getInstance()
        cal.time
        val endTime: Long  = cal.timeInMillis
        cal.add(Calendar.YEAR, -1)
        val startTime: Long = cal.timeInMillis

        val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .bucketByTime(1, TimeUnit.DAYS)
                .build()

//        val pendingResult = Fitness.HistoryApi.readData(client, readRequest)
//pendingResult.setResultCallback(ResultCallback<DataReadResult>() {
//
//    fun onResult(@NonNull dataReadResult: DataReadResult) {
//        val allBuckets:List<Bucket>  = dataReadResult.buckets
//
//        for (bucket in allBuckets) {
//            val startAtSeconds = bucket.getStartTime(TimeUnit.SECONDS);

//            val stepsValue: Value = getValue(bucket, DataType.TYPE_STEP_COUNT_DELTA, Field.FIELD_STEPS);
//            val steps: Int = stepsValue.asInt()
//
//            Log.d("GOOGLE_FIT", String.format("startAtSeconds %s, steps %s", startAtSeconds, steps));
//        }
//    }
//})
//        var result = Fitness.getHistoryClient(activity!!, googleSignInAccount)
//                .readData(readRequest)
//                .addOnSuccessListener(object: OnSuccessListener<DataReadResponse> {
//                    override fun onSuccess(dataReadResponse: DataReadResponse ) {
//                        Toast.makeText(
//                            activity!!.applicationContext,
//                            "Google Fit OnSuccess",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                })
//                .addOnFailureListener(object: OnFailureListener {
//                    override fun onFailure(p0: Exception) {
//                        Toast.makeText(
//                            activity!!.applicationContext,
//                            "Google Fit OnFailure",
//                            Toast.LENGTH_SHORT
//                        ).show()                    }
//                })
//                .addOnCompleteListener(object: OnCompleteListener<DataReadResponse>{
//                    override fun onComplete(p0: Task<DataReadResponse>) {
//                        Toast.makeText(
//                            activity!!.applicationContext,
//                            "Google Fit OnComplete",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                })
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
                chrono.setText("00:00")
                chrono.setTextColor(Color.parseColor("#8bc34a"))
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

    // TODO: Use proper function to get the string for today's usage of an app!!
    private fun getAdapterList(): List<AppDisplayListItem> {
        val restrictedApps = sharedPrefs.getStringSet("restricted_apps", mutableSetOf())!!
        var blockedAppList: MutableList<AppDisplayListItem> = arrayListOf()
        restrictedApps.forEach { appPackageName ->
            val blockFinishTimeStamp =
                if (currentlyBlockedApps.contains(appPackageName)) currentlyBlockedApps[appPackageName] else null
            blockedAppList.add(
                AppDisplayListItem(
                    getAppNameFromPackage(appPackageName, context!!),
                    blockFinishTimeStamp,
                    appStepCounters[appPackageName],
                    getAppIcon(appPackageName), "0.0 hours"
                )
            )
        }
        blockedAppList.sortWith(compareBy { it.displayName })
        return blockedAppList
    }

    private fun startPedometer(appName: String, numberOfSteps: Int) {
        // initialize step counter views
        pedometerValue.setText("${appStepCounters[appName]}")
        pedometerMaxValue.setText(" of $numberOfSteps")

        val pedometer = StepCounter(activity!!)
        pedometer.accessGoogleFit()
        val initialCount = pedometer.getStepCount()

//        var steps = Step(numberOfSteps)

//        val handler = Handler()
//        object: Runnable {
//            override fun run() {
//                pedometer.accessGoogleFit()
//                appStepCounters[appName] = appStepCounters[appName]!! + pedometer.getStepCount()
//                with(sharedPrefs.edit()) {
//                    // update changed values
//                    putString("appStepCounters", MainActivity.gson.toJson(appStepCounters))
//                    commit()
//                }
//                pedometerValue.setText("${appStepCounters[appName]}")
//                handler.postDelayed(this, 1000)
//            }
//        }
//        pedometer = object : Pedometer() {
//            override fun onSensorChanged(event: SensorEvent?) {
//                if (appStepCounters[appName]!! < maxStepCount) {
//                    super.onSensorChanged(event)
//                }
//            }
//
//            override fun step(timeNs: Long) {
//                super.step(timeNs)
//                appStepCounters[appName] = appStepCounters[appName]!! + super.numSteps
//                with(sharedPrefs.edit()) {
//                    // update changed values
//                    putString("appStepCounters", MainActivity.gson.toJson(appStepCounters))
//                    commit()
//                }
//                pedometerValue.setText("${appStepCounters[appName]}")
//
//                if (appStepCounters[appName]!! >= numberOfSteps) {
//                    pedometerValue.setTextColor(Color.parseColor("#8bc34a"))
//                    pedometerMaxValue.setTextColor(Color.parseColor("#8bc34a"))
//                }
//            }
//        }
        sensorManager = activity!!.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager!!.registerListener(
            object: SensorEventListener{
                override fun onSensorChanged(event: SensorEvent?) {

                    pedometer.accessGoogleFit()
                    appStepCounters[appName] = appStepCounters[appName]!! + (pedometer.getStepCount() - initialCount)
                    with(sharedPrefs.edit()) {
                        // update changed values
                        putString("appStepCounters", MainActivity.gson.toJson(appStepCounters))
                        commit()
                    }
                    pedometerValue.setText("${appStepCounters[appName]}")
//                    newCount = readData() - initialCount
//                    appStepCounters[appName] = appStepCounters[appName]!! + newCount
//                with(sharedPrefs.edit()) {
//                    // update changed values
//                    putString("appStepCounters", MainActivity.gson.toJson(appStepCounters))
//                    commit()
//                }
//                pedometerValue.setText("${appStepCounters[appName]}")

                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            },
            sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
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

    // Pedometer feature using built in pedometer hardware
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
                pedometerValue.setText("$currentStepCount / $maxStepCount steps")
            }
        }
    }

    private fun hideViews() {
        //Hide app blocking countdown and pedometer views if no currently blocked apps
        blockTitle.text = "NO ACTIVE APP BLOCK"
        blockTitle.marginTop
        val blockTitleParams = blockTitle.layoutParams as ViewGroup.MarginLayoutParams
        blockTitleParams.topMargin = 150
        val dividerParams = divider.layoutParams as ViewGroup.MarginLayoutParams
        dividerParams.topMargin = 200

        blockedAppName.visibility = View.GONE
        appIcon.visibility = View.GONE
        chrono.visibility = View.GONE
        blockTimeLabel.visibility = View.GONE
        appUsageTime.visibility = View.GONE
        divider2.visibility = View.GONE
        motivationalText.visibility = View.GONE
        hidePedometerViews()
    }

    private fun hidePedometerViews() {
        // Hide and collapse pedometer views
        pedometerTitle.visibility = View.GONE
        pedometerLabel.visibility = View.GONE
        pedometerValue.visibility = View.GONE
        pedometerMaxValue.visibility = View.GONE
    }
}



