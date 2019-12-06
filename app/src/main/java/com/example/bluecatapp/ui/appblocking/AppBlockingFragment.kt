package com.example.bluecatapp.ui.appblocking

import android.app.Activity
import android.app.AlertDialog
import android.app.AppOpsManager
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
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
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
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.Bucket
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.data.Value
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.android.gms.fitness.result.DataReadResult
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.fragment_appblocking.*
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.jar.Manifest

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

    // Appblock variables
    private lateinit var blockTitle: TextView
    private lateinit var blockedAppName: TextView
    private lateinit var chrono: Chronometer
    private lateinit var blockTimeLabel: TextView
    private lateinit var appIcon: ImageView
    private lateinit var appUsageTime: TextView
    private lateinit var appBlockListTitle: TextView
    private lateinit var divider: View
    private lateinit var divider2: View


    // Pedometer variables
    private lateinit var pedometer: Pedometer
    private var pedometerEnabled: Boolean = false
    private lateinit var sensorManager: SensorManager
    private lateinit var pedometerTitle: TextView
    private lateinit var pedometerLabel: TextView
    private lateinit var pedometerValue: TextView
    private lateinit var pedometerMaxValue: TextView
    private lateinit var motivationalText: TextView
    private var maxStepCount: Int = 10

    private lateinit var fitnessOptions: FitnessOptions

    // Constants
    val MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION = 151
    val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 150
    val TAG = "GOOGLE_FIT"


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
    }

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
        // Check usage data access permissions
        if (hasUsageDataAccessPermission()) {
            Log.d("bcat", "Permissions all good")
        } else {
            // Permission is not granted, show alert dialog to request for permission
            showAlertDialog()
        }
    }

    private fun requestGoogleFitPermissions() {
        // Declare FitAPI data types
        fitnessOptions =
            FitnessOptions.builder()
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build()

        // From Android 10 (API 29) Activity Recognition permissions required
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q){
            if (
                ActivityCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Requesting Activity Recognition permissions
                ActivityCompat.requestPermissions(activity!!,
                    arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                    MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION)
            }
        }

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(context), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this, // your activity
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                GoogleSignIn.getLastSignedInAccount(context),
                fitnessOptions)
        } else {
            Toast.makeText(
                activity!!.applicationContext,
                "Google fit api permission already exists",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkGoogleFitPermissions(fitnessOptions: FitnessOptions): Boolean {
        return GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(context), fitnessOptions)
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
    fun subscribeGoogleFit() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        GoogleSignIn.getLastSignedInAccount(context)?.let {
            Fitness.getRecordingClient(context!!, it)
                .subscribe(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Successfully subscribed!")
                    } else {
                        Log.d(TAG, "There was a problem subscribing.", task.exception)
                    }
                }
        }
    }

    /**
     * Reads the current daily step total, computed from midnight of the current day on the device's
     * current timezone.
     */
    private fun readGoogleFitStepData(appName: String): Int {
        var total = 0

        val  fitnessOptions: GoogleSignInOptionsExtension =
                FitnessOptions.builder()
                    .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                    .build()

        val googleSignInAccount =
            GoogleSignIn.getAccountForExtension(context!!, fitnessOptions);

        // Listen to live changes in step count
        val stepCountListener = OnDataPointListener { p0 ->
            p0!!.dataType.fields.forEach { field ->
                val value : Value = p0.getValue(field)
                //                    Value(TotalSteps);
                //                     TotalSteps=val+TotalSteps;
                Log.i(TAG, "Detected DataPoint field: ${field.name}")
                Log.i(TAG, "Detected DataPoint value: $value")

                val stepCountOfApp = appStepCounters[appName]

                if (stepCountOfApp!! < maxStepCount) {
                    // Increment step count
                    appStepCounters[appName] = stepCountOfApp +
                            value.asInt() % (maxStepCount - stepCountOfApp + 1) // sum should not exceed maxStepCount
                }
                // Update changes in step count
                with(sharedPrefs.edit()) {
                    putString("appStepCounters", MainActivity.gson.toJson(appStepCounters))
                    commit()
                }
                pedometerValue.text = "${appStepCounters[appName]}"

                if (appStepCounters[appName]!! >= maxStepCount) {
                    pedometerValue.setTextColor(Color.parseColor("#8bc34a"))
                    pedometerMaxValue.setTextColor(Color.parseColor("#8bc34a"))
                }
            }
        }

        var  response: Task<Void> = Fitness.getSensorsClient(context!!, googleSignInAccount)
                                        .add(SensorRequest.Builder()
                                            .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                                            .setSamplingRate(1, TimeUnit.SECONDS)  // sample once every second
                                            .setAccuracyMode(SensorRequest.ACCURACY_MODE_HIGH)
                                            .build(),
                                            stepCountListener)


        Fitness.getHistoryClient(context!!, GoogleSignIn.getAccountForExtension(context!!, fitnessOptions))
            .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener { dataSet ->
                if (!dataSet.isEmpty) {
                    total = dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt()
                    Log.d("GOOGLE_FIT", "Total steps today: $total")
                }
            }
            .addOnFailureListener {Log.w(
                TAG,
                "There was a problem getting the step count."
            )}
        return total
    }

//    private fun accessGoogleFit() {
//        val googleSignInAccount: GoogleSignInAccount = GoogleSignIn.getAccountForExtension(context!!, fitnessOptions)
//
//        val cal: Calendar = Calendar.getInstance()
//        cal.time
//        val endTime: Long  = cal.timeInMillis
//        cal.add(Calendar.YEAR, -1)
//        val startTime: Long = cal.timeInMillis
//
//        val readRequest = DataReadRequest.Builder()
//                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
//                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
//                .bucketByTime(1, TimeUnit.DAYS)
//                .build()
//
//        val pendingResult = Fitness.HistoryApi.readData(client, readRequest)
//pendingResult.setResultCallback(ResultCallback<DataReadResult>() {
//
//    fun onResult(@NonNull dataReadResult: DataReadResult) {
//        val allBuckets:List<Bucket>  = dataReadResult.buckets
//
//        for (bucket in allBuckets) {
//            val startAtSeconds = bucket.getStartTime(TimeUnit.SECONDS);
//
//            val stepsValue: Value = DataType.getValue(bucket, DataType.TYPE_STEP_COUNT_DELTA, Field.FIELD_STEPS);
//            val steps: Int = stepsValue.asInt()
//
//            Log.d("GOOGLE_FIT", String.format("startAtSeconds %s, steps %s", startAtSeconds, steps));
//        }
//    }
//})
//        var result = Fitness.getHistoryClient(activity!!, googleSignInAccount)
//                .readData(readRequest)
//                .addOnSuccessListener {
//                    Toast.makeText(
//                        activity!!.applicationContext,
//                        "Google Fit OnSuccess",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            .addOnFailureListener {
//                Toast.makeText(
//                    activity!!.applicationContext,
//                    "Google Fit OnFailure",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//            .addOnCompleteListener {
//                Toast.makeText(
//                    activity!!.applicationContext,
//                    "Google Fit OnComplete",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//    }

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
        Log.d(TAG, "Pedometer started")
        // initialize step counter views
        pedometerValue.text = "${appStepCounters[appName]}"
        pedometerMaxValue.text = " of $numberOfSteps"

        /**
         * Check permissions for GoogleFit API
         */
        requestGoogleFitPermissions()
        val hasGoogleFitPermissions = checkGoogleFitPermissions(fitnessOptions)
        var initialGoogleFitData = 0
        val initialStepCount = appStepCounters[appName]?:0

        if(hasGoogleFitPermissions) {
            subscribeGoogleFit()
            // start recording GoogleFit step count
           readGoogleFitStepData(appName)
        } else {
            // Use accelerometer-based pedometer as fallback option
            Toast.makeText(
                activity!!.applicationContext,
                "Using regular pedometer",
                Toast.LENGTH_SHORT
            ).show()

            // Declare pedometer implementation
            pedometer = object : Pedometer() {
                override fun onSensorChanged(event: SensorEvent?) {
                    // Only detect steps if under max step count
                        if (appStepCounters[appName]!! < maxStepCount) {
                            super.onSensorChanged(event)
                        }
                    }

                // Behavior when a step is detected
                override fun step(timeNs: Long) {
                    super.step(timeNs)
                    // Step count should not go over upper bound
                    if (appStepCounters[appName]!! < maxStepCount) {
                        // Increment step count
                        appStepCounters[appName] = appStepCounters[appName]!!.plus(1)
                    }
                    // Update changes in step count
                    with(sharedPrefs.edit()) {
                        putString("appStepCounters", MainActivity.gson.toJson(appStepCounters))
                        commit()
                    }
                    pedometerValue.text = "${appStepCounters[appName]}"

                    if (appStepCounters[appName]!! >= numberOfSteps) {
                        pedometerValue.setTextColor(Color.parseColor("#8bc34a"))
                        pedometerMaxValue.setTextColor(Color.parseColor("#8bc34a"))
                    }
                }
            }
            sensorManager = activity!!.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager!!.registerListener(
                pedometer,
                sensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_FASTEST
            )
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




