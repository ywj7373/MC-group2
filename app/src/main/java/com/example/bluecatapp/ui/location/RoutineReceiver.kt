package com.example.bluecatapp.ui.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.app.AlarmManager
import android.app.PendingIntent
import android.widget.Toast
import java.util.*
import androidx.core.content.ContextCompat.startForegroundService
import android.os.Build



/*
class RoutineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("RoutineDebug", "onReceive called")
        val mServiceIntent = Intent(context, RoutineService::class.java )
        context?.startService(mServiceIntent)
    }
}
*/
class RoutineReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("RoutineDebug", "onReceive called")
        val mServiceIntent = Intent(context, RoutineService::class.java)
        //context?.startService(mServiceIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(Intent(context, RoutineService::class.java))
        } else {
            context.startService(Intent(context, RoutineService::class.java))
        }
    }

    // Set AlarmManager
    fun setRoutine(context: Context) {
        // get a Calendar object with current time
        Log.d("RoutineDebug", "setRoutine called")
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