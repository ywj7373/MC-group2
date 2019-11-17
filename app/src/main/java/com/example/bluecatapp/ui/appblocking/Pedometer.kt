package com.example.bluecatapp.ui.appblocking

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.Toast

class Pedometer(mContext: Context): SensorEventListener {

    private var sensorManager : SensorManager?= null
    private var pedometer : Sensor ?= null
    private var sharedPreferences: SharedPreferences ?= null
    private var totalSteps : Int = 10
    private var mContext = mContext

    init {
        displayToast("PEDOMETER CLASS CREATED")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val stepCount = event!!.values[0]
        displayToast("YOU MOVED: $stepCount")
    }

    private fun displayToast(message: String){
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT)
    }
}