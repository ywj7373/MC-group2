package com.example.bluecatapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager

open class SingletonHolder<out T : Any, in A>(creator: (A) -> T) {
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
}

class Sensors private constructor(context: Context) {

    companion object : SingletonHolder<Sensors, Context>(::Sensors)

    //================================== Common ==================================//

    private var preference: SharedPreferences
    private var editor: SharedPreferences.Editor

    //================================== Motion Sensors ==================================//
    //================================== Detect Shaking ==================================//
    private var shakeSensorManager: SensorManager

    private var shakeAccel: Float = 0.toFloat() // acceleration apart from gravity
    private var shakeAccelCurrent: Float = 0.toFloat() // current acceleration including gravity
    private var shakeAccelLast: Float = 0.toFloat() // last acceleration including gravity

    private var shakeLimit: Int = 0// (at onCreate) set shake count limit from shared preferences
    private var shakeCount: Int = 0

    var isShakeSensorOn: Boolean
        get() = isShakeSensorOn
        set(b) {
            isShakeSensorOn = b
        }

    private val shakeSensorListener = object : SensorEventListener {

        //a sensor reports a new value. In this case, the system invokes the onSensorChanged() method,
        // providing you with a SensorEvent object. A SensorEvent object contains information about the new sensor data,
        // including:
        //new data that the sensor recorded
        //data accuracy
        //the sensor that generated the data
        //the timestamp at which the data was generated
        override fun onSensorChanged(se: SensorEvent) {
            val x = se.values[0]
            val y = se.values[1]
            val z = se.values[2]
            shakeAccelLast = shakeAccelCurrent
            shakeAccelCurrent = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta = shakeAccelCurrent - shakeAccelLast
            shakeAccel = shakeAccel * 0.9f + delta // perform low-cut filter

            Log.d("Sensors:onSensorChanged", "shakeAccel : $shakeAccel")
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
                val toast = Toast.makeText(
                    context,
                    "Mission Complete. Hope you are Awake!",
                    Toast.LENGTH_SHORT
                )
                toast.show()

                // * fire an alarm to notify that shaking has been completed
                val hwAlarmReceiver = HwAlarmReceiver()
                hwAlarmReceiver.setShakeDoneAlarm(context)
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

    private var walkAccel: Float = 0.toFloat()
    private var walkAccelCurrent: Float = 0.toFloat()
    private var walkAccelLast: Float = 0.toFloat()

    var isWalkSensorOn: Boolean
        get() = isWalkSensorOn
        set(b) {
            isWalkSensorOn = b
        }

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

        when (s) {
            "ALL" ->{
                shakeSensorManager.unregisterListener(shakeSensorListener)
                isShakeSensorOn = false
                walkSensorManager.unregisterListener(walkSensorListener)
                isWalkSensorOn = false
                Log.d("Sensors:unRegister", "$s Sensor unRegistered")
                return 1
            }
            "SHAKE" -> {
                shakeSensorManager.unregisterListener(shakeSensorListener)
                isShakeSensorOn = false
                Log.d("Sensors:unRegister", "$s Sensor unRegistered")
                return 1
            }
            "WALK" -> {
                walkSensorManager.unregisterListener(walkSensorListener)
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
        when (s) {
            "ALL" ->{
                shakeSensorManager.registerListener(
                    shakeSensorListener,
                    shakeSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                isShakeSensorOn = true

                walkSensorManager.registerListener(
                    walkSensorListener,
                    walkSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL
                )
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
                isWalkSensorOn = true
                Log.d("Sensors:reRegister", "$s Sensor reRegister")

                return 1
            }
        }
        Log.d("Sensors:reRegister", "$s Sensor undefined")
        return 0
    }
}