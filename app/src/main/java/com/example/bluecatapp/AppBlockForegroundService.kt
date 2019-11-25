package com.example.bluecatapp

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.gson.reflect.TypeToken


class AppBlockForegroundService : Service() {
    // Constants
    private val CHANNEL_ID = "AppBlockForegroundService"
    private val UPDATE_INTERVAL: Long = 1000

    // Mutable maps for blocked app list
    private lateinit var myUsageStatsMap: MutableMap<String, UsageStats>
    private lateinit var currentlyBlockedApps: MutableMap<String, Long>
    private lateinit var appUsageTimers: MutableMap<String, Long>
    private lateinit var appStepCounters: MutableMap<String, Int> // stores step count for blocked apps

    private lateinit var appOps: AppOpsManager
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var currentAppUsageTimerHandler: Handler
    private var currentAppUsageTimerRunnable: Runnable = Runnable {}
    private var currentAppUsage: Long = 0
    private var prevDetectedForegroundAppPackageName: String? = null

    // Shared Preferences
    private lateinit var sharedPrefs: SharedPreferences
    private var hwModeOn: Boolean = false
    private var pedometerEnabled: Boolean = false
    private var maxStepCount = 0


    companion object {
        fun startService(context: Context, message: String) {
            val startIntent = Intent(context, AppBlockForegroundService::class.java)
            startIntent.putExtra("inputExtra", message)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, AppBlockForegroundService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        appUsageTimers = mutableMapOf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("bcat", "Started app blocking service.")

        // Load preferences
//        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        pedometerEnabled = sharedPrefs.getBoolean("pedometer", false)
        maxStepCount = sharedPrefs.getString("pedometer_count", "0")!!.toInt()

        val input = intent?.getStringExtra("inputExtra")
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java).putExtra(
            "frgToLoad", FragmentToLoad.APPBLOCK
        )
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create persistent notification so that process should stay alive
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App blocking notifications")
            .setContentText(input)
            .setSmallIcon(R.drawable.ic_hourglass_empty_black_24dp)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        currentAppUsageTimerHandler = Handler()
        // Keep checking if we should block the current app
        handler = Handler()
        runnable = Runnable {
            checkIfShouldBlockForegroundApp()
            checkForAppsToUnblock()
            checkForAppUsagesToReset()
            /**
             * TODO: improve algorithm for more efficient update interval
             * Suggestion: calculate appropriate check in interval based on
             * block duration current totaltimeinforground
             */
            handler.postDelayed(runnable, UPDATE_INTERVAL)
            getForegroundApp()?.let { retryBlockIfFailed(it) }
        }
        handler.postDelayed(runnable, UPDATE_INTERVAL)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("bcat", "App blocking service destroyed.")
        handler.removeCallbacks(runnable)
        currentAppUsageTimerHandler.removeCallbacks(currentAppUsageTimerRunnable)
    }

    private fun createNotificationChannel() {
        // Create a notification channel for these notifications if supported (sdk over Oreo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "AppBlock Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun hasUsageDataAccessPermission(): Boolean {
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            this.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun getForegroundApp(): String? {
        var foregroundPackageName: String? = null
        val usage = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - (1 * 60 * 1000)
        myUsageStatsMap = usage.queryAndAggregateUsageStats(
            System.currentTimeMillis() - (24 * 60 * 60 * 1000),
            endTime
        )
        val usageEvents = usage.queryEvents(beginTime, endTime)
        val event: UsageEvents.Event = UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundPackageName = event.packageName
            }
        }
        return foregroundPackageName
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

    private fun blockApp(packageName: String) {
        Log.d(
            "bcat",
            "Attempting to block app $packageName"
        )

        var fragmentToLoad: Int = FragmentToLoad.APPBLOCK
        if(hwModeOn){
            // If HW mode enabled redirect to HW mode screen
            fragmentToLoad = FragmentToLoad.TODO
        }

        application.startActivity(
            Intent(this, MainActivity::class.java).putExtra(
                "frgToLoad",
                fragmentToLoad
            ).putExtra("blockedAppName", packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun checkIfShouldBlockForegroundApp() {
        currentlyBlockedApps = getCurrentlyBlockedApps()
        appStepCounters = getAppStepCounters()
        hwModeOn = sharedPrefs.getBoolean("hw_mode_bool", false)

        if (hasUsageDataAccessPermission()) {
            val foregroundApp = getForegroundApp()
            val maxTimeLimit: Long = sharedPrefs.getString("time_limit", "${10 * 1000}")!!.toLong()
            val blockDuration: Long =
                sharedPrefs.getString("block_duration", "${10 * 1000}")?.toLong() ?: (10
                        * 1000) // Default 10 seconds
            val restrictedApps = sharedPrefs.getStringSet("restricted_apps", mutableSetOf())!!
            if (foregroundApp != null) {


                if (hwModeOn && restrictedApps.contains(foregroundApp)) {
                    /**
                     * block restricted app without adding to blocked app list
                     * or checking time limit
                     * until HW mode turned off
                     */
                    foregroundApp?.let { blockApp(it) }
                }
                else {
                    /**
                     * Use regular app blocking algorithm based on usage time
                     */
                    currentAppUsage = appUsageTimers.getOrPut(foregroundApp) { 0 }
                    // Send notification 5 min before block
                    if ((maxTimeLimit - currentAppUsage) == 1000 * 60 * 5.toLong()) {
                        val toast = Toast.makeText(
                            this.applicationContext, "We will block ${getAppNameFromPackage(
                                foregroundApp,
                                this.applicationContext
                            )} in 5 minutes if you continue using it", Toast.LENGTH_LONG
                        )
                        toast.show()
                    }
                    if ((maxTimeLimit - currentAppUsage) == 1000 * 60 * 1.toLong()) {
                        val toast = Toast.makeText(
                            this.applicationContext, "We will block ${getAppNameFromPackage(
                                foregroundApp,
                                this.applicationContext
                            )} in 1 minute if you continue using it", Toast.LENGTH_LONG
                        )
                        toast.show()
                    }
                    if (currentAppUsage >= maxTimeLimit) {
                        // App should be blocked
                        addToBlockList(foregroundApp)
                    }
                    Log.d(
                        "bcat",
                        "| Open app: ${getAppNameFromPackage(
                            foregroundApp,
                            this.applicationContext
                        )} | Blocked apps: $currentlyBlockedApps | Restricted app usage: $currentAppUsage | Max time: ${maxTimeLimit / 1000}s | Block duration: ${blockDuration / 1000}s |"
                    )
                }
            }

            if (!prevDetectedForegroundAppPackageName.equals(foregroundApp)) {
                // A new app has been opened
                // Stop the timer for the old restricted app, if any.
                onCloseRestrictedApp()
                Log.d("bcat", "Cleaned up old timer (that might never have been started btw)")
                if (foregroundApp != null && restrictedApps.contains(foregroundApp)) {
                    // Start the timer for the newly opened restricted app
                    onOpenRestrictedApp(foregroundApp)
                    Log.d("bcat", "Started up new timer")
                }

                // Check if the current app should be blocked
                Log.d(
                    "bcat",
                    "App changed, block? ${if (currentlyBlockedApps.keys.contains(foregroundApp)) "YES" else "No"}"
                )
                if (currentlyBlockedApps.keys.contains(foregroundApp)) {
                    foregroundApp?.let { blockApp(it) }
                }
            }
            if (!TextUtils.isEmpty(foregroundApp)) {
                prevDetectedForegroundAppPackageName = foregroundApp
            }
        }
    }

    private fun retryBlockIfFailed(packageName: String) {
        val currentlyBlockedApps = getCurrentlyBlockedApps()
        if (currentlyBlockedApps.keys.contains(packageName)) {
            Log.d(
                "bcat",
                "Retry blocking $packageName"
            )
            blockApp(packageName)
        }

    }

    // Queries all device's app usage stats in a given time interval
    private fun getUsageStatsForApp(targetPackageName: String): UsageStats? {
        myUsageStatsMap.forEach { (_, usageStats) ->
            if (usageStats.packageName == targetPackageName) {
                return usageStats
            }
        }
        return null
    }

    private fun incrementTimer(packageName: String) {
        currentAppUsage += 1000
        appUsageTimers[packageName] = currentAppUsage
        with(sharedPrefs.edit()) {
            putString("appUsageTimers", MainActivity.gson.toJson(appUsageTimers))
            commit()
        }
    }

    private fun resetTimer(packageName: String) {
        currentAppUsage = 0
        appUsageTimers[packageName] = 0
        with(sharedPrefs.edit()) {
            putString("appUsageTimers", MainActivity.gson.toJson(appUsageTimers))
            commit()
        }
    }

    private fun checkForAppsToUnblock() {
        var didChange = false
        var unblockList: MutableSet<String> = mutableSetOf()
        currentlyBlockedApps.forEach { (appName, unblockTime) ->
            if (unblockTime < System.currentTimeMillis()
                && !pedometerEnabled || (appStepCounters[appName]==null || appStepCounters[appName]!! >= maxStepCount)
            ) {
                unblockList.add(appName)
                didChange = true
            }
        }
        if (didChange) {
            // remove apps from unblock lists
            unblockList.forEach {
                currentlyBlockedApps.remove(it)
                if(pedometerEnabled && appStepCounters[it]!=null) appStepCounters.remove(it)
            }
            var unblockListPrettyNames = ""
            unblockList.forEach { appName ->
                unblockListPrettyNames += "${
                getAppNameFromPackage(
                    appName,
                    this.applicationContext
                )} "
            }
            Toast.makeText(
                this.applicationContext,
                "Block lifted: $unblockListPrettyNames",
                Toast.LENGTH_LONG
            )
                .show()
            with(sharedPrefs.edit()) {
                putString("currentlyBlockedApps", MainActivity.gson.toJson(currentlyBlockedApps))
                putString("appStepCounters", MainActivity.gson.toJson(appStepCounters))
                commit()
            }
        }
    }

    private fun checkForAppUsagesToReset() {
        val blockDuration: Long =
            sharedPrefs.getString("block_duration", "${10 * 60 * 1000}")!!.toLong()
        val restrictedApps = sharedPrefs.getStringSet("restricted_apps", mutableSetOf())!!

        appUsageTimers.forEach { (packageName: String, usageTime: Long) ->
            val usageStats: UsageStats? = getUsageStatsForApp(packageName)
            if (usageTime > 0 && restrictedApps.contains(packageName)
                && (usageStats?.lastTimeUsed != null)
                && ((System.currentTimeMillis() - usageStats.lastTimeUsed) > blockDuration)
            ) {
                resetTimer(packageName)
                Toast.makeText(
                    this.applicationContext, "Reset app usage for ${getAppNameFromPackage(
                        packageName,
                        this.applicationContext
                    )}", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun onCloseRestrictedApp() {

        currentAppUsageTimerHandler.removeCallbacks(currentAppUsageTimerRunnable)
    }

    private fun onOpenRestrictedApp(appName: String) {
        currentAppUsageTimerRunnable = Runnable {
            incrementTimer(appName)
            currentAppUsageTimerHandler.postDelayed(currentAppUsageTimerRunnable, 1000)
        }
        currentAppUsageTimerHandler.postDelayed(currentAppUsageTimerRunnable, 1000)
    }

    private fun addToBlockList(packageName: String) {
        val blockDuration: Long =
            sharedPrefs.getString("block_duration", "${10 * 60 * 1000}")?.toLong() ?: (10 * 60
                    * 1000) // Default 10 seconds

        currentlyBlockedApps[packageName] = System.currentTimeMillis() + blockDuration
        appStepCounters[packageName] = 0 // initialize step count as 0

        with(sharedPrefs.edit()) {
            putString("currentlyBlockedApps", MainActivity.gson.toJson(currentlyBlockedApps))
            putString("appStepCounters", MainActivity.gson.toJson(appStepCounters))
            commit()
        }
        resetTimer(packageName)
        Toast.makeText(
            this.applicationContext,
            "Added ${getAppNameFromPackage(
                packageName,
                this.applicationContext
            )} to block list (${blockDuration / 1000}s",
            Toast.LENGTH_SHORT
        ).show()
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
        return packageName
    }
}

