/**
 * Pedometer source code
 * Author: Govindz
 * Date: 2018
 * Availability: https://github.com/AndroidCodility/Pedometer
 * */

package com.example.bluecatapp

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log

open class Pedometer : SensorEventListener,
    StepListener {
    private var stepDetector: StepDetector =
        StepDetector()
    var numSteps: Int = 0

    init {
        stepDetector!!.registerListener(this)
    }
/*
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pedometer)

        stepCount = findViewById(R.id.stepCount)
        startPedometer = findViewById(R.id.pedometer_start)
        resetPedometer = findViewById(R.id.pedometer_stop)
        pausePedometer = findViewById(R.id.pedometer_pause)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        simpleStepDetector = StepDetector()
        simpleStepDetector!!.registerListener(this)

        numSteps = 0
        stepCount.setText("0")

        startPedometer.setOnClickListener(View.OnClickListener {
            sensorManager!!.registerListener(this, sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST)
        })

        pausePedometer.setOnClickListener(View.OnClickListener {
            sensorManager!!.unregisterListener(this)
        })

        //reinitialize step count when stopped
        resetPedometer.setOnClickListener(View.OnClickListener {
            numSteps = 0
            stepCount.setText("0")
        })
    }
*/
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            stepDetector!!.updateAccelerometer(event.timestamp, event.values[0], event.values[1], event.values[2])
        }
    }

    override fun step(timeNs: Long) {
        numSteps++
        Log.d("Pedometer", "Step count: $numSteps")
    }
}