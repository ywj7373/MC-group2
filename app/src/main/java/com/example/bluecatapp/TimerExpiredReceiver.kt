package com.example.bluecatapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

import com.example.bluecatapp.util.NotificationUtil
import com.example.bluecatapp.util.PrefUtil

class TimerExpiredReceiver : BroadcastReceiver() {

    companion object {
        fun initiateSensor(context: Context) : Int{
            var status : Boolean
            try{
                status = ::mSensors.isInitialized
            }catch (e:Exception){
                Log.d("TimerExpiredReceiver:initiateSensor", "[ERROR] ${e.message}")
                return 0
            }

            if(status){
                Log.d("TimerExpiredReceiver:initiateSensor", "already initiated.")
                return 2
            }else{
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
        Log.d("TimerExpiredReceiver:onReceive","intent.action : ${intent.action}")
        when (intent.action) {
            HWConstants.ACTION_ALARM_FINAL -> {

                NotificationUtil.showTimerExpired(context)

                TimerActivity.isShaking = true
                PrefUtil.setTimerState(TimerActivity.TimerState.Stopped, context)
                PrefUtil.setAlarmSetTime(0, context)


//                mSensors = Sensors.getInstance(context)
//                Log.d("TimerExpiredReceiver:onReceive:ACTION_ALARM_FINAL","isShakeSensorOn : ${mSensors.isShakeSensorOn}")
//
//                if (!mSensors.isShakeSensorOn) {
//                    mSensors.reRegister(context.getString(R.string.SHAKE))
//
//                }
//                Log.d("TimerExpiredReceiver:onReceive:ACTION_ALARM_FINAL","isShakeSensorOn : ${mSensors.isShakeSensorOn}")
//
//                //                if (!mSensors.isWalkSensorOn) {
////
////                    mSensors.reRegister(context.getString(R.string.WALK))
////
////                }



                // * activate sensor and force the user to shake the phone until the shake limit
                var state = initiateSensor(context)
                if(state > 0){
                    if(state == 1){ // new
                        mSensors.isShakeOn= true
                        Sensors.vibratePhone(context)
                        var i = Intent(context, TimerActivity::class.java)

                        i.putExtra("id", context.getString(R.string.SHAKE))
                        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_FORWARD_RESULT
                        context.startActivity(i)

                    }else if(state == 2){ // already exist
                        mSensors.isShakeOn= true
                        mSensors.reRegister("SHAKE")
                        Sensors.vibratePhone(context)
                        var i = Intent(context, TimerActivity::class.java)

                        i.putExtra("id", context.getString(R.string.SHAKE))
                        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(i)

                    }else{
                        Log.d("TimerExpiredReceiver:onReceive:ACTION_ALARM_FINAL","[ERROR] Initializing")
                        Toast.makeText(context!!.applicationContext, "Sensor Error", Toast.LENGTH_LONG).show()
                    }

//                    mSensors.isWalkSensorOn= true
//                    Toast.makeText(context!!.applicationContext, "Wake UP !!!!!!!!!! Start Walking", Toast.LENGTH_LONG).show()
                }else{
                    Log.d("TimerExpiredReceiver:onReceive:ACTION_ALARM_FINAL","[ERROR] Initializing")
                    Toast.makeText(context!!.applicationContext, "Sensor Error", Toast.LENGTH_LONG).show()
                }

            }

            HWConstants.ACTION_ALARM_NOTIFICATION ->{

                Sensors.vibratePhone(context)
                NotificationUtil.showTimerSoonBeExpired(context)

                Toast.makeText(context!!.applicationContext, "Timer Will be Expired!", Toast.LENGTH_LONG).show()

                TimerActivity.notiAlarmSeconds = 0
                PrefUtil.setAlarmSetTime2(0, context)

            }
        }

    }
}