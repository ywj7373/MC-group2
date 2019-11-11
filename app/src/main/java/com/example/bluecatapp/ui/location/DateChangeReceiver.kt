package com.example.bluecatapp.ui.location

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.bluecatapp.data.LocationRepository

class DateChangeReceiver : BroadcastReceiver() {
    private val TAG = "DateChangeReceiver"
    override fun onReceive(context: Context?, intent: Intent?) {
        val application = context?.applicationContext as Application
        LocationRepository(application).updateAllNotDone_days()
        Log.d(TAG, "onReceiver called : updateAllNotDone_days() called")
    }
}