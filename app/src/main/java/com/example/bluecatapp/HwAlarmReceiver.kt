package com.example.bluecatapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.example.bluecatapp.ui.todo.TodoFragment


class HwAlarmReceiver : BroadcastReceiver() {
    private lateinit var mSensors: Sensors
    private val SHAKE_COMPLETE_CODE = R.integer.SHAKE_COMPLETE_CODE
    private val ALARM_NOTI_REQUEST_CODE = R.integer.ALARM_NOTI_REQUEST_CODE
    private val ALARM_FINAL_REQUEST_CODE = R.integer.ALARM_FINAL_REQUEST_CODE
    private lateinit var vibrator: Vibrator

    override fun onReceive(context: Context, intent: Intent) {

        val iSHAKE_COMPLETE = context.getString(R.string.SHAKE_COMPLETE)
        val iNOTIFICATION = context.getString(R.string.NOTIFICATION)
        val iFINAL_ALARM = context.getString(R.string.FINAL_ALARM)
        val mPowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HW:WakeLock")

        mWakeLock.acquire()

        if (intent.action == iNOTIFICATION) {
            vibratePhone(context)
            //@ todo send notification here
        } else if (intent.action == iFINAL_ALARM) {

            Log.d("HW:onReceive:iFINAL_ALARM","${this::mSensors.isInitialized}")
            if (this::mSensors.isInitialized) {
                mSensors.reInit(context)
            } else {
                mSensors = Sensors(context)
            }

            Toast.makeText(context, "Wake UP !!!!!!!!!! Shake your Phone", Toast.LENGTH_LONG).show()

            for (i in 0 until 5) {
                vibratePhone(context)
            }

        } else if (intent.action == iSHAKE_COMPLETE) {
            var todoFragment = TodoFragment()
            todoFragment.turnHWModeOff()

            mSensors.unRegister()
        } else {
            Log.d("HwAlarmReceiver:onReceive", "action not defined")
        }

        mWakeLock.release()

    }

    // setTime : (user-set) hwModeTime - 5 minute
    fun setNotiAlarm(context: Context, setTime: Long) {
        val mAlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(
            context,
            HwAlarmReceiver::class.java
        ).setAction(context.getString(R.string.NOTIFICATION))
        val pi = PendingIntent.getBroadcast(context, ALARM_NOTI_REQUEST_CODE, i, 0)

        mAlarmManager.set(
            AlarmManager.RTC_WAKEUP,
            setTime,
            pi
        )
    }

    // setTime : (user-set) hwModeTime - 1 second
    fun setFinalAlarm(context: Context, setTime: Long) {
        val mAlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(
            context,
            HwAlarmReceiver::class.java
        ).setAction(context.getString(R.string.FINAL_ALARM))
        val pi = PendingIntent.getBroadcast(context, ALARM_FINAL_REQUEST_CODE, i, 0)

        mAlarmManager.set(
            AlarmManager.RTC_WAKEUP,
            setTime,
            pi
        )
    }

    fun setShakeDoneAlarm(context: Context) {
        val mAlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(
            context,
            HwAlarmReceiver::class.java
        ).setAction(context.getString(R.string.SHAKE_COMPLETE))
        val pi = PendingIntent.getBroadcast(context, SHAKE_COMPLETE_CODE, i, 0)

        mAlarmManager.set(
            AlarmManager.RTC_WAKEUP,
            200,
            pi
        )
    }

    // * alarmType : ALARM_NOTI_REQUEST_CODE or ALARM_FINAL_REQUEST_CODE
    fun cancelAlarm(context: Context, alarmType: Int) {
        val intent = Intent(context, HwAlarmReceiver::class.java)
        val sender = PendingIntent.getBroadcast(context, alarmType, intent, 0)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(sender)
    }


    // * vibrate phone when sending notification * //
    private fun vibratePhone(context: Context) {
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(200)
        }
    }

}