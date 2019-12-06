/**
 * Pedometer source code
 * Author: Govindz
 * Date: 2018
 * Availability: https://github.com/AndroidCodility/Pedometer
 * */

package com.example.bluecatapp.pedometer

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType

open class Pedometer : SensorEventListener,
    StepListener {
    private var stepDetector: StepDetector =
        StepDetector()

    // Initialize starting point to count steps
    var numSteps: Int = 0

    init {
        stepDetector!!.registerListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("Pedometer", "Accuracy changed")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            stepDetector!!.updateAccelerometer(event.timestamp, event.values[0], event.values[1], event.values[2])
        }
    }

    override fun step(timeNs: Long) {
        numSteps ++
        Log.d("Pedometer", "Step count: $numSteps")
    }
}