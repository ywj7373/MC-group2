package com.example.goldenpegasus.util

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.goldenpegasus.*
import java.text.SimpleDateFormat
import java.util.*
import android.os.PowerManager


class NotificationUtil {
    companion object {
        private const val CHANNEL_ID_TIMER = "HW_TIMER_ID"
        private const val CHANNEL_NAME_TIMER = "HW_TIMER"
        private const val TIMER_ID = 0

        fun showTimerExpired(context: Context) {

            Log.d("NotificationUtil:showTimerExpired", "showTimerExpired")

            // ==== [obsolete] Timer Activity 내 resetTimerFuntions 에서 똑같은 작업 수행함.
            // PrefUtil.setTimerState(TimerActivity.TimerState.Stopped,context)

            //================= Build Notification =================//
            val nBuilder = getBasicNotificationBuilder(context, CHANNEL_ID_TIMER, true)
            nBuilder.setContentTitle("Timer Expired!")
                .setContentText("Tap the alarm to restart")
                .setContentIntent(
                    getPendingIntentWithStack(
                        context,
                        MainActivity::class.java,
//                        true,
                    context.getString(R.string.FROM_FINALNOTI)
                    )
                )
                .setOngoing(true)

            //================= Send Notification =================//
            val nManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nManager.createNotificationChannel(CHANNEL_ID_TIMER, CHANNEL_NAME_TIMER, true)
            nManager.notify(TIMER_ID, nBuilder.build())

            //================= Wake Up =================//
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,"NotificationUtil:showTimerExpired")
            wl.acquire(15000)
        }

        fun showTimerSoonBeExpired(context: Context) {
            Log.d("NotificationUtil:showTimerSoonBeExpired", "showTimerSoonBeExpired")

            //================= Build Notification =================//
            val nBuilder = getBasicNotificationBuilder(context, CHANNEL_ID_TIMER, true)
            nBuilder.setContentTitle("Timer Will be Expired!")
                .setContentText("Tap the alarm to restart")
                .setContentIntent(
                    getPendingIntentWithStack(
                        context,
                        TimerActivity::class.java,
//                        false
                        context.getString(R.string.FROM_PRENOTI)
                    )
                )
                .setOngoing(true)

            //================= Send Notification =================//
            val nManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nManager.createNotificationChannel(CHANNEL_ID_TIMER, CHANNEL_NAME_TIMER, true)

            //================= Wake Up =================//
            nManager.notify(TIMER_ID, nBuilder.build())
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,"NotificationUtil:showTimerSoonBeExpired")
            wl.acquire(15000)
        }

        fun showTimerRunning(context: Context, wakeUpTime: Long) {
            Log.d("NotificationUtil:showTimerRunning", "showTimerRunning")

            //================= Build Notification =================//
            val df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT)
            val nBuilder = getBasicNotificationBuilder(context, CHANNEL_ID_TIMER, true)
            nBuilder.setContentTitle("Timer is Running.")
                .setContentText("Ends at : ${df.format(Date(wakeUpTime))}")
                .setContentIntent(
                    getPendingIntentWithStack(
                        context,
                        TimerActivity::class.java,
//                        false,
                    context.getString(R.string.FROM_RUNNINGNOTI)
                    )
                )
                .setOngoing(true)

            //================= Send Notification =================//
            val nManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nManager.createNotificationChannel(CHANNEL_ID_TIMER, CHANNEL_NAME_TIMER, true)
            nManager.notify(TIMER_ID, nBuilder.build())
        }

        //[obsolete] no Pause State
//        fun showTimerPaused(context: Context) {
//            val resumeIntent = Intent(context, TimerNotificationActionReceiver::class.java)
//            resumeIntent.action = HWConstants.ACTION_RESUME
//            val resumePendingIntent = PendingIntent.getBroadcast(
//                context,
//                0, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT
//            )
//
//            val nBuilder = getBasicNotificationBuilder(context, CHANNEL_ID_TIMER, true)
//            nBuilder.setContentTitle("Timer is paused.")
//                .setContentText("Resume?")
//                .setContentIntent(
//                    getPendingIntentWithStack(
//                        context,
//                        TimerActivity::class.java,
////                        false,
//                    "null"
//                    )
//                )
//                .setOngoing(true)
//                .addAction(R.drawable.ic_start, "Resume", resumePendingIntent)
//
//            val nManager =
//                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            nManager.createNotificationChannel(CHANNEL_ID_TIMER, CHANNEL_NAME_TIMER, true)
//
//            nManager.notify(TIMER_ID, nBuilder.build())
//        }

        fun hideTimerNotification(context: Context) {
            val nManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nManager.cancel(TIMER_ID)
        }

        private fun getBasicNotificationBuilder(
            context: Context,
            channelId: String,
            playSound: Boolean
        )
                : NotificationCompat.Builder {
            val notificationSound: Uri =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val nBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(true)
                .setDefaults(0)
            if (playSound) nBuilder.setSound(notificationSound)
            return nBuilder
        }

        private fun <T> getPendingIntentWithStack(
            context: Context,
            javaClass: Class<T>,
            id: String
        ): PendingIntent {

            val stackBuilder = TaskStackBuilder.create(context)
            stackBuilder.addParentStack(javaClass)

            // set the intent id to notify the timerActivity
            val resultIntent = Intent(context, javaClass)
            resultIntent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            resultIntent.putExtra("id",id)
            stackBuilder.addNextIntent(resultIntent)
            return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        @TargetApi(26)
        private fun NotificationManager.createNotificationChannel(
            channelID: String,
            channelName: String,
            playSound: Boolean
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelImportance = if (playSound) NotificationManager.IMPORTANCE_DEFAULT
                else NotificationManager.IMPORTANCE_LOW
                val nChannel = NotificationChannel(channelID, channelName, channelImportance)
                nChannel.enableLights(true)
                nChannel.lightColor = Color.BLUE
                this.createNotificationChannel(nChannel)
            }
        }
    }
}