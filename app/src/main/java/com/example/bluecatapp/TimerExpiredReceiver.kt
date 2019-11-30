package com.example.bluecatapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager

import com.example.bluecatapp.util.NotificationUtil
import com.example.bluecatapp.util.PrefUtil
import java.util.*

class TimerExpiredReceiver : BroadcastReceiver() {

    companion object {
        fun initiateSensor(context: Context): Int {
            var status: Boolean
            try {
                status = ::mSensors.isInitialized
            } catch (e: Exception) {
                Log.d("TimerExpiredReceiver:initiateSensor", "[ERROR] ${e.message}")
                return 0
            }

            if (status) {
                Log.d("TimerExpiredReceiver:initiateSensor", "already initiated.")
                return 2
            } else {
                mSensors = Sensors.getInstance(context)
                Log.d("TimerExpiredReceiver:initiateSensor", "initiated the sensor.")
                return 1
            }

            return 0
        }

//        fun unRegisterSensors(type:String) : Int{
//            var status : Boolean
//            try{
//                status = ::mSensors.isInitialized
//            }catch (e:Exception){
//                Log.d("TimerExpiredReceiver:initiateSensor", "[ERROR] ${e.message}")
//                return 0
//            }
//
//            if(status){
//                mSensors.unRegister(type)
//                Log.d("TimerExpiredReceiver:initiateSensor", "unregistered $type.")
//                return 1
//            }else{
//                Log.d("TimerExpiredReceiver:initiateSensor", "[ERROR] Sensor not activated.")
//                return 0
//            }
//
//            return 0
//        }

        lateinit var mSensors: Sensors
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("TimerExpiredReceiver:onReceive", "intent.action : ${intent.action}")
        when (intent.action) {
            HWConstants.ACTION_ALARM_FINAL -> {

                NotificationUtil.showTimerExpired(context)

                TimerActivity.isShaking = true
                PrefUtil.setTimerState(TimerActivity.TimerState.Stopped, context)
                PrefUtil.setAlarmSetTime(0, context)

                // * activate sensor and force the user to shake or walk
                var state = initiateSensor(context) // sensor state
                var isPedometerOn = PreferenceManager.getDefaultSharedPreferences(context) // whether pedometer setting is on or off
                    .getBoolean(context.getString(R.string.hw_pedometer_bool), false)
                var modeArr: Array<String>

                Log.d(
                    "TimerExpiredReceiver:onReceive:ACTION_ALARM_FINAL",
                    "isPedometerOn : $isPedometerOn"
                )

                if (state > 0) {
                    if (state == 1) { // new
                        if (isPedometerOn) {
                            modeArr = arrayOf("SHAKE", "WALK")
                        } else {
                            modeArr = arrayOf("SHAKE")
                        }
                        if (pickRandomMode(modeArr, context, false) == 1) {
                            Log.d(
                                "TimerExpiredReceiver:onReceive:ACTION_ALARM_FINAL",
                                "Picked one mode"
                            )
                        } else {
                            Log.d(
                                "TimerExpiredReceiver:onReceive:ACTION_ALARM_FINAL",
                                "[ERROR] PickingRandomMode"
                            )
                        }


                    } else if (state == 2) { // already exist
                        if (isPedometerOn) {
                            modeArr = arrayOf("SHAKE", "WALK")
                        } else {
                            modeArr = arrayOf("SHAKE")
                        }

                        if (pickRandomMode(modeArr, context, true) == 1) {
                            Log.d(
                                "TimerExpiredReceiver:onReceive:ACTION_ALARM_FINAL",
                                "Picked one mode"
                            )
                        } else {
                            Log.d(
                                "TimerExpiredReceiver:onReceive:ACTION_ALARM_FINAL",
                                "[ERROR] PickingRandomMode"
                            )
                        }


                    } else {
                        Log.d(
                            "TimerExpiredReceiver:onReceive:ACTION_ALARM_FINAL",
                            "[ERROR] Initializing"
                        )
                        Toast.makeText(
                            context!!.applicationContext,
                            "Sensor Error",
                            Toast.LENGTH_LONG
                        ).show()
                    }

//                    mSensors.isWalkSensorOn= true
//                    Toast.makeText(context!!.applicationContext, "Wake UP !!!!!!!!!! Start Walking", Toast.LENGTH_LONG).show()
                } else {
                    Log.d(
                        "TimerExpiredReceiver:onReceive:ACTION_ALARM_FINAL",
                        "[ERROR] Initializing"
                    )
                    Toast.makeText(context!!.applicationContext, "Sensor Error", Toast.LENGTH_LONG)
                        .show()
                }

            }

            HWConstants.ACTION_ALARM_NOTIFICATION -> {

                Sensors.vibratePhone(context)
                NotificationUtil.showTimerSoonBeExpired(context)

                Toast.makeText(
                    context!!.applicationContext,
                    "Timer Will be Expired!",
                    Toast.LENGTH_LONG
                ).show()

                TimerActivity.notiAlarmSeconds = 0
                PrefUtil.setAlarmSetTime2(0, context)

            }
        }

    }

    private fun pickRandomMode(arr: Array<String>, context: Context, isReRegister: Boolean): Int {
        val index = Random().nextInt(arr.size)
        Log.d("TimerExpiredReceiver:pickRandomMode","index : $index, Pick : ${arr[index]} ")
        if (arr[index] == "SHAKE") {
            mSensors.isShakeOn = true
            if (isReRegister) {
                mSensors.reRegister("SHAKE", context)
            }
            if(mSensors.isWalkOn){
                mSensors.isWalkOn = false
                mSensors.unRegister("WALK",context)
            }

            Sensors.vibratePhone(context)
            var i = Intent(context, TimerActivity::class.java)
            i.putExtra("id", context.getString(R.string.SHAKE))
            i.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_FORWARD_RESULT
            context.startActivity(i)
            return 1
        } else if (arr[index] == "WALK") {
            mSensors.isWalkOn = true
            if (isReRegister) {
                mSensors.reRegister("WALK", context)
            }
            if(mSensors.isShakeOn){
                mSensors.isShakeOn = false
                mSensors.unRegister("SHAKE",context)
            }
            Sensors.vibratePhone(context)
            var i = Intent(context, TimerActivity::class.java)
            i.putExtra("id", context.getString(R.string.WALK))
            i.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_FORWARD_RESULT
            context.startActivity(i)
            return 1
        } else {
            return 0
        }
    }

}