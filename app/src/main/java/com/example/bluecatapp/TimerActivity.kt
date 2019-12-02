package com.example.bluecatapp

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluecatapp.data.TodoItem
import com.example.bluecatapp.ui.todo.TodoAdapter
import com.example.bluecatapp.ui.todo.TodoFragment
import com.example.bluecatapp.ui.todo.TodoViewModel
import com.example.bluecatapp.util.PrefUtil
import com.example.bluecatapp.util.NotificationUtil
import kotlinx.android.synthetic.main.activity_timer.*
import kotlinx.android.synthetic.main.content_timer.*
import kotlinx.android.synthetic.main.fragment_todo.*
import java.util.*


class TimerActivity : AppCompatActivity() {

    companion object {

        // * final alarm
        fun setAlarm(context: Context, nowSeconds: Long, secondsRemaining: Long): Long {

            val wakeUpTime = (
                    nowSeconds +
                            secondsRemaining
                    ) * 1000
            Log.d(
                "TimerActivity:setAlarm",
                "secondsRemaining : $secondsRemaining, wakeUpTime : $wakeUpTime"
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TimerExpiredReceiver::class.java)
                .apply { action = HWConstants.ACTION_ALARM_FINAL }
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeUpTime, pendingIntent)
            PrefUtil.setAlarmSetTime(nowSeconds, context)
            return wakeUpTime
        }

        // * notification alarm ( 3 minutes prior to the final alarm )
        fun setNotificationAlarm(
            context: Context,
            nowSeconds: Long,
            notiAlarmSeconds: Long
        ): Long {
            val wakeUpTime = (
                    nowSeconds +
                            notiAlarmSeconds
                    ) * 1000
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

        // * final alarm
        fun removeAlarm(context: Context) {
            val intent = Intent(context, TimerExpiredReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            PrefUtil.setAlarmSetTime(0, context)
        }

        // notification alarm
        fun removeNotificationAlarm(context: Context) {
            val intent = Intent(context, TimerExpiredReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 1, intent, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            PrefUtil.setAlarmSetTime2(0, context)
        }

        val nowSeconds: Long
            get() = Calendar.getInstance().timeInMillis / 1000

        //        const val threeMinutes =  (60 * 3)
        const val threeMinutes = (56) // testOffset
        var notiAlarmSeconds: Long = 0

        var isShaking = false
        var isWalking = false

    }

    enum class TimerState {
        Stopped, Paused, Running
    }

    //    private lateinit var preference : SharedPreferences
//    private var shakeLimit = 0
    //================================== Homework mode general ==================================//
    val hwDoneDisplayText = "I have done my homework."
    val hwDoneConfirmText = hwDoneDisplayText.replace("\\s".toRegex(), "").toLowerCase()

    //================================== todos Left ==================================//
    private lateinit var todoViewModel: TodoViewModel
    private val todoAdapter = TodoAdapter()

    private lateinit var timer: CountDownTimer
    private var timerLengthSeconds: Long = 0
    private var timerState = TimerState.Stopped
    private var secondsRemaining: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        var shouldCheckPerm = !checkPermissions()
        Log.d("TimerActivity:onCreate:checkPermissions","shouldCheckPerm : $shouldCheckPerm")
        if(shouldCheckPerm){
            requestPermissions()
        }

        when(intent.getStringExtra("id")){
            getString(R.string.SHAKE)->{
                Log.d("TimerActivity:onCreate", "flag SHAKE intent")
                Toast.makeText(
                    this,
                    "Wake UP !!!!!!!!!! Shake your Phone!",
                    Toast.LENGTH_SHORT
                ).show()

                include_time_counter.visibility = View.GONE
//            include_sensor_counter.visibility = View.VISIBLE

                tv_wakeUp_shake.visibility = View.VISIBLE
                isShaking = true
            }
            getString(R.string.SHAKE_COMPLETE)->{
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
            }
            getString(R.string.WALK)->{
                Log.d("TimerActivity:onCreate", "flag WALK intent")
                Toast.makeText(
                    this,
                    "Wake UP !!!!!!!!!! Walk Around!",
                    Toast.LENGTH_SHORT
                ).show()

                include_time_counter.visibility = View.GONE
//            include_sensor_counter.visibility = View.VISIBLE

                tv_wakeUp_walk.visibility = View.VISIBLE
                isWalking = true
            }
            getString(R.string.WALK_COMPLETE)->{
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
            }
            getString(R.string.FROMBLOCK) ->{

            }
        }


//        preference = PreferenceManager.getDefaultSharedPreferences(this)
//        shakeLimit = preference.getInt(getString(R.string.hw_shake_value),30)

        Log.d("TimerActivity:onCreate", "isShaking : $isShaking")
        Log.d("TimerActivity:onCreate", "isWalking : $isWalking")

        todoViewModel =
            ViewModelProviders.of(this).get(TodoViewModel::class.java)

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

        fab_start.setOnClickListener { v ->
            startTimer()
            timerState = TimerState.Running
            updateButtons()
        }

//        fab_pause.setOnClickListener { v ->
//            timer.cancel()
//            timerState = TimerState.Paused
//            updateButtons()
//        }

        fab_stop.setOnClickListener { v ->
            turnHWModeOff()
        }

        notiAlarmSeconds = PrefUtil.getTimerLength(this) * 60L - threeMinutes
    }

    override fun onResume() {
        super.onResume()

        Log.d("TimerActivity:onResume", "isShaking : $isShaking")
        Log.d("TimerActivity:onResume", "isWalking : $isWalking")

        when(intent.getStringExtra("id")){
            getString(R.string.SHAKE)->{
                Log.d("TimerActivity:onCreate", "flag SHAKE intent")
                Toast.makeText(
                    this,
                    "Wake UP !!!!!!!!!! Shake your Phone!",
                    Toast.LENGTH_SHORT
                ).show()

//                include_time_counter.visibility = View.GONE
////            include_sensor_counter.visibility = View.VISIBLE
//
//                tv_wakeUp_shake.visibility = View.VISIBLE
                isShaking = true
            }
            getString(R.string.SHAKE_COMPLETE)->{
                Log.d("TimerActivity:onCreate", "flag SHAKE Complete")

                Toast.makeText(
                    this,
                    "Mission Complete. Hope you are Awake!",
                    Toast.LENGTH_SHORT
                ).show()

//                include_time_counter.visibility = View.VISIBLE
////            include_sensor_counter.visibility = View.GONE
//
//                tv_wakeUp_shake.visibility = View.GONE
                isShaking = false
            }
            getString(R.string.WALK)->{
                Log.d("TimerActivity:onCreate", "flag WALK intent")
                Toast.makeText(
                    this,
                    "Wake UP !!!!!!!!!! Walk Around!",
                    Toast.LENGTH_SHORT
                ).show()

//                include_time_counter.visibility = View.GONE
////            include_sensor_counter.visibility = View.VISIBLE
//
//                tv_wakeUp_shake.visibility = View.VISIBLE
                isWalking = true
            }
            getString(R.string.WALK_COMPLETE)->{
                Log.d("TimerActivity:onCreate", "flag WALK Complete")

                Toast.makeText(
                    this,
                    "Mission Complete. Hope you are Awake!",
                    Toast.LENGTH_SHORT
                ).show()

//                include_time_counter.visibility = View.VISIBLE
////            include_sensor_counter.visibility = View.GONE
//
//                tv_wakeUp_walk.visibility = View.GONE
                isWalking = false
            }
        }
        initTimer()
        removeAlarm(this)
        removeNotificationAlarm(this)
        NotificationUtil.hideTimerNotification(this)
//        }

    }

    override fun onPause() {
        super.onPause()

        if (timerState == TimerState.Running) {
            timer.cancel()
            Log.d(
                "TimerActivity:onPause",
                "nowSeconds : $nowSeconds, secondsRemaining : $secondsRemaining , notiAlarmSeconds : $notiAlarmSeconds"
            )

            if (secondsRemaining > notiAlarmSeconds) {
                //* final alarm
                val wakeUpTime = setAlarm(this, nowSeconds, secondsRemaining)
                //* notification alarm
                val wakeUpTime2 = setNotificationAlarm(this, nowSeconds, notiAlarmSeconds)

                Log.d(
                    "TimerActivity:onPause",
                    "wakeUpTime : $wakeUpTime, wakeUpTime2 : $wakeUpTime2"
                )
                NotificationUtil.showTimerRunning(this, wakeUpTime)
            }

        } else if (timerState == TimerState.Paused) {
            NotificationUtil.showTimerPaused(this)
        }

        PrefUtil.setPreviousTimerLengthSeconds(timerLengthSeconds, this)
        PrefUtil.setSecondsRemaining(secondsRemaining, this)
        PrefUtil.setTimerState(timerState, this)
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

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TimerActivity:onDestroy", "TimerActivity:onDestroy")
    }

    private fun initTimer() {
//        isShaking = false
        timerState = PrefUtil.getTimerState(this)

        //we don't want to change the length of the timer which is already running
        //if the length was changed in settings while it was backgrounded
        if (timerState == TimerState.Stopped)
            setNewTimerLength()
        else
            setPreviousTimerLength()

        Log.d(
            "TimerActivity:initTimer",
            "isShaking : $isShaking, " +
                    "isWalking : $isWalking, " +
                    "secondsRemaining : $secondsRemaining , notiAlarmSeconds : $notiAlarmSeconds"
        )

        val alarmSetTime = PrefUtil.getAlarmSetTime(this)

        if (alarmSetTime > 0)
            secondsRemaining -= nowSeconds - alarmSetTime

        val alarmSetTime2 = PrefUtil.getAlarmSetTime2(this)
        if (alarmSetTime2 > 0) {
            if ((secondsRemaining - threeMinutes) > notiAlarmSeconds) {
                notiAlarmSeconds = secondsRemaining - threeMinutes
            } else {
                notiAlarmSeconds = 0
            }
        } else {
            notiAlarmSeconds = 0
        }

        Log.d(
            "TimerActivity:initTimer",
            "PrefUtil.getAlarmSetTime2 : ${PrefUtil.getAlarmSetTime2(this)}," +
                    "nowSeconds : $nowSeconds" +
                    "notiAlarmSeconds : $notiAlarmSeconds"
        )

        if (secondsRemaining <= 0)
            onTimerFinished()
        else if (timerState == TimerState.Running)
            startTimer()

        Log.d(
            "TimerActivity:initTimer",
            "PrefUtil.getAlarmSetTime : ${PrefUtil.getAlarmSetTime(this)}," +
                    "secondsRemaining : $secondsRemaining"
        )

        updateButtons()
        updateCountdownUI()
    }

    private fun onTimerFinished() {
        timerState = TimerState.Stopped

        //set the length of the timer to be the one set in SettingsActivity
        //if the length was changed when the timer was running
        setNewTimerLength()

        progress_countdown.progress = 0

        Log.d(
            "TimerActivity:onTimerFinished",
            "secondsRemaining : $secondsRemaining , notiAlarmSeconds: $notiAlarmSeconds"
        )

        PrefUtil.setSecondsRemaining(timerLengthSeconds, this)
        secondsRemaining = timerLengthSeconds

        updateButtons()
        updateCountdownUI()
    }

    private fun startTimer() {
        timerState = TimerState.Running

        Toast.makeText(this, "Homework mode Start.", Toast.LENGTH_SHORT).show()
        setAlarm(this, nowSeconds, secondsRemaining)
        setNotificationAlarm(this, nowSeconds, notiAlarmSeconds)

        Log.d(
            "TimerActivity:startTimer",
            "secondsRemaining : $secondsRemaining , notiAlarmSeconds : $notiAlarmSeconds"
        )

        timer = object : CountDownTimer(secondsRemaining * 1000, 1000) {
            override fun onFinish() = onTimerFinished()

            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000
                updateCountdownUI()
            }
        }.start()

    }

    private fun setNewTimerLength() {
        val lengthInMinutes = PrefUtil.getTimerLength(this)
        timerLengthSeconds = (lengthInMinutes * 60L)

        if(nowSeconds> notiAlarmSeconds){
            notiAlarmSeconds = timerLengthSeconds - threeMinutes
        }else{
            notiAlarmSeconds = 0
        }

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
                TimerState.Paused -> {
                    fab_start.isEnabled = true
//                    fab_pause.isEnabled = false
                    fab_stop.isEnabled = true
                }
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

    private fun checkPermissions(): Boolean {
//        val permissionState = checkSelfPermission(Manifest.permission.SYSTEM_ALERT_WINDOW)
        val permissionState2 = checkSelfPermission(Manifest.permission.VIBRATE)
        return (
//                permissionState == PackageManager.PERMISSION_GRANTED &&
                permissionState2 == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermissions() {
//        val shouldProvideRationale = shouldShowRequestPermissionRationale(Manifest.permission.SYSTEM_ALERT_WINDOW)
        val shouldProvideRationale2 = shouldShowRequestPermissionRationale(Manifest.permission.VIBRATE)
        if (
//            shouldProvideRationale ||
            shouldProvideRationale2) {
            Log.d("TimerActivity:requestPermissions", "Displaying permission")
            //-----------------------permission rationale not yet implemented -------------------------------

        }
        else {
            Log.d("TimerActivity:requestPermissions", "Requesting Permission")
            requestPermissions( arrayOf(
//                Manifest.permission.SYSTEM_ALERT_WINDOW,
                        Manifest.permission.VIBRATE), 800)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 800){

            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.d("TimerActivity:onRequestPermissionsResult:permission_granted", "grantResults : $${grantResults[0]}")
            }
            else {
                //-----------------------not yet implemented -------------------------------
                Log.d("TimerActivity:onRequestPermissionsResult:permission_not_granted", "grantResults : ${grantResults[0]}")
            }
        }

    }

}
