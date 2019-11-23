package com.example.bluecatapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.bluecatapp.ui.todo.TodoFragment

import com.example.bluecatapp.util.NotificationUtil
import com.example.bluecatapp.util.PrefUtil

class TimerExpiredReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NotificationUtil.showTimerExpired(context)

        PrefUtil.setTimerState(TimerActivity.TimerState.Stopped, context)
        PrefUtil.setAlarmSetTime(0, context)

        // * @todo activate sensor and force the user to shake the phone until the shake limit
    }
}