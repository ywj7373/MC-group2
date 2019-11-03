package com.example.bluecatapp.ui.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.app.AlarmManager
import android.app.PendingIntent
import java.util.Calendar
import android.os.Build

class RoutineReceiver : BroadcastReceiver() {
    private val TAG = "RoutineReceiver"
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive called")
        val mServiceIntent = Intent(context, RoutineService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(mServiceIntent)
        } else {
            context.startService(mServiceIntent)
        }
    }

    // Set AlarmManager
    fun setRoutine(context: Context) {
        // get a Calendar object with current time
        Log.d(TAG, "setRoutine called")
        val cal = Calendar.getInstance()
        val intent = Intent(context, RoutineReceiver::class.java)
        val sender = PendingIntent.getBroadcast(context, 486486, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Not accurate to 60 second in order to save battery. Use setExact to be accurate.
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 60000, sender)
    }

    // Free AlarmManager
    fun unsetRoutine(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, RoutineReceiver::class.java)
        val sender = PendingIntent.getBroadcast(context, 486486, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.cancel(sender)
    }
}