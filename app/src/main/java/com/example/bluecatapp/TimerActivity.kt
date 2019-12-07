package com.example.bluecatapp

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluecatapp.ui.todo.TodoAdapter
import com.example.bluecatapp.ui.todo.TodoViewModel
import com.example.bluecatapp.util.PrefUtil
import com.example.bluecatapp.util.NotificationUtil
import kotlinx.android.synthetic.main.activity_timer.*
import kotlinx.android.synthetic.main.content_timer.*
import java.util.*

class TimerActivity : AppCompatActivity() {

    companion object {

        // * final alarm
        fun setAlarm(context: Context, nowSeconds: Long, secondsRemaining: Long): Long {
            val wakeUpTime = (nowSeconds + secondsRemaining) * 1000
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TimerExpiredReceiver::class.java)
                .apply { action = HWConstants.ACTION_ALARM_FINAL }
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeUpTime, pendingIntent)
            PrefUtil.setAlarmSetTime(nowSeconds, context)
            return wakeUpTime
        }

        // * final alarm
        fun removeAlarm(context: Context) {
            val intent = Intent(context, TimerExpiredReceiver::class.java)
                .apply { action = HWConstants.ACTION_ALARM_FINAL }
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            PrefUtil.setAlarmSetTime(0, context)
        }

        // * notification alarm ( 3 minutes prior to the final alarm )
        fun setNotificationAlarm(
            context: Context,
            nowSeconds: Long,
            notiAlarmSeconds: Long
        ): Long {
            val wakeUpTime = (nowSeconds + notiAlarmSeconds) * 1000
            Log.d(
                "TimerActivity:setNotificationAlarm",
                "notiAlarmSeconds : ${notiAlarmSeconds}, wakeUpTime : $wakeUpTime"
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TimerExpiredReceiver::class.java)
                .apply { action = HWConstants.ACTION_ALARM_NOTIFICATION }
            val pendingIntent = PendingIntent.getBroadcast(context, 1, intent, 0)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeUpTime, pendingIntent)
            PrefUtil.setAlarmSetTime2(nowSeconds, context)
            return wakeUpTime
        }

        // notification alarm
        fun removeNotificationAlarm(context: Context) {
            val intent = Intent(context, TimerExpiredReceiver::class.java)
                .apply { action = HWConstants.ACTION_ALARM_NOTIFICATION }
            val pendingIntent = PendingIntent.getBroadcast(context, 1, intent, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            PrefUtil.setAlarmSetTime2(0, context)
        }

        val nowSeconds: Long
            get() = Calendar.getInstance().timeInMillis / 1000

        var notiAlarmOffset: Long = 0L  // one minute - notiAlarmSeconds

        // test purpose
        const val notiAlarmSeconds: Long = 4L // 4 seconds
        // production version @todo should be changed when building production version ( + root_preference )
        // const val notiAlarmSeconds : Long = 180L // threeMinutes

        var isShaking = false
        var isWalking = false

    }

    enum class TimerState {
        Stopped, Paused, Running
    }

    //================================== timer ==================================//
    // time : seconds in Long & minute in Int
    private lateinit var timer: CountDownTimer
    private var timerLengthSeconds: Long = 0
    private var timerState = TimerState.Stopped
    private var secondsRemaining: Long = 0

    //================================== Homework mode general ==================================//
    val hwDoneDisplayText = "I have done my homework."
    val hwDoneConfirmText = hwDoneDisplayText.replace("\\s".toRegex(), "").toLowerCase()

    //================================== todos Left ==================================//
    private lateinit var todoViewModel: TodoViewModel
    private lateinit var todoAdapter: TodoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        if (intent.getStringExtra("id") != null) {
            when (intent.getStringExtra("id")) {
                getString(R.string.SHAKE) -> {
                    Log.d("TimerActivity:onCreate", "flag SHAKE intent")
                    Toast.makeText(
                        this,
                        "Wake UP !!!!!!!!!! Shake your Phone!",
                        Toast.LENGTH_SHORT
                    ).show()

                    include_time_counter.visibility = View.GONE
//            include_sensor_counter.visibility = View.VISIBLE

                    isWalking = false
                    tv_wakeUp_walk.visibility = View.GONE
                    tv_wakeUp_shake.visibility = View.VISIBLE

                    isShaking = true
                    resetTimerFuntions()
                }
                getString(R.string.SHAKE_COMPLETE) -> {
                    Log.d("TimerActivity:onCreate", "flag SHAKE Complete")

                    Toast.makeText(
                        this,
                        "Mission Complete. Hope you are Awake!",
                        Toast.LENGTH_SHORT
                    ).show()

                    include_time_counter.visibility = View.VISIBLE
//            include_sensor_counter.visibility = View.GONE

                    tv_wakeUp_shake.visibility = View.GONE
                    isShaking = false
                    resetTimerFuntions()
                }
                getString(R.string.WALK) -> {
                    Log.d("TimerActivity:onCreate", "flag WALK intent")
                    Toast.makeText(
                        this,
                        "Wake UP !!!!!!!!!! Walk Around!",
                        Toast.LENGTH_SHORT
                    ).show()

                    include_time_counter.visibility = View.GONE
//            include_sensor_counter.visibility = View.VISIBLE

                    isShaking = false
                    tv_wakeUp_shake.visibility = View.GONE
                    tv_wakeUp_walk.visibility = View.VISIBLE

                    isWalking = true
                    resetTimerFuntions()
                }
                getString(R.string.WALK_COMPLETE) -> {
                    Log.d("TimerActivity:onCreate", "flag WALK Complete")

                    Toast.makeText(
                        this,
                        "Mission Complete. Hope you are Awake!",
                        Toast.LENGTH_SHORT
                    ).show()

                    include_time_counter.visibility = View.VISIBLE
//            include_sensor_counter.visibility = View.GONE
                    tv_wakeUp_walk.visibility = View.GONE

                    isWalking = false
                    resetTimerFuntions()
                }
                getString(R.string.FROMBLOCK) -> {
                    Log.d("TimerActivity:onCreate", "flag FROMBLOCK")
                    Toast.makeText(
                        this,
                        "Don't use the blocked apps!",
                        Toast.LENGTH_LONG
                    ).show()
                    include_time_counter.visibility = View.VISIBLE
//                    resumeTimerFuntions()
                }
                getString(R.string.FROM_PRENOTI) -> {
                    Log.d("TimerActivity:onCreate", "flag FROM_PRENOTI")
                    Toast.makeText(
                        this,
                        "Welcome Back!",
                        Toast.LENGTH_LONG
                    ).show()
                    resetTimerFuntions()
                }
                getString(R.string.FROM_FINALNOTI) -> {
                    Log.d("TimerActivity:onCreate", "flag FROM_FINALNOTI")
                    Toast.makeText(
                        this,
                        "Welcome Back!",
                        Toast.LENGTH_LONG
                    ).show()
                    resetTimerFuntions()
                }
                getString(R.string.FROM_RUNNINGNOTI) -> {
                    Log.d("TimerActivity:onCreate", "flag FROM_RUNNINGNOTI")
                    Toast.makeText(
                        this,
                        "Welcome Back!",
                        Toast.LENGTH_LONG
                    ).show()
                    include_time_counter.visibility = View.VISIBLE
//                    resumeTimerFuntions()
                }

            }
        } else {
//            if (PrefUtil.getTimerState(this) == TimerState.Running) {
//                include_time_counter.visibility = View.VISIBLE
//                resumeTimerFuntions()
//            }
            resumeTimerFuntions()
        }

        Log.d("TimerActivity:onCreate", "isShaking : $isShaking")
        Log.d("TimerActivity:onCreate", "isWalking : $isWalking")

        todoViewModel =
            ViewModelProviders.of(this).get(TodoViewModel::class.java)

        todoAdapter = TodoAdapter(todoViewModel)
        todoViewModel.getTodoItemsNotDone()
            .observe(this,
                androidx.lifecycle.Observer { t -> todoAdapter.setTodoItems(t!!) })

        timerActivity_todo_recycler_view.apply {
            // set a LinearLayoutManager to handle Android
            // RecyclerView behavior
            layoutManager = LinearLayoutManager(this@TimerActivity)
            // set the custom adapter to the RecyclerView
            adapter = todoAdapter
            setHasFixedSize(true)
        }

        // view model 을 통해 특정 todoItem 에 대한 데이터 변경 실행.
        todoAdapter.onItemClick = { todoItem ->
            //            Log.d("test","todoItem test "+todoItem.id.toString()+"/"+todoItem.done.toString())
            todoViewModel.updateTodoStatus(todoItem)
        }

        if(isShaking){
            include_time_counter.visibility = View.GONE
            tv_wakeUp_shake.visibility = View.VISIBLE
        }else if (isWalking){
            include_time_counter.visibility = View.GONE
            tv_wakeUp_walk.visibility = View.VISIBLE
        }

        fab_start.setOnClickListener { v ->
            startTimer()
            timerState = TimerState.Running
            updateButtons()
        }

        fab_stop.setOnClickListener { v ->
            turnHWModeOff()
        }

    }

    override fun onResume() {
        super.onResume()

        Log.d("TimerActivity:onResume", "isShaking : $isShaking")
        Log.d("TimerActivity:onResume", "isWalking : $isWalking")

        if (intent.getStringExtra("id") != null) {
            when (intent.getStringExtra("id")) {
                getString(R.string.SHAKE) -> {
                    Log.d("TimerActivity:onResume", "flag SHAKE intent")
                    Toast.makeText(
                        this,
                        "Wake UP !!!!!!!!!! Shake your Phone!",
                        Toast.LENGTH_SHORT
                    ).show()
                    include_time_counter.visibility = View.GONE
//            include_sensor_counter.visibility = View.VISIBLE

                    tv_wakeUp_shake.visibility = View.VISIBLE

//                include_time_counter.visibility = View.GONE
////            include_sensor_counter.visibility = View.VISIBLE
//
//                tv_wakeUp_shake.visibility = View.VISIBLE
                    isShaking = true

                    resetTimerFuntions()
                }
                getString(R.string.SHAKE_COMPLETE) -> {
                    Log.d("TimerActivity:onResume", "flag SHAKE Complete")

                    Toast.makeText(
                        this,
                        "Mission Complete. Hope you are Awake!",
                        Toast.LENGTH_SHORT
                    ).show()

                    include_time_counter.visibility = View.VISIBLE
//            include_sensor_counter.visibility = View.GONE

                    tv_wakeUp_shake.visibility = View.GONE
                    isShaking = false

                    resetTimerFuntions()
                }
                getString(R.string.WALK) -> {
                    Log.d("TimerActivity:onResume", "flag WALK intent")
                    Toast.makeText(
                        this,
                        "Wake UP !!!!!!!!!! Walk Around!",
                        Toast.LENGTH_SHORT
                    ).show()

                    include_time_counter.visibility = View.GONE
//            include_sensor_counter.visibility = View.VISIBLE

                    tv_wakeUp_shake.visibility = View.VISIBLE
                    isWalking = true

                    resetTimerFuntions()
                }
                getString(R.string.WALK_COMPLETE) -> {
                    Log.d("TimerActivity:onResume", "flag WALK Complete")

                    Toast.makeText(
                        this,
                        "Mission Complete. Hope you are Awake!",
                        Toast.LENGTH_SHORT
                    ).show()

                    include_time_counter.visibility = View.VISIBLE
//            include_sensor_counter.visibility = View.GONE

                    tv_wakeUp_walk.visibility = View.GONE
                    isWalking = false

                    resetTimerFuntions()
                }
                getString(R.string.FROMBLOCK) -> {
                    Log.d("TimerActivity:onResume", "flag FROMBLOCK")
                    Toast.makeText(
                        this,
                        "Don't use the blocked apps!",
                        Toast.LENGTH_LONG
                    ).show()
                    include_time_counter.visibility = View.VISIBLE
                    resumeTimerFuntions()
                }
                getString(R.string.FROM_PRENOTI) -> {
                    Log.d("TimerActivity:onResume", "flag FROM_PRENOTI")
                    Toast.makeText(
                        this,
                        "Welcome Back!",
                        Toast.LENGTH_LONG
                    ).show()
                    resetTimerFuntions()
                }
                getString(R.string.FROM_FINALNOTI) -> {
                    Log.d("TimerActivity:onResume", "flag FROM_FINALNOTI")
                    Toast.makeText(
                        this,
                        "Welcome Back!",
                        Toast.LENGTH_LONG
                    ).show()
                    resetTimerFuntions()
                }
                getString(R.string.FROM_RUNNINGNOTI) -> {
                    Log.d("TimerActivity:onResume", "flag FROM_RUNNINGNOTI")
                    Toast.makeText(
                        this,
                        "Welcome Back!",
                        Toast.LENGTH_LONG
                    ).show()
                    include_time_counter.visibility = View.VISIBLE
                    resumeTimerFuntions()
                }
            }
        } else {
//            if (PrefUtil.getTimerState(this) == TimerState.Running) {
//                include_time_counter.visibility = View.VISIBLE
//                resumeTimerFuntions()
//            }
            resumeTimerFuntions()
        }

        Log.d(
            "TimerActivity:onResume",
            "notiAlarmOffset : $notiAlarmOffset, notiAlarmSeconds : $notiAlarmSeconds"
        )
    }

    private fun resetTimerFuntions() {
        // === 여기서 state 를 stop 으로 변경해주어야 activity 의 context 에서 제대로 인식한다. === //
        PrefUtil.setTimerState(TimerState.Stopped, this)
        Log.d(
            "TimerActivity:resetTimerFuntions",
            "\ntimerState : $timerState\n" +
                    "PrefUtil.getTimerState(this) : ${PrefUtil.getTimerState(this)}"
        )
        //======= set the alarm time to be 0 before entering initTimer function =======//
        val wakeUpTime = PrefUtil.getAlarmSetTime(this)
        val wakeUpTime2 = PrefUtil.getAlarmSetTime2(this)
        if (wakeUpTime > 0) {
            Log.d(
                "TimerActivity:resetTimerFuntions:removeAlarm",
                "PrefUtil.getAlarmSetTime(this) : $wakeUpTime"
            )
            removeAlarm(this)
        }
        if (wakeUpTime2 > 0) {
            Log.d(
                "TimerActivity:resetTimerFuntions:removeNotificationAlarm",
                "PrefUtil.getAlarmSetTime2(this) : $wakeUpTime2"
            )
            removeNotificationAlarm(this)
        }

        //======= initialize timer after removing alarm =======//
        initTimer()

        Log.d(
            "TimerActivity:resetTimerFuntions",
            "finalAlarmSetTime : ${PrefUtil.getAlarmSetTime(this)} \n" +
                    "preAlarmSetTime : ${PrefUtil.getAlarmSetTime2(this)} \n" +
                    "secondsRemaining: $secondsRemaining \n" +
                    "notiAlarmOffset: $notiAlarmOffset \n" +
                    "notiAlarmSeconds: $notiAlarmSeconds \n"
        )
        NotificationUtil.hideTimerNotification(this)
    }

    private fun resumeTimerFuntions() {
        // resume 하는 순간 notification alarm 은 지워야 한다. ( 화면이 켜진 상태에서는 안 울려야 하니까.)
        removeNotificationAlarm(this)

        initTimer()
        // initTimer 안의 StartTimer 에서 final alarm 을 다시 설정하기 때문에, removeAlarm 을 하면 안된다.
        // removeAlarm(this)

        NotificationUtil.hideTimerNotification(this)
    }

    override fun onPause() {
        super.onPause()

        if (timerState == TimerState.Running) {
            timer.cancel()
            Log.d(
                "TimerActivity:onPause",
                "nowSeconds : $nowSeconds, secondsRemaining : $secondsRemaining , notiAlarmSeconds : $notiAlarmSeconds, notiAlarmOffset : $notiAlarmOffset"
            )

            val wakeUpTime = setAlarm(this, nowSeconds, secondsRemaining)

            // === only set the notification alarm when the enough time is left === //
            if (secondsRemaining > notiAlarmOffset) {
                setNotificationAlarm(this, nowSeconds, (secondsRemaining - notiAlarmOffset))
            }

            NotificationUtil.showTimerRunning(this, wakeUpTime)

        }

        PrefUtil.setPreviousTimerLengthSeconds(timerLengthSeconds, this)
        PrefUtil.setSecondsRemaining(secondsRemaining, this)
        PrefUtil.setTimerState(timerState, this)
    }

    private fun initTimer() {
        timerState = PrefUtil.getTimerState(this)

        //we don't want to change the length of the timer which is already running
        //if the length was changed in settings while it was backgrounded
        if (timerState == TimerState.Stopped) {
            setNewTimerLength()
            notiAlarmOffset = timerLengthSeconds - notiAlarmSeconds // set the notiAlarmOffset right after setting the timerLength
        } else
            setPreviousTimerLength()

        var prevSecRemaining = PrefUtil.getSecondsRemaining(this)
        Log.d(
            "TimerActivity:initTimer",
            "\nprevSecRemaining: $prevSecRemaining\n" +
                    "secondsRemaining : $secondsRemaining \n"
        )
        if (timerState == TimerState.Running) {
            if(secondsRemaining == 0L){
                secondsRemaining = prevSecRemaining
            }
//            else if (secondsRemaining >= prevSecRemaining) {
//                secondsRemaining = prevSecRemaining
//            }
        } else {
            secondsRemaining = timerLengthSeconds
        }

        val alarmSetTime = PrefUtil.getAlarmSetTime(this)
        if (alarmSetTime > 0) {
            if (secondsRemaining >= prevSecRemaining) {
                secondsRemaining -= (nowSeconds - alarmSetTime)
            }
        }

        if (secondsRemaining <= 0)
            onTimerFinished()
        else if (timerState == TimerState.Running)
            startTimer()

        updateButtons()
        updateCountdownUI()

        Log.d(
            "TimerActivity:initTimer",
            "PrefUtil.getAlarmSetTime : ${PrefUtil.getAlarmSetTime(this)}\n" +
                    "PrefUtil.getAlarmSetTime2 : ${PrefUtil.getAlarmSetTime2(this)}\n" +
                    "isShaking : $isShaking \n " +
                    "isWalking : $isWalking \n" +
                    "nowSeconds : $nowSeconds \n" +
                    "secondsRemaining : $secondsRemaining \n" +
                    "notiAlarmSeconds: $notiAlarmSeconds \n" +
                    "notiAlarmOffset : $notiAlarmOffset"
        )
    }

    private fun onTimerFinished() {
        timerState = TimerState.Stopped

        setNewTimerLength()

        progress_countdown.progress = 0

        PrefUtil.setSecondsRemaining(timerLengthSeconds, this)
        secondsRemaining = timerLengthSeconds

        updateButtons()
        updateCountdownUI()

        Log.d(
            "TimerActivity:onTimerFinished",
            "secondsRemaining : $secondsRemaining , notiAlarmSeconds: $notiAlarmSeconds"
        )
    }

    private fun startTimer() {
        timerState = TimerState.Running

//        Toast.makeText(this, "Homework Mode Start Timer", Toast.LENGTH_SHORT).show()

        timer = object : CountDownTimer(secondsRemaining * 1000, 1000) {
            override fun onFinish() = onTimerFinished()

            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000
                updateCountdownUI()
            }
        }.start()

        setAlarm(this, nowSeconds, secondsRemaining)

        Log.d(
            "TimerActivity:startTimer",
            "secondsRemaining : $secondsRemaining , notiAlarmSeconds : $notiAlarmSeconds, notiAlarmOffset : $notiAlarmOffset"
        )

    }

    private fun setNewTimerLength() {
        val lengthInMinutes = PrefUtil.getTimerLength(this)
        timerLengthSeconds = (lengthInMinutes * 60L)

        progress_countdown.max = timerLengthSeconds.toInt()
    }

    private fun setPreviousTimerLength() {
        timerLengthSeconds = PrefUtil.getPreviousTimerLengthSeconds(this)
        progress_countdown.max = timerLengthSeconds.toInt()
    }

    private fun updateCountdownUI() {
        val minutesUntilFinished = secondsRemaining / 60
        val secondsInMinuteUntilFinished = secondsRemaining - minutesUntilFinished * 60
        val secondsStr = secondsInMinuteUntilFinished.toString()
        textView_countdown.text =
            "$minutesUntilFinished:${if (secondsStr.length == 2) secondsStr else "0" + secondsStr}"
        progress_countdown.progress = (timerLengthSeconds - secondsRemaining).toInt()
    }

    private fun updateButtons() {
        if (isShaking || isWalking) { // block all the buttons
            fab_start.isEnabled = false
//            fab_pause.isEnabled = false
            fab_stop.isEnabled = false
        } else {
            when (timerState) {
                TimerState.Running -> {
                    fab_start.isEnabled = false
//                    fab_pause.isEnabled = true
                    fab_stop.isEnabled = true
                }
                TimerState.Stopped -> {
                    fab_start.isEnabled = true
//                    fab_pause.isEnabled = false
                    fab_stop.isEnabled = false
                }
//                TimerState.Paused -> {
//                    fab_start.isEnabled = true
////                    fab_pause.isEnabled = false
//                    fab_stop.isEnabled = true
//                }
            }
        }

    }

    private fun turnHWModeOff() {
        var alertDialog = AlertDialog.Builder(this).create()
        val editText = EditText(this)
        alertDialog.setMessage(
            "Have you finished your homework?\n" +
                    "Type in\n" +
                    "\"" + hwDoneDisplayText + "\""
        )
        alertDialog.setTitle("Turn Off HW Mode")
        alertDialog.setView(editText)
        alertDialog.setButton(
            DialogInterface.BUTTON_POSITIVE,
            "Yes!",
            DialogInterface.OnClickListener { dialog, whichButton ->

                val inputText =
                    editText.text.toString().replace("\\s".toRegex(), "").toLowerCase()

                if (inputText.equals(hwDoneConfirmText)) { // correct input text.

                    // * cancel the timer
                    timer.cancel()
                    onTimerFinished()

                    // * dismiss the dialog

                    alertDialog.dismiss()

                    // * back to TodoList Fragment

                    val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
                    editor.putBoolean(getString(R.string.hw_mode_bool), false)
                    editor.apply()

                    val data = Intent().apply {
                        action = "TIMER_CANCELED"
                    }

                    setResult(Activity.RESULT_OK, data)
                    finish()

                } else {
                    Toast.makeText(
                        this,
                        "Type in the given sentence properly",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        alertDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            "No...",
            DialogInterface.OnClickListener { dialog, whichButton ->
                Toast.makeText(
                    this.applicationContext
                    , "Keep Working!",
                    Toast.LENGTH_SHORT
                ).show()

                alertDialog.dismiss()
            })

        alertDialog.show()
    }

    override fun onBackPressed() {
        // * detect back button press & not using super method to prevent basic back button logic.
        // * if the timer is stopped ( not started yet or stopped after the full cycle. )
        Log.d("TimerActivity:onBackPressed", "timerState : $timerState")
        if (timerState == TimerState.Stopped) {

            if (isShaking || isWalking) {
                Toast.makeText(this, "You Still need to WAKE UP!", Toast.LENGTH_SHORT).show()
            } else {
                val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
                editor.putBoolean(getString(R.string.hw_mode_bool), false)
                editor.apply()

//            super.onBackPressed()
                val data = Intent().apply {
                    action = "NORMAL_BACKBUTTON"
                }

                setResult(Activity.RESULT_OK, data)
                finish()
            }
            // * if the timer is not stopped turning off the hw mode logic
        } else {
            turnHWModeOff()
        }

    }

//    private fun checkPermissions(): Boolean {
////        val permissionState = checkSelfPermission(Manifest.permission.SYSTEM_ALERT_WINDOW)
//        val permissionState2 = checkSelfPermission(Manifest.permission.VIBRATE)
//        return (
////                permissionState == PackageManager.PERMISSION_GRANTED &&
//                permissionState2 == PackageManager.PERMISSION_GRANTED)
//    }
//
//    private fun requestPermissions() {
////        val shouldProvideRationale = shouldShowRequestPermissionRationale(Manifest.permission.SYSTEM_ALERT_WINDOW)
//        val shouldProvideRationale2 =
//            shouldShowRequestPermissionRationale(Manifest.permission.VIBRATE)
//        if (
////            shouldProvideRationale ||
//            shouldProvideRationale2) {
//            Log.d("TimerActivity:requestPermissions", "Displaying permission")
//            //-----------------------permission rationale not yet implemented -------------------------------
//
//        } else {
//            Log.d("TimerActivity:requestPermissions", "Requesting Permission")
//            requestPermissions(
//                arrayOf(
////                Manifest.permission.SYSTEM_ALERT_WINDOW,
//                    Manifest.permission.VIBRATE
//                ), 800
//            )
//        }
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        if (requestCode == 800) {
//
//            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                Log.d(
//                    "TimerActivity:onRequestPermissionsResult:permission_granted",
//                    "grantResults : $${grantResults[0]}"
//                )
//            } else {
//                //-----------------------not yet implemented -------------------------------
//                Log.d(
//                    "TimerActivity:onRequestPermissionsResult:permission_not_granted",
//                    "grantResults : ${grantResults[0]}"
//                )
//            }
//        }
//
//    }

}
