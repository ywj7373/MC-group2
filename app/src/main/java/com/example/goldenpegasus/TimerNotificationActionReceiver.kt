package com.example.goldenpegasus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.goldenpegasus.util.NotificationUtil
import com.example.goldenpegasus.util.PrefUtil

class TimerNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action){

            HWConstants.ACTION_STOP -> {
                TimerActivity.removeAlarm(context)
                TimerActivity.removeNotificationAlarm(context)

                PrefUtil.setTimerState(TimerActivity.TimerState.Stopped, context)

                NotificationUtil.hideTimerNotification(context)
            }

            // obsolete
//            HWConstants.ACTION_PAUSE -> {
//                var secondsRemaining = PrefUtil.getSecondsRemaining(context)
//                val alarmSetTime = PrefUtil.getAlarmSetTime(context)
//                val nowSeconds = TimerActivity.nowSeconds
//
//                secondsRemaining -= nowSeconds - alarmSetTime
//                PrefUtil.setSecondsRemaining(secondsRemaining, context)
//
//                TimerActivity.removeAlarm(context)
//                TimerActivity.removeNotificationAlarm(context)
//
//                PrefUtil.setTimerState(TimerActivity.TimerState.Paused, context)
//                NotificationUtil.showTimerPaused(context)
//            }

            // obsolete
//            HWConstants.ACTION_RESUME -> {
//                val secondsRemaining = PrefUtil.getSecondsRemaining(context)
//                val notiAlarmSeconds = TimerActivity.notiAlarmSeconds
//
//                val wakeUpTime = TimerActivity.setAlarm(context, TimerActivity.nowSeconds, secondsRemaining)
//                TimerActivity.setNotificationAlarm(context, TimerActivity.nowSeconds, notiAlarmSeconds )
//
//                PrefUtil.setTimerState(TimerActivity.TimerState.Running, context)
//                NotificationUtil.showTimerRunning(context, wakeUpTime)
//            }
//
            HWConstants.ACTION_START -> {
                val minutesRemaining = PrefUtil.getTimerLength(context)
                val secondsRemaining = minutesRemaining * 60L
                val notiAlarmOffset = TimerActivity.notiAlarmOffset

                //final Alarm
                val wakeUpTime = TimerActivity.setAlarm(context, TimerActivity.nowSeconds, secondsRemaining)
                //notification Alarm
                val wakeUpTime2 = TimerActivity.setNotificationAlarm(context, TimerActivity.nowSeconds, notiAlarmOffset)

                PrefUtil.setTimerState(TimerActivity.TimerState.Running, context)
                PrefUtil.setSecondsRemaining(secondsRemaining, context)

                Log.d("TimerNotificationActionReceiver:OnReceive:ACTION_START",
                    "wakeUpTime : $wakeUpTime, preWakeUpTime : $wakeUpTime2, secondsRemaining : $secondsRemaining")
                NotificationUtil.showTimerRunning(context, wakeUpTime)
            }
        }
    }
}
