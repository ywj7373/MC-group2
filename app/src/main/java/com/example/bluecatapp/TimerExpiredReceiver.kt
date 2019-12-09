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


        lateinit var mSensors: Sensors

        var sensorIndex = -1
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("TimerExpiredReceiver:onReceive", "intent.action : ${intent.action}")
        when (intent.action) {
            HWConstants.ACTION_ALARM_FINAL -> {

                Sensors.vibratePhone(context)
                NotificationUtil.showTimerExpired(context)
                Toast.makeText(
                    context!!.applicationContext,
                    "Timer Expired!",
                    Toast.LENGTH_LONG
                ).show()

                // [필요없음] 어차피 TimerActivity 내에서 onTimerFinished 에서 timerState stopped 로 지정함.
                // PrefUtil.setTimerState(TimerActivity.TimerState.Stopped, context)

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
                        if(sensorIndex<0){
                            if (pickRandomMode(modeArr, context, false) != 1) {
                                Log.d(
                                    "TimerExpiredReceiver:onReceive:ACTION_ALARM_FINAL",
                                    "[ERROR] PickingRandomMode"
                                )
                            }
                        }


                    } else if (state == 2) { // already exist
                        if (isPedometerOn) {
                            modeArr = arrayOf("SHAKE", "WALK")
                        } else {
                            modeArr = arrayOf("SHAKE")
                        }

                        if(sensorIndex<0){
                            if (pickRandomMode(modeArr, context, false) != 1) {
                                Log.d(
                                    "TimerExpiredReceiver:onReceive:ACTION_ALARM_FINAL",
                                    "[ERROR] PickingRandomMode"
                                )
                            }
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

                // [필요없음] 어차피 알람이 발생한 상황이기 때문에, 알람을 다시 한 번 제거해줄 필요가 없다.
                // TimerActivity.removeNotificationAlarm(context)
            }
        }
    }

    private fun pickRandomMode(arr: Array<String>, context: Context, isReRegister: Boolean): Int {
        if(sensorIndex < 0){ // if none of the sensor is enabled, sensorIndex stays as -1


            // @todo should be set to this in production / demo
             sensorIndex= Random().nextInt(arr.size)

            //test _ only used to force the walk mode on
//            if(arr.size>1){
//                sensorIndex = 1
//            }else{
//                sensorIndex = 0
//            }

            val selectedSensor = arr[sensorIndex]
            Log.d("TimerExpiredReceiver:pickRandomMode","sensorIndex : $sensorIndex, Pick : $selectedSensor ")

            if (selectedSensor == "SHAKE") {
                Log.d(
                    "TimerExpiredReceiver:pickRandomMode",
                    "Picked one mode : SHAKE"
                )
                if(mSensors.isWalkOn){
                    mSensors.isWalkOn = false
                    mSensors.unRegister("WALK",context)
                }

                mSensors.isShakeOn = true
                mSensors.register("SHAKE", context)

                Sensors.vibratePhone(context)
                var i = Intent(context, TimerActivity::class.java)
                i.putExtra("id", context.getString(R.string.SHAKE))
                i.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(i)
                return 1

            } else if (selectedSensor == "WALK") {
                Log.d(
                    "TimerExpiredReceiver:pickRandomMode",
                    "Picked one mode : WALK"
                )
                if(mSensors.isShakeOn){
                    mSensors.isShakeOn = false
                    mSensors.unRegister("SHAKE",context)
                }

                mSensors.isWalkOn = true
                mSensors.register("WALK", context)

                Sensors.vibratePhone(context)
                var i = Intent(context, TimerActivity::class.java)
                i.putExtra("id", context.getString(R.string.WALK))
                i.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(i)

                return 1
            } else {
                return 0
            }
        }else{
            return 1

        }
    }
}