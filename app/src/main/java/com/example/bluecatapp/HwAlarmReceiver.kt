package com.example.bluecatapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.PowerManager



class HwAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mPowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        val mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"HW:WakeLock")
        mWakeLock.acquire()

        // Put here YOUR code.
        Toast.makeText(context, "Wake UP !!!!!!!!!! Shake your Phone", Toast.LENGTH_LONG).show()

        mWakeLock.release()
    }

    fun setAlarm(context: Context, setTime : Int) {
        val mAlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(context, HwAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, 0, i, 0)
//        am.setRepeating(
//            AlarmManager.RTC_WAKEUP,
//            System.currentTimeMillis(),
//            (1000 * 60 * 10).toLong(),
//            pi
//        ) // Millisec * Second * Minute

    }

    fun cancelAlarm(context: Context) {
        val intent = Intent(context, HwAlarmReceiver::class.java)
        val sender = PendingIntent.getBroadcast(context, 0, intent, 0)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(sender)
    }
}