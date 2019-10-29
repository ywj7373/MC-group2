package com.example.bluecatapp

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.gson.reflect.TypeToken
import android.text.TextUtils
import android.util.Log

class AppBlockForegroundService : Service() {
    private val CHANNEL_ID = "AppBlockForegroundService"
    private lateinit var appOps: AppOpsManager
//    private lateinit var usage: UsageStatsManager
    private lateinit var usageStatsMap: MutableMap<String, UsageStats>
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private val UPDATE_INTERVAL: Long = 1000
    private lateinit var sharedPrefs: SharedPreferences
    private var prevDetectedForegroundAppPackageName: String? = null

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
        var timeInForeground: Long? = 0

        val usage = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        //query usage time for previous 2 hours
        val beginTime = endTime - (1 * 60 * 60 * 1000)

        val usageEvents = usage.queryEvents(beginTime, endTime)
        val usageStatsMap = usage.queryAndAggregateUsageStats(beginTime, endTime)
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
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val blockedAppsJson = sharedPrefs.getString("currentlyBlockedApps", null)
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
                sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
                //test with 10s
                val maxTimeLimit= 10000//sharedPrefs.getString("time_limit", "0")!!.toLong()

                Log.d(
                    "bcat",
                    "CHECK - ${if (!prevDetectedForegroundAppPackageName.equals(foregroundApp)) "APP CHANGED - " else ""}Prev app $prevDetectedForegroundAppPackageName, current app open $foregroundApp"
                )
                if (!prevDetectedForegroundAppPackageName.equals(foregroundApp)) {
                    // A new app has been opened, check if it should be blocked
                    Log.d(
                        "bcat",
                        "App changed, block? ${if (currentlyBlockedApps.keys.contains(foregroundApp)) "YES" else "No"}"
                    )
                    if (currentlyBlockedApps.keys.contains(foregroundApp)
                        && (usageStatsMap.get(foregroundApp)!!.totalTimeInForeground >= maxTimeLimit)) {
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

    private fun setCountDownTimer(countDownFromTime: Long, interval: Long) {
        val countDownTimer = object: CountDownTimer(countDownFromTime, interval){
            override fun onTick(millisUntilFinished: Long) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onFinish() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }
}

