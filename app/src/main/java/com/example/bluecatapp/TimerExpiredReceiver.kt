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

        PrefUtil.setTimerState(TodoFragment.TimerState.Stopped, context)
        PrefUtil.setAlarmSetTime(0, context)
    }
}