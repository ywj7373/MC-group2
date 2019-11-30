package com.example.bluecatapp

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import android.widget.TextView
import com.google.gson.reflect.TypeToken

open class SingletonHolder<out T : Any, in A>(creator: (A) -> T) {

    fun vibratePhone(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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

    private var creator: ((A) -> T)? = creator
    @Volatile
    private var instance: T? = null

    fun getInstance(arg: A): T {
        val checkInstance = instance
        if (checkInstance != null) {
            return checkInstance
        }

        return synchronized(this) {
            val checkInstanceAgain = instance
            if (checkInstanceAgain != null) {
                checkInstanceAgain
            } else {
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }

    var shakeCount = 0
    var isShakeSensorOn = false
    var isWalkSensorOn = false
    val registerDelay = 10 * 1000
}

class Sensors private constructor(context: Context) {

    companion object : SingletonHolder<Sensors, Context>(::Sensors)

    //================================== Common ==================================//

    private var preference: SharedPreferences
    private var editor: SharedPreferences.Editor

    //================================== Motion Sensors ==================================//
    //================================== Detect Shaking ==================================//
    private var shakeSensorManager: SensorManager
    var isShakeOn = isShakeSensorOn

    private var shakeAccel: Float = 0.toFloat() // acceleration apart from gravity
    private var shakeAccelCurrent: Float = 0.toFloat() // current acceleration including gravity
    private var shakeAccelLast: Float = 0.toFloat() // last acceleration including gravity

    private var shakeLimit: Int = 0// (at onCreate) set shake count limit from shared preferences

    private val shakeSensorListener = object : SensorEventListener {

        //a sensor reports a new value. In this case, the system invokes the onSensorChanged() method,
        // providing you with a SensorEvent object. A SensorEvent object contains information about the new sensor data,
        // including:
        //new data that the sensor recorded
        //data accuracy
        //the sensor that generated the data
        //the timestamp at which the data was generated
        override fun onSensorChanged(se: SensorEvent) {
            if (!isShakeOn) {
                // unregister the sensors as shaking completed
                unRegister("SHAKE", context)
                return
            }

            val x = se.values[0]
            val y = se.values[1]
            val z = se.values[2]
            shakeAccelLast = shakeAccelCurrent
            shakeAccelCurrent = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta = shakeAccelCurrent - shakeAccelLast
            shakeAccel = shakeAccel * 0.9f + delta // perform low-cut filter

            Log.d("Sensors:onSensorChanged", "shakeAccel : $shakeAccel")

            vibratePhone(context)
            if (shakeAccel > 6) {
                shakeCount++
                val toast = Toast.makeText(
                    context,
                    "You have shaken $shakeCount times.\n${shakeLimit-shakeCount} times left",
                    Toast.LENGTH_SHORT
                )
                toast.show()
            }
            if (shakeCount >= shakeLimit) {
                shakeCount = 0

                isShakeOn = false
                isShakeSensorOn = false

                var i = Intent(context, TimerActivity::class.java)

                i.putExtra("id", context.getString(R.string.SHAKE_COMPLETE))
                i.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_FORWARD_RESULT
                context.startActivity(i)
            }


        }

        //a sensor’s accuracy changes. In this case, the system invokes the onAccuracyChanged() method,
        // providing you with a reference to the Sensor object, which has changed, and the new accuracy of the sensor.
        // Accuracy is represented by one of four status constants:
        //SENSOR_STATUS_ACCURACY_LOW,
        //SENSOR_STATUS_ACCURACY_MEDIUM,
        //SENSOR_STATUS_ACCURACY_HIGH,
        //SENSOR_STATUS_UNRELIABLE.

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    //================================== Detect Walking ==================================//
    //Pedometer variables
    private var pedometer: Pedometer
    private var isPedometerEnabled: Boolean = false
    private var walkSensorManager: SensorManager
    private var hwStepCounter: Int = 0
    private var walkLimit: Int = 10
    var isWalkOn = isWalkSensorOn

    init {

        Log.d("Sensors:init", "sensor init")
        preference = PreferenceManager.getDefaultSharedPreferences(context)
        editor = preference.edit()

        //================================== Motion Sensors ==================================//
        //================================== Detect Shaking ==================================//

        shakeCount = 0
        shakeLimit = preference.getInt(context.getString(R.string.hw_shake_value), 30)


        Log.d("Sensors:init:shakeLimit", "value : $shakeLimit")

        this.shakeSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        shakeAccel = 0.00f;
        shakeAccelCurrent = SensorManager.GRAVITY_EARTH;
        shakeAccelLast = SensorManager.GRAVITY_EARTH;

        shakeSensorManager.registerListener(
            shakeSensorListener,
            shakeSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
        isShakeOn = true

        //================================== Detect Walking ==================================//
        hwStepCounter = 0
        isPedometerEnabled =
            preference.getBoolean(context.getString(R.string.hw_pedometer_bool), false)
        walkLimit = preference.getInt(context.getString(R.string.hw_pedometer_value), 10)
        walkSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pedometer = object : Pedometer() {
            override fun onSensorChanged(event: SensorEvent?) {

                if (!isWalkOn) {
                    // unregister the sensors as shaking completed
                    unRegister("WALK", context)
                    return
                }
                if (hwStepCounter < walkLimit) {
                    super.onSensorChanged(event)
                    val toast = Toast.makeText(
                        context,
                        "You have walked $hwStepCounter steps.\n${walkLimit-hwStepCounter} steps left.",
                        Toast.LENGTH_SHORT
                    )
                    toast.show()
                } else {

                    isWalkOn = false
                    isWalkSensorOn = false

                    var i = Intent(context, TimerActivity::class.java)

                    i.putExtra("id", context.getString(R.string.WALK_COMPLETE))
                    i.flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_FORWARD_RESULT
                    context.startActivity(i)
                }
            }

            override fun step(timeNs: Long) {
                super.step(timeNs)
                hwStepCounter += super.numSteps
                with(preference.edit()) {
                    // update changed values
                    putInt(
                        "hwStepCounters",
                        hwStepCounter
                    )
                    commit()
                }
            }
        }

        Log.d("Sensors:init:walkLimit", "value : $walkLimit")
        Log.d("Sensors:init:isPedometerEnabled", "value : $isPedometerEnabled")

        if (isPedometerEnabled) {
            Log.d("Sensors:init:pedometerOn", "isPedometerEnabled : $isPedometerEnabled")
            isWalkOn = true
            togglePedometer(context, true)
        }

    }

    // * s : "SHAKE" or "WALK"
    fun unRegister(s: String, context: Context): Int {
        when (s) {
            "ALL" -> {
                shakeCount = 0
                hwStepCounter = 0
                isShakeOn = false
                isShakeSensorOn = false
                shakeSensorManager.unregisterListener(shakeSensorListener)

                isWalkOn = false
                isWalkSensorOn = false
                togglePedometer(context, false)

                Log.d("Sensors:unRegister", "$s Sensor unRegistered")
                return 1
            }
            "SHAKE" -> {
                shakeCount = 0
                isShakeOn = false
                isShakeSensorOn = false
                shakeSensorManager.unregisterListener(shakeSensorListener)

                Log.d("Sensors:unRegister", "$s Sensor unRegistered")
                return 1
            }
            "WALK" -> {
                hwStepCounter = 0
                isWalkOn = false
                isWalkSensorOn = false
                togglePedometer(context, false)

                Log.d("Sensors:unRegister", "$s Sensor unRegistered")
                return 1
            }
        }
        Log.d("Sensors:unRegister", "$s Sensor undefined")
        return 0
    }

    // * s : "SHAKE" or "WALK"
    fun reRegister(s: String, context: Context): Int {
        shakeCount = 0
        when (s) {
            "ALL" -> {
                shakeCount = 0
                hwStepCounter = 0
                shakeSensorManager.registerListener(
                    shakeSensorListener,
                    shakeSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                isShakeOn = true
                isShakeSensorOn = true

                togglePedometer(context, true)
                isWalkOn = true
                isWalkSensorOn = true

                Log.d("Sensors:reRegister", "$s Sensor reRegister")

                return 1
            }
            "SHAKE" -> {
                shakeCount = 0
                shakeSensorManager.registerListener(
                    shakeSensorListener,
                    shakeSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                isShakeOn = true
                isShakeSensorOn = true
                Log.d("Sensors:reRegister", "$s Sensor reRegister")

                return 1
            }
            "WALK" -> {
                hwStepCounter = 0
                togglePedometer(context, true)
                isWalkOn = true
                isWalkSensorOn = true
                Log.d("Sensors:reRegister", "$s Sensor reRegister")

                return 1
            }
        }
        Log.d("Sensors:reRegister", "$s Sensor undefined")
        return 0
    }

    private fun togglePedometer(context: Context, makeOn: Boolean) {
        Log.d("Sensors:togglePedometer", "makeOn : $makeOn")
        if (makeOn) { // on
            walkSensorManager.registerListener(
                pedometer, walkSensorManager
                    .getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL
            )
        } else { // off
            walkSensorManager.unregisterListener(pedometer)
        }
    }
}