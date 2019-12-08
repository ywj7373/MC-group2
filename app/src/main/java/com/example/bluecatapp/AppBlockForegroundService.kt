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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class AppBlockForegroundService : Service() {
    // Constants
    private val CHANNEL_ID = "AppBlockForegroundService"
    private val UPDATE_INTERVAL: Long = 1000
    private var isHandlerRunning: Boolean = false
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
    private var countSwitchedApps: MutableList<Long> = mutableListOf()

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
        Log.d("bcat", "Started app blocking service.")
        super.onCreate()
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        appUsageTimers = mutableMapOf()
        pedometerEnabled = sharedPrefs.getBoolean("pedometer", false)
        maxStepCount = sharedPrefs.getString("pedometer_count", "0")!!.toInt()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

        if (!isHandlerRunning) {
            // Ensure that then runnable does not duplicate.
            Log.d("bcat", "Handler is running")
            isHandlerRunning = true

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
        }
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
        // Usage stats for all apps over the past 24 hours
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
            "\uD83D\uDED1 Attempting to block $packageName"
        )

        if (hwModeOn) {
            // If HW mode enabled redirect to HW mode screen
            application.startActivity(
                Intent(this, TimerActivity::class.java)
                    .putExtra("blockedAppName", packageName)
                    .putExtra("id", getString(R.string.FROMBLOCK))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        } else {
            application.startActivity(
                Intent(this, MainActivity::class.java).putExtra(
                    "frgToLoad",
                    FragmentToLoad.APPBLOCK
                ).putExtra("blockedAppName", packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun checkIfShouldBlockForegroundApp() {
        currentlyBlockedApps = getCurrentlyBlockedApps()
        appStepCounters = getAppStepCounters()
        hwModeOn = sharedPrefs.getBoolean("hw_mode_bool", false)
        val smartBlockingEnabled = sharedPrefs.getBoolean("smart_blocking", false)


        if (hasUsageDataAccessPermission()) {
            val foregroundApp = getForegroundApp()
            val maxTimeLimit: Long = sharedPrefs.getString("time_limit", "${10 * 1000}")!!.toLong()
            val blockDuration: Long =
                sharedPrefs.getString("block_duration", "${10 * 1000}")?.toLong() ?: (10
                        * 1000) // Default 10 seconds
            val restrictedApps = sharedPrefs.getStringSet("restricted_apps", mutableSetOf())!!

            if (foregroundApp == null && prevDetectedForegroundAppPackageName != null) {
                currentAppUsage =
                    appUsageTimers.getOrPut(prevDetectedForegroundAppPackageName!!) { 0 }
            } else if (foregroundApp != null) {
                currentAppUsage = appUsageTimers.getOrPut(foregroundApp) { 0 }
            }

            if (foregroundApp != null) {
                if (hwModeOn && restrictedApps.contains(foregroundApp)) {
                    /**
                     * Block restricted app without adding to blocked app list
                     * or checking time limit until HW mode turned off
                     */
                    blockApp(foregroundApp)
                } else if (smartBlockingEnabled && shouldUseStrictMode()) {
                    // This is the part of the code where apps are added to block list (does not enforce block)
                    var totalAppUsageTime: Long = 0

                    appUsageTimers.forEach {
                        totalAppUsageTime += it.value
                    }

                    if ((maxTimeLimit - totalAppUsageTime) == 1000 * 60 * 5.toLong()) {
                        val toast = Toast.makeText(
                            this.applicationContext,
                            "Strict mode: We will block all restricted apps in 5 minutes if you continue using them",
                            Toast.LENGTH_LONG
                        )
                        toast.show()
                    }
                    if ((maxTimeLimit - totalAppUsageTime) == 1000 * 60 * 1.toLong()) {
                        val toast = Toast.makeText(
                            this.applicationContext,
                            "Strict mode: We will block all restricted apps in 1 minute if you continue using them",
                            Toast.LENGTH_LONG
                        )
                        toast.show()
                    } else if (totalAppUsageTime >= maxTimeLimit) {
                        // All apps should be blocked
                        addAllToBlockList()
                    }
                } else {
                    /**
                     * Use regular app blocking algorithm based on usage time.
                     * This is the part of the code where apps are added to block list (does not enforce block)
                     */
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
                    } else if (currentAppUsage >= maxTimeLimit) {
                        // App should be blocked
                        addToBlockList(foregroundApp)
                    }
                    Log.d(
                        "bcat",
                        "\uD83D\uDCD6 Currently open app: ${getAppNameFromPackage(
                            foregroundApp,
                            this.applicationContext
                        )} | Blocked apps: $currentlyBlockedApps | Restricted app usage: $currentAppUsage | Max time: ${maxTimeLimit / 1000}s | Block duration: ${blockDuration / 1000}s |"
                    )
                }
            }

            // This is the part of the code where usage timers are handled and blocking is enforced.
            if (prevDetectedForegroundAppPackageName != null && foregroundApp == null) {
                /* You are still using the app stored in prevDetectedForegroundAppPackageName
                 * Foreground is null because no new events has been made during the last minute
                 * (e.g. no app moved to background or foreground because you've just stayed in the same app).
                 * Keep tracking usage of that app.
                 */
                Log.d("bcat", "⏳ Still using $prevDetectedForegroundAppPackageName")
            } else if ((prevDetectedForegroundAppPackageName == null && foregroundApp != null) ||
                (prevDetectedForegroundAppPackageName != null && foregroundApp != null && !prevDetectedForegroundAppPackageName.equals(
                    foregroundApp
                ))
            ) {
                // Two cases:
                // 1. First iteration after service has been started and you have opened an app. Regard as "App has switched".  OR
                // 2. Foreground app has changed! "App has switched". Handle stopping usage tracking of previous app and start tracking usage of new app if it is restricted.

                // Stop the timer for the old restricted app, if the previous app was a restricted app.
                onCloseRestrictedApp()
                Log.d(
                    "bcat",
                    "\u23F9️ Stopped usage timer that *might* have previously been started for $prevDetectedForegroundAppPackageName"
                )

                if (restrictedApps.contains(foregroundApp)) {
                    // If the newly opened app is restricted, start tracking the usage
                    onOpenRestrictedApp(foregroundApp)

                    // If smart blocking is enabled, track frequent app switching and add apps to block list if too frequent.
                    if (smartBlockingEnabled && (currentlyBlockedApps.size != restrictedApps.size)) {
                        // Count the switch since the newly opened app is a restricted app
                        countSwitchedApps.add(System.currentTimeMillis())
                        checkIfFrequentAppSwitching()
                    }

                }
                // So we switched the app, should we block it?
                // This is the part of the code that ACTUALLY blocks an app if it should be blocked.
                if (currentlyBlockedApps.keys.contains(foregroundApp)) {
                    blockApp(foregroundApp)
                    Toast.makeText(
                        this.applicationContext,
                        "The app has to be unblocked before you can use it!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                Log.d(
                    "bcat",
                    "↔️ App changed. Should we block it? \uD83E\uDD14 ${if (currentlyBlockedApps.keys.contains(
                            foregroundApp
                        )
                    ) "Yes \uD83D\uDE46️, it's been put in the blocklist."
                    else "No \uD83D\uDE45, it's not currently being blocked."}"
                )
            }
            // Save the previously opened app (if it's not null)
            if (!TextUtils.isEmpty(foregroundApp)) {
                prevDetectedForegroundAppPackageName = foregroundApp
            }
        }
    }

    private fun checkIfFrequentAppSwitching() {
        removeOldAppSwitchingCounts()

        if (countSwitchedApps.size == 7) {
            Toast.makeText(
                this.applicationContext,
                "You're jumping between restricted apps quite frequently. Are you procrastinating?",
                Toast.LENGTH_LONG
            ).show()
        } else if (countSwitchedApps.size == 10) {
            Toast.makeText(
                this.applicationContext,
                "Based on your behaviour we think you are procrastination. Let's take a short break.",
                Toast.LENGTH_LONG
            ).show()

            countSwitchedApps.clear()
            // Block logic
            addAllToBlockList()
        }
    }

    private fun removeOldAppSwitchingCounts() {
        var tempRemoveList = mutableListOf<Long>()
        // Ensure that the list reflects the past 15 minutes only
        countSwitchedApps.forEach { timestamp: Long ->
            if (System.currentTimeMillis() - timestamp > 15 * 60 * 1000) { // 15*60*1000 = 15 min in ms
                tempRemoveList.add(timestamp)
            }
        }
        countSwitchedApps.removeAll(tempRemoveList)

    }

    private fun retryBlockIfFailed(packageName: String) {
        val currentlyBlockedApps = getCurrentlyBlockedApps()
        if (currentlyBlockedApps.keys.contains(packageName)) {
            Log.d(
                "bcat",
                "\uD83D\uDED1 Retry blocking $packageName"
            )
            blockApp(packageName)
        }
    }

    private fun getUsageStatsForApp(targetPackageName: String): UsageStats? {
        // Return the usage stats object for a spesific app. Queried over the past 24 hours.
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
        val minutesConsumed = TimeUnit.MILLISECONDS.toMinutes(currentAppUsage)
        val secondsConsumed = TimeUnit.MILLISECONDS.toSeconds(currentAppUsage)
        Log.d(
            "bcat",
            String.format("⏳ Incremented usage timer for $packageName. Now: $minutesConsumed min consumed ($secondsConsumed s)")
        )
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

    private fun checkForAppUsagesToReset() {
        val blockDuration: Long =
            sharedPrefs.getString("block_duration", "${10 * 60 * 1000}")!!.toLong()
        val restrictedApps = sharedPrefs.getStringSet("restricted_apps", mutableSetOf())!!

        val isStillRunningPrevDetectedApp: Boolean =
            prevDetectedForegroundAppPackageName != null && getForegroundApp() == null

        // Check each timer if it should be reset
        appUsageTimers.forEach { (packageName: String, usageTime: Long) ->
            val usageStats: UsageStats? = getUsageStatsForApp(packageName)

            if (prevDetectedForegroundAppPackageName.equals(packageName)
                && restrictedApps.contains(packageName) && isStillRunningPrevDetectedApp
            ) {
                // If you are running the same app for a longer period of time, this check is needed to avoid resetting usage timer of an app you are still using.
                // Would be a false positive reset due to when lastTimeUsed is updated (when an app is opened and closed I believe. Not every second it is in use, as previously assumed.)
                Log.d(
                    "bcat",
                    "⏳ Didn't reset $packageName since user is still using it. " +
                            if (usageStats != null) "lastTimeUsed: ${TimeUnit.MILLISECONDS.toSeconds(
                                System.currentTimeMillis() - usageStats.lastTimeUsed
                            )}s ago" else ""
                )
            } else if (usageTime > 0 && usageStats != null
                // Check if user has taken a proper break from the app. In that case, reset the usage and let the user start afresh.
                && ((System.currentTimeMillis() - usageStats.lastTimeUsed) > blockDuration)
            ) {
                resetTimer(packageName)
                Log.d(
                    "bcat",
                    "\uD83D\uDCE4 Resetted usage timer for $packageName! lastTimeUsed: ${TimeUnit.MILLISECONDS.toSeconds(
                        System.currentTimeMillis() - usageStats.lastTimeUsed
                    )}s ago"
                )
            }
        }
    }

    private fun checkForAppsToUnblock() {
        pedometerEnabled = sharedPrefs.getBoolean("pedometer", false)
        var didChange = false
        var unblockList: MutableSet<String> = mutableSetOf()
        currentlyBlockedApps.forEach { (appName, unblockTime) ->
            if (unblockTime < System.currentTimeMillis()
                && (!pedometerEnabled
                        || (appStepCounters[appName] == null
                        || appStepCounters[appName]!! >= maxStepCount)
                        )
            ) {
                unblockList.add(appName)
                didChange = true
            }
        }
        if (didChange) {
            // remove apps from unblock lists
            unblockList.forEach {
                currentlyBlockedApps.remove(it)
                if (pedometerEnabled && appStepCounters[it] != null) appStepCounters.remove(it)
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
                "\uD83C\uDFCB️ Block on $unblockListPrettyNames has been lifted.",
                Toast.LENGTH_LONG
            ).show()
            with(sharedPrefs.edit()) {
                putString(
                    "currentlyBlockedApps",
                    MainActivity.gson.toJson(currentlyBlockedApps)
                )
                putString("appStepCounters", MainActivity.gson.toJson(appStepCounters))
                commit()
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

        Log.d(
            "bcat",
            "⏳ Started a usage timer for $appName. Starting usage: $currentAppUsage"
        )
    }

    private fun addToBlockList(packageName: String) {
        val blockDuration: Long =
            sharedPrefs.getString("block_duration", "${10 * 60 * 1000}")?.toLong() ?: (10 * 60
                    * 1000) // Default 10 min

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
            )} to block list (${blockDuration / 1000}s)",
            Toast.LENGTH_SHORT
        ).show()
        Log.d(
            "bcat", "➕ Added ${getAppNameFromPackage(
                packageName,
                this.applicationContext
            )} to block list (${blockDuration / 1000}s)"
        )
    }

    private fun addAllToBlockList() {
        val blockDuration: Long = System.currentTimeMillis() + 10 * 60 * 1000 // 10 minutes in ms
        val restrictedApps = sharedPrefs.getStringSet("restricted_apps", mutableSetOf())!!
        restrictedApps.forEach {
            currentlyBlockedApps[it] = blockDuration
            appStepCounters[it] = 0 // initialize step count as 0
        }

        with(sharedPrefs.edit()) {
            putString("currentlyBlockedApps", MainActivity.gson.toJson(currentlyBlockedApps))
            putString("appStepCounters", MainActivity.gson.toJson(appStepCounters))
            commit()
        }

        appUsageTimers.forEach {
            resetTimer(it.key)
        }
        Log.d("bcat", "➕ Added all restricted apps to block list (${blockDuration / 1000}s)")
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

    private fun shouldUseStrictMode(): Boolean {
        // If the time is between 00:00 and 10:00, be stricter in terms of when to block.

        val startTimeHarshModeRaw = "00:00:00"
        val startTime: Date = SimpleDateFormat("HH:mm:ss").parse(startTimeHarshModeRaw)
        val startTimeHarshMode: Calendar = Calendar.getInstance()
        startTimeHarshMode.time = startTime
        startTimeHarshMode.add(Calendar.DATE, 1)


        val endTimeHarshModeRaw = "10:00:00"
        val endTime: Date = SimpleDateFormat("HH:mm:ss").parse(endTimeHarshModeRaw)
        val endTimeHarshMode: Calendar = Calendar.getInstance()
        endTimeHarshMode.time = endTime
        endTimeHarshMode.add(Calendar.DATE, 1)

        val currentDateTime: Calendar = Calendar.getInstance()
        currentDateTime.add(Calendar.DATE, 1)

        val currentTime: Date = currentDateTime.time
        if (currentTime.after(startTimeHarshMode.time) && currentTime.before(endTimeHarshMode.time)) { // Checks whether the current time is between 00:00:00 and 10:00:00.
            return true
        }
        return false
    }
}

