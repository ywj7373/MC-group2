package com.example.bluecatapp

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.core.app.NotificationCompat.getExtras
import android.os.Bundle

class HwAlarmReceiver : BroadcastReceiver() {
    private lateinit var mSensors: Sensors

    private val HW_NOTIFICATION_CODE = R.integer.HW_NOTIFICATION_CODE
    private val SHAKE_COMPLETE_CODE = R.integer.SHAKE_COMPLETE_CODE
    private val ALARM_NOTI_REQUEST_CODE = R.integer.ALARM_NOTI_REQUEST_CODE
    private val ALARM_FINAL_REQUEST_CODE = R.integer.ALARM_FINAL_REQUEST_CODE
    private lateinit var vibrator: Vibrator

    override fun onReceive(context: Context, intent: Intent) {

        val iSHAKE_COMPLETE = context.getString(R.string.SHAKE_COMPLETE)
        val iWALK_COMPLETE = context.getString(R.string.WALK_COMPLETE)
        val iNOTIFICATION = context.getString(R.string.NOTIFICATION)
        val iFINAL_ALARM = context.getString(R.string.FINAL_ALARM)

        val mPowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HW:WakeLock")

        mWakeLock.acquire()

        when (intent.action) {
            iNOTIFICATION -> {
                Log.d("HwAlarmReceiver:onReceive:iNOTIFICATION", "action : ${intent.action}")

                vibratePhone(context)
                // send notification here

                createNotificationChannel(context)

                // Create an explicit intent for an Activity in your app

                val notificationIntent = NavDeepLinkBuilder(context)
                    .setGraph(R.navigation.mobile_navigation)
                    .setDestination(R.id.navigation_todo)
                    .createPendingIntent()

//            val intent = Intent(context, TodoFragment::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            }

//            val pendingIntent: PendingIntent = PendingIntent.getActivity(context, HW_NOTIFICATION_CODE, intent, 0)

                val hwNotificationChannelId = context.getString(R.string.HW_NOTIFICATION_CHANNEL_ID)
                val hwNotificationChannelTitle = context.getString(R.string.HW_NOTIFICATION_TITLE)
                val hwNotificationChannelContent =
                    context.getString(R.string.HW_NOTIFICATION_CONTENT)

                val builder = NotificationCompat.Builder(context, hwNotificationChannelId)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(hwNotificationChannelTitle)
                    .setContentText(hwNotificationChannelContent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(notificationIntent)
                    .setAutoCancel(true)

                with(NotificationManagerCompat.from(context)) {
                    // notificationId is a unique int for each notification that you must define
                    notify(98, builder.build())
                }

                Toast.makeText(context!!.applicationContext, hwNotificationChannelContent, Toast.LENGTH_LONG).show()
            }
            iFINAL_ALARM -> {
                Log.d("HwAlarmReceiver:onReceive:iFINAL_ALARM", "action : ${intent.action}")

                mSensors = Sensors.getInstance(context)

                if (!mSensors.isShakeSensorOn) {

                    mSensors.reRegister(context.getString(R.string.SHAKE))

                }

                if (!mSensors.isWalkSensorOn) {

                    mSensors.reRegister(context.getString(R.string.WALK))

                }


                vibratePhone(context)
                Toast.makeText(context!!.applicationContext, "Wake UP !!!!!!!!!! Shake your Phone", Toast.LENGTH_LONG).show()

            }

            iSHAKE_COMPLETE -> {
                Log.d("HwAlarmReceiver:onReceive:iSHAKE_COMPLETE", "action : ${intent.action}")

                mSensors.unRegister(context.getString(R.string.SHAKE))

                try {

                    var i = Intent(context, MainActivity::class.java)

                    intent.putExtra("id", context.getString(R.string.SHAKE_COMPLETE))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(i)

                } catch (e: Exception) {
                    Toast.makeText(context, "There was an error ", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }


                var activityIntent = Intent(context, MainActivity::class.java)
                activityIntent.putExtra(context.getString(R.string.SHAKE_COMPLETE), context.getString(R.string.SHAKE_COMPLETE))
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(activityIntent)

            }
        }



        Log.d("HwAlarmReceiver:onReceive", "action not defined")

        mWakeLock.release()

    }

    // setTime : (user-set) hwModeTime - 5 minute
    fun setNotiAlarm(context: Context, setTime: Long) {
        Log.d("HwAlarmReceiver:setNotiAlarm", "setTime : $setTime")
        val mAlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(
            context,
            HwAlarmReceiver::class.java
        ).apply {
            action = context.getString(R.string.NOTIFICATION)
        }

        val pi = PendingIntent.getBroadcast(context, ALARM_NOTI_REQUEST_CODE, i, 0)

        mAlarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            setTime,
            pi
        )
    }

    // setTime : (user-set) hwModeTime - 1 second
    fun setFinalAlarm(context: Context, setTime: Long) {
        Log.d("HwAlarmReceiver:setFinalAlarm", "setTime : $setTime")
        val mAlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(
            context,
            HwAlarmReceiver::class.java
        ).apply {
            action = context.getString(R.string.FINAL_ALARM)
        }
        val pi = PendingIntent.getBroadcast(context, ALARM_FINAL_REQUEST_CODE, i, 0)

        mAlarmManager.setExact(
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

        mAlarmManager.setExact(
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
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    1000,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator.vibrate(1000)
        }
    }

    private fun createNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.HW_NOTIFICATION_CHANNEL_NAME)
            val descriptionText =
                context.getString(R.string.HW_NOTIFICATION_CHANNEL_DESCRIPTION)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                context.getString(R.string.HW_NOTIFICATION_CHANNEL_ID),
                name,
                importance
            ).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}