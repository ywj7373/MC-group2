package com.example.bluecatapp.ui.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.app.AlarmManager
import android.app.PendingIntent
import java.util.Calendar
import android.os.Build

const val ROUTINE_REQEUST_CODE = 486486

class RoutineReceiver : BroadcastReceiver() {
    private val TAG = "RoutineReceiver"
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive called")
        val mServiceIntent = Intent(context, RoutineService::class.java)
        mServiceIntent.action = "Run"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(mServiceIntent)
        } else context.startService(mServiceIntent)
    }

    // Set AlarmManager
    fun setRoutine(context: Context) {
        // get a Calendar object with current time
        Log.d(TAG, "setRoutine called")
        val receiverIntent = Intent(context, RoutineReceiver::class.java)
        val sender = PendingIntent.getBroadcast(context, ROUTINE_REQEUST_CODE, receiverIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Not accurate to 60 second in order to save battery. Use setExact to be accurate.
        val serviceIntent = Intent(context, RoutineService::class.java)
        serviceIntent.action = "Run"
        context.startService(serviceIntent)
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().timeInMillis, 60000, sender)
    }

    // Free AlarmManager
    fun unsetRoutine(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val receiverIntent = Intent(context, RoutineReceiver::class.java)
        val sender = PendingIntent.getBroadcast(context, ROUTINE_REQEUST_CODE, receiverIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        //stop foreground service
        val serviceIntent = Intent(context, RoutineService::class.java)
        serviceIntent.action = "Stop"
        context.startService(serviceIntent)
        //stop alarm manager
        alarmManager.cancel(sender)
    }
}