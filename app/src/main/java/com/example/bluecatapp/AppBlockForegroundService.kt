package com.example.bluecatapp

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.gson.reflect.TypeToken
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.example.bluecatapp.ui.appblocking.AppBlockingFragment
import java.util.*


class AppBlockForegroundService : Service() {
    private val CHANNEL_ID = "AppBlockForegroundService"
    private lateinit var appOps: AppOpsManager
//    private lateinit var usage: UsageStatsManager
    private lateinit var myUsageStatsMap: MutableMap<String, UsageStats>
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private val UPDATE_INTERVAL: Long = 1000
    private lateinit var sharedPrefs: SharedPreferences
    private var prevDetectedForegroundAppPackageName: String? = null
    /**
     * Mark time stamp when Start Service button is pressed
     * App usage time limit for blocking based on when user manually starts monitoring
     */
    private var monitorStartTimeStamp: Long = 0

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
        appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("bcat", "Started app blocking service.")
        monitorStartTimeStamp = Calendar.getInstance().timeInMillis
        Toast.makeText(
            this.applicationContext,
            "Monitoring from $monitorStartTimeStamp : ${monitorStartTimeStamp/(1000*3600) % 24} ms",
            Toast.LENGTH_SHORT
        ).show()
        // Load preferences
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

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

        // Keep checking if we should block the current app
        handler = Handler()
        runnable = Runnable {
            checkIfShouldBlockForegroundApp()
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
        //query usage time starting from when Start button was clicked
        val endTime = System.currentTimeMillis()
        Log.d(
            "bcat",
            "Current End TIme is $endTime"
        )
        val beginTime = monitorStartTimeStamp

        myUsageStatsMap = usage.queryAndAggregateUsageStats(beginTime, endTime)

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

    private fun getCurrentlyBlockedApps(): MutableMap<String, Long>? {
        val type = object : TypeToken<MutableMap<String, Long>>() {}.type
        val blockedAppsJson = sharedPrefs.getString("currentlyBlockedApps", null)

        //TODO: FIXME
//        val blockDuration = sharedPrefs.getString("block_duration", "0")!!.toLong()
        val currentlyBlockedApps: MutableMap<String, Long>? =
            if (blockedAppsJson !== null) MainActivity.gson.fromJson(
                blockedAppsJson,
                type
            ) else null
        return currentlyBlockedApps
    }

    private fun blockApp(packageName: String) {
        Log.d(
            "bcat",
            "Attempting to block app $packageName"
        )
        application.startActivity(
            Intent(this, MainActivity::class.java).putExtra(
                "frgToLoad",
                FragmentToLoad.APPBLOCK
            ).putExtra("blockedAppName", packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun checkIfShouldBlockForegroundApp() {
        val currentlyBlockedApps = getCurrentlyBlockedApps()
        if (currentlyBlockedApps !== null) {
            if (hasUsageDataAccessPermission()) {
                val foregroundApp = getForegroundApp()
                val totalUsageTime = (foregroundApp?.let { getUsageStatsForApp(it)?.totalTimeInForeground }) ?: 0
                sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
                // Default maxTimeLimit set to 10s
                // TODO: check when deactivated
                val maxTimeLimit= sharedPrefs.getString("time_limit", "0")!!.toLong()
                Log.d(
                    "bcat",
                    "MAX TIME LIMIT IS $maxTimeLimit"
                )
                Log.d(
                    "bcat",
                    "CHECK - ${if (!prevDetectedForegroundAppPackageName.equals(foregroundApp)) 
                        "APP CHANGED - " else ""}Prev app $prevDetectedForegroundAppPackageName, current app open $foregroundApp"
                )

                if (!prevDetectedForegroundAppPackageName.equals(foregroundApp)) {
                    // A new app has been opened, check if it should be blocked
                    Log.d(
                        "bcat",
                        "App changed, block? ${if (currentlyBlockedApps.keys.contains(foregroundApp)) "YES" else "No"}"
                    )
                    // && ((totalUsageTime?:0) >= maxTimeLimit)
                    if (currentlyBlockedApps.keys.contains(foregroundApp) && (totalUsageTime >= maxTimeLimit)) {
                        foregroundApp?.let { blockApp(it) }
                    }
                }
                if (!TextUtils.isEmpty(foregroundApp)) {
                    prevDetectedForegroundAppPackageName = foregroundApp
                }
            }
        }
    }

    private fun retryBlockIfFailed(packageName: String) {
        val currentlyBlockedApps = getCurrentlyBlockedApps()
        if (currentlyBlockedApps != null) {

            if (currentlyBlockedApps.keys.contains(packageName)) {
                Log.d(
                    "bcat",
                    "Retry blocking $packageName"
                )
                blockApp(packageName)
            }
        }
    }

    // Queries all device's app usage stats in a given time interval
    private fun getUsageStatsForApp(targetPackageName: String): UsageStats? {
        myUsageStatsMap.forEach { (_,usageStats) ->
            if (usageStats.packageName == targetPackageName) {
                return usageStats
            }
        }
        return null
    }
}
