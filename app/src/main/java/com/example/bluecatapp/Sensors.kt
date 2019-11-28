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
            if(!isShakeOn){
                // unregister the sensors as shaking completed
                unRegister("SHAKE")
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
                    "Device has shaken $shakeCount times",
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
                i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_FORWARD_RESULT
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
    //    @todo own version of pedometer
    private lateinit var walkSensorManager: SensorManager
    var isWalkOn = isWalkSensorOn

    private var walkAccel: Float = 0.toFloat()
    private var walkAccelCurrent: Float = 0.toFloat()
    private var walkAccelLast: Float = 0.toFloat()

    private val walkSensorListener = object : SensorEventListener {

        override fun onSensorChanged(se: SensorEvent) {
//            val x = se.values[0]
//            val y = se.values[1]
//            val z = se.values[2]
//            shakeAccelLast = shakeAccelCurrent
//            shakeAccelCurrent = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
//            val delta = shakeAccelCurrent - shakeAccelLast
//            shakeAccel = shakeAccel * 0.9f + delta // perform low-cut filter

        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

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
//        this.walkSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

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

        //        walkSensorManager.registerListener(
//            walkSensorListener,
//            walkSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
//            SensorManager.SENSOR_DELAY_NORMAL
//        );
//
//        walkAccel = 0.00f;
//        walkAccelCurrent = SensorManager.GRAVITY_EARTH;
//        walkAccelLast = SensorManager.GRAVITY_EARTH;

    }

    // * s : "SHAKE" or "WALK"
    fun unRegister(s: String): Int {
        shakeCount = 0
        when (s) {
            "ALL" ->{
                shakeSensorManager.unregisterListener(shakeSensorListener)
                isShakeOn = false
                isShakeSensorOn = false

                walkSensorManager.unregisterListener(walkSensorListener)
                isWalkOn = false
                isWalkSensorOn = false
                Log.d("Sensors:unRegister", "$s Sensor unRegistered")
                return 1
            }
            "SHAKE" -> {
                shakeSensorManager.unregisterListener(shakeSensorListener)
                isShakeOn = false
                isShakeSensorOn = false
                Log.d("Sensors:unRegister", "$s Sensor unRegistered")
                return 1
            }
            "WALK" -> {
                walkSensorManager.unregisterListener(walkSensorListener)
                isWalkOn = false
                isWalkSensorOn = false
                Log.d("Sensors:unRegister", "$s Sensor unRegistered")
                return 1
            }
        }
        Log.d("Sensors:unRegister", "$s Sensor undefined")
        return 0
    }

    // * s : "SHAKE" or "WALK"
    fun reRegister(s: String): Int {
        shakeCount = 0
        when (s) {
            "ALL" ->{
                shakeSensorManager.registerListener(
                    shakeSensorListener,
                    shakeSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                isShakeOn = true
                isShakeSensorOn = true

                walkSensorManager.registerListener(
                    walkSensorListener,
                    walkSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                isWalkOn = true
                isWalkSensorOn = true

                Log.d("Sensors:reRegister", "$s Sensor reRegister")

                return 1
            }
            "SHAKE" -> {
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
                walkSensorManager.registerListener(
                    walkSensorListener,
                    walkSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                isWalkOn = true
                isWalkSensorOn = true
                Log.d("Sensors:reRegister", "$s Sensor reRegister")

                return 1
            }
        }
        Log.d("Sensors:reRegister", "$s Sensor undefined")
        return 0
    }
}