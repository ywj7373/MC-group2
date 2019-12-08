package com.example.bluecatapp.ui.appblocking

import android.app.Activity
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.net.ConnectivityManager
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
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.text.bold
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluecatapp.AppBlockForegroundService
import com.example.bluecatapp.MainActivity
import com.example.bluecatapp.pedometer.Pedometer
import com.example.bluecatapp.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.data.Value
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import com.google.android.gms.tasks.Task
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.fragment_appblocking.*
import java.util.concurrent.TimeUnit

class AppDisplayListItem(
    val displayName: String?,
    val blockTimeStamp: Long?,
    var blockSteps: Int?,
    var icon: Drawable?,
    var todayUsageString: String,
    var remainingUsage: String
)

class AppBlockingFragment : Fragment() {
    private lateinit var appOps: AppOpsManager
    private lateinit var appBlockingViewModel: AppBlockingViewModel
    private lateinit var usage: UsageStatsManager
    private lateinit var packageManager: PackageManager
    private lateinit var currentlyBlockedApps: MutableMap<String, Long>
    private lateinit var appUsageTimers: MutableMap<String, Long>
    private lateinit var appStepCounters: MutableMap<String, Int>
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var usageStatsMap: MutableMap<String, UsageStats>
    private lateinit var restrictedApps: Set<String>

    // Appblock variables
    private lateinit var blockTitle: TextView
    private lateinit var blockedAppName: TextView
    private lateinit var chrono: Chronometer
    private lateinit var blockTimeLabel: TextView
    private lateinit var appIcon: ImageView
    private lateinit var appUsageTime: TextView
    private lateinit var appBlockListTitle: TextView
    private lateinit var totalUsageTime: TextView
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
        sharedPrefs = getDefaultSharedPreferences(context)
        appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        packageManager = context.packageManager
        currentlyBlockedApps = getCurrentlyBlockedApps()
        appStepCounters = getAppStepCounters()
        usageStatsMap = getUsageStatsMap()
        appUsageTimers = getAppUsageTimers()
        restrictedApps = sharedPrefs.getStringSet("restricted_apps", mutableSetOf())!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPrefs = getDefaultSharedPreferences(this.context)

        /** Declare FitAPI data types */
        fitnessOptions =
            FitnessOptions.builder()
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        appBlockingViewModel =
            ViewModelProviders.of(this).get(AppBlockingViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_appblocking, container, false)

        // Initialize app block views
        blockTitle = root.findViewById(R.id.block_title)
        blockedAppName = root.findViewById(R.id.blocked_app_name)
        appIcon = root.findViewById(R.id.app_icon)
        appUsageTime = root.findViewById(R.id.app_usage_time)
        chrono = root.findViewById(R.id.view_timer)
        blockTimeLabel = root.findViewById(R.id.block_explanation)
        appBlockListTitle = root.findViewById(R.id.app_list_title)
        divider = root.findViewById(R.id.app_divider)
        totalUsageTime = root.findViewById(R.id.total_usage_time)

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

        totalUsageTime.text =
            "Total usage of restricted apps today: ${getTotalUsageTimeDayAllRestrictedApps()}"

        if (currentlyBlockedApps.entries.count() == 0) {
            hideViews()
        } else {
            blockTitle.setText("ACTIVE APP BLOCK")

            currentlyBlockedApps.forEach { (appPackageName, finishTimeStamp) ->
                blockedAppName.setText(getAppNameFromPackage(appPackageName))
                appUsageTime.text =
                    "Total usage today: " + getAppTotalUsageTimeDay(appPackageName, true)
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

    /************************************************
     * Pedometer and GoogleFit API functions
     ***********************************************/

    private fun requestGoogleFitPermissions() {

        if(isConnectedToNetwork()) {
            // From Android 10 (API 29) Activity Recognition permissions required
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                if (
                    ActivityCompat.checkSelfPermission(
                        requireContext(),
                        android.Manifest.permission.ACTIVITY_RECOGNITION
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    // Requesting Activity Recognition permissions
                    ActivityCompat.requestPermissions(
                        activity!!,
                        arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                        MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION
                    )
                }
            }

            if (!GoogleSignIn.hasPermissions(
                    GoogleSignIn.getLastSignedInAccount(context),
                    fitnessOptions
                )
            ) {
                GoogleSignIn.requestPermissions(
                    this, // your activity
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(context),
                    fitnessOptions
                )
            } else {
                Log.d(TAG, "Google fit api permission already exists")
            }
        } else{
            Log.d(TAG, "NO AVAILABLE NETWORK CONNECTION")
            Toast.makeText(
                activity!!.applicationContext,
                "No Internet connection",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Check for existing permissions
    private fun checkGoogleFitPermissions(fitnessOptions: FitnessOptions): Boolean {
        return GoogleSignIn.hasPermissions(
            GoogleSignIn.getLastSignedInAccount(context),
            fitnessOptions
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
                Log.d(TAG,"Google fit api permission granted")
            }
        }
    }

    /**
     * Enable to stop GoogleFit pedometer tracking in background
     */
//    override fun onStop() {
//        super.onDestroyView()
//        if(checkGoogleFitPermissions(fitnessOptions)){
//            // Unsubscribe GoogleFit Recording Client
//            GoogleSignIn.getLastSignedInAccount(context)?.let {
//                Fitness.getRecordingClient(context!!, it)
//                    .unsubscribe(DataType.TYPE_STEP_COUNT_DELTA)
//                    .addOnCompleteListener { task ->
//                        if (task.isSuccessful) {
//                            Log.d(TAG, "Successfully unsubscribed!")
//                        } else {
//                            Log.d(TAG, "There was a problem unsubscribing.", task.exception)
//                        }
//                    }
//            }
//        }
//    }

    /** Start pedometer feature using GoogleFit Api or by overriding Pedometer class */
    private fun startPedometer(appName: String, numberOfSteps: Int) {
        Log.d(TAG, "Pedometer started")
        // initialize step counter views
        pedometerValue.text = "${appStepCounters[appName]}"
        pedometerMaxValue.text = " of $numberOfSteps"

        /* Check permissions for GoogleFit API */
        var hasGoogleFitPermissions = false
        requestGoogleFitPermissions()
        hasGoogleFitPermissions = checkGoogleFitPermissions(fitnessOptions)

        if(hasGoogleFitPermissions) {
            Toast.makeText(
                activity!!.applicationContext,
                "GoogleFit enabled",
                Toast.LENGTH_SHORT
            ).show()
            subscribeGoogleFit()
            // start recording GoogleFit step count
            readGoogleFitStepData(appName)
        } else {
            /**
             *  No permission for GoogleFit
             *  Use accelerometer-based pedometer as fallback option
             * */
            Toast.makeText(
                activity!!.applicationContext,
                "GoogleFit disabled",
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

    /** Read Google Fit Data */
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
                val value : Value = p0.getValue(field) // Number of steps detected by listener
                //                    Value(TotalSteps);
                //                     TotalSteps=val+TotalSteps;
                Log.i(TAG, "Detected DataPoint field: ${field.name}")
                Log.i(TAG, "Detected DataPoint value: $value")

                /**
                 * Update values upon data change here
                 */
                val stepCountOfApp = appStepCounters[appName]

                if (stepCountOfApp!! < maxStepCount) {
                    // Increment step count
                    appStepCounters[appName] = stepCountOfApp +
                            value.asInt() % (maxStepCount - stepCountOfApp + 1)
                    // prevent sum from exceeding maxStepCount
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
                    total = dataSet.dataPoints[0].getValue(Field.FIELD_STEPS).asInt()
                    Log.d(TAG, "Total steps today: $total")
                }
            }
            .addOnFailureListener {Log.w(
                TAG,
                "There was a problem getting the step count."
            )}
        return total
    }

    // Check for network connectivity
    @Suppress("DEPRECATION")
    private fun isConnectedToNetwork(): Boolean {
        Log.d(TAG, "CHECKING NETWORK")
        val connectivityManager = context?.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager?
        return connectivityManager?.activeNetworkInfo?.isConnectedOrConnecting ?: false
    }

    /************************************************/

    /************************************************/


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

    private fun getAppNameFromPackage(targetPackageName: String): String {
        val fullAppList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        fullAppList.forEach { appInfo: ApplicationInfo ->
            if (appInfo.packageName == targetPackageName) {
                return appInfo.loadLabel(packageManager).toString()
            }
        }
        return targetPackageName
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
        var blockedAppList: MutableList<AppDisplayListItem> = arrayListOf()
        restrictedApps.forEach { appPackageName ->
            val blockFinishTimeStamp =
                if (currentlyBlockedApps.contains(appPackageName)) currentlyBlockedApps[appPackageName] else null
            blockedAppList.add(
                AppDisplayListItem(
                    getAppNameFromPackage(appPackageName),
                    blockFinishTimeStamp,
                    appStepCounters[appPackageName],
                    getAppIcon(appPackageName),
                    getAppTotalUsageTimeDay(appPackageName),
                    getRemainingAppUsageToString(appPackageName)
                )
            )
        }
        blockedAppList.sortWith(compareBy { it.displayName })
        return blockedAppList
    }

    private fun hideViews() {
        //Hide app blocking countdown and pedometer views if no currently blocked apps
        blockTitle.text = "NO ACTIVE APP BLOCK"
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

    private fun getUsageStatsMap(): MutableMap<String, UsageStats> {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - (24 * 60 * 60 * 1000)
        // Usage stats for all apps over the past 24 hours
        return usage.queryAndAggregateUsageStats(beginTime, endTime)
    }

    private fun convertMsToHoursToString(millis: Long, usePretty: Boolean = false): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours)

        return String.format(
            if (usePretty) "%dh %dmin" else "%02d:%02d",
            hours, minutes
        )
    }

    private fun getTotalUsageTimeDayAllRestrictedApps(): String {
        var result: Long = 0
        usageStatsMap.filter { (packageName: String, _) ->
            restrictedApps.contains(packageName)
        }.forEach { (_, usageStats) ->
            result += usageStats.totalTimeInForeground
        }
        return convertMsToHoursToString(result, true)
    }

    private fun getAppTotalUsageTimeDay(
        targetPackageName: String,
        usePretty: Boolean = false
    ): String {
        usageStatsMap.forEach { (_, usageStats) ->
            if (usageStats.packageName == targetPackageName) {
                return convertMsToHoursToString(usageStats.totalTimeInForeground, usePretty)
            }
        }
        return "00:00"
    }

    private fun getAppUsageTimers(): MutableMap<String, Long> {
        val type = object : TypeToken<MutableMap<String, Long>>() {}.type
        val appUsageTimersJson = sharedPrefs.getString("appUsageTimers", null)

        appUsageTimers =
            if (appUsageTimersJson !== null) MainActivity.gson.fromJson(
                appUsageTimersJson,
                type
            ) else mutableMapOf()
        return appUsageTimers
    }

    private fun getRemainingAppUsageToString(targetPackageName: String): String {
        val maxTimeLimit: Long =
            sharedPrefs.getString("time_limit", "${30 * 60 * 1000}")!!.toLong() // ms
        val appUsage = appUsageTimers.getOrDefault(targetPackageName, 0) // ms
        val millisRemaining = maxTimeLimit - appUsage
        val minutesRemaining = TimeUnit.MILLISECONDS.toMinutes(millisRemaining)

        return String.format("%d min left", minutesRemaining)
    }
}

