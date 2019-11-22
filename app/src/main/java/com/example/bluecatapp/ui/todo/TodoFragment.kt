package com.example.bluecatapp.ui.todo

import android.app.*
import android.content.*
import android.graphics.Color
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluecatapp.data.TodoItem
import kotlinx.android.synthetic.main.fragment_todo.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import android.widget.Chronometer
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.NavDeepLinkBuilder
import androidx.preference.PreferenceManager
import com.example.bluecatapp.*
import com.example.bluecatapp.util.NotificationUtil
import com.example.bluecatapp.util.PrefUtil
import kotlinx.android.synthetic.main.content_timer.*
import org.koin.android.ext.android.get
import java.util.*

class TodoFragment : Fragment() {

    companion object {
        fun setAlarm(context: Context, nowSeconds: Long, secondsRemaining: Long): Long{
            val wakeUpTime = (nowSeconds + secondsRemaining) * 1000
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TimerExpiredReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeUpTime, pendingIntent)
            PrefUtil.setAlarmSetTime(nowSeconds, context)
            return wakeUpTime
        }

        fun removeAlarm(context: Context){
            val intent = Intent(context, TimerExpiredReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            PrefUtil.setAlarmSetTime(0, context)
        }

        val nowSeconds: Long
            get() = Calendar.getInstance().timeInMillis / 1000
    }

    enum class TimerState{
        Stopped, Paused, Running
    }

    private lateinit var timer: CountDownTimer
    private var timerLengthSeconds: Long = 0
    private var timerState = TimerState.Stopped

    private var secondsRemaining: Long = 0

    //================================== Common ==================================//
    private lateinit var preference: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val _5Min: Long = 1000 * 60 * 5
    private val _testAlarmOffset: Long = 1000 * 60 * 4 + 1000 * 57
    private val _1Sec: Long = 1000

    //================================== Notification ==================================//
    private var notiOffset: Long = _5Min
    private var notiAlarmTime: Long = 0 // hwModeTime - notiOffset @onCreate

    //================================== Alarms ==================================//

    private var alarmOffset: Long = _testAlarmOffset // test
//    private var alarmOffset: Long = _1Sec
    private var finalAlarmTime: Long = 0 // hwModeTime - alarmOffset @onCreate

    private val SHAKE_COMPLETE_CODE = R.integer.SHAKE_COMPLETE_CODE
    private val ALARM_NOTI_REQUEST_CODE = R.integer.ALARM_NOTI_REQUEST_CODE
    private val ALARM_FINAL_REQUEST_CODE = R.integer.ALARM_FINAL_REQUEST_CODE

    private lateinit var hwAlarmReceiver: HwAlarmReceiver

    //================================== Homework mode general ==================================//
    val hwDoneDisplayText = "I have done my homework."
    val hwDoneConfirmText = hwDoneDisplayText.replace("\\s".toRegex(), "").toLowerCase()

    var currentHwClockTime: Long = 0 // to track the time when a user gets out of the fragment

    private var isHomeworkMode = false

    private var hwModeTime: Long =
        0// at onCreate, initialized following the value of shared preference

//    private var hwModeTime2: Long = 0// test

//    private var hwModeClockBaseTime: Long = 0

    //================================== To-do List ==================================//
    private val ADD_TODO_REQUEST = 1
    private lateinit var todoViewModel: TodoViewModel
    private val todoAdapter = TodoAdapter()
    private val todoAdapter2 = TodoAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //================================== General ==================================//
        setHasOptionsMenu(true)
        retainInstance = true

        preference = PreferenceManager.getDefaultSharedPreferences(requireContext())
        editor = preference.edit()

        //================================== Alarms ==================================//

        hwAlarmReceiver = HwAlarmReceiver()

        hwModeTime = preference.getLong(getString(R.string.hw_time_value), 1000 * 60 * 45)
        Log.d("HW:onCreate:hwModeTime", "value : $hwModeTime")

        notiAlarmTime = hwModeTime - notiOffset // hwModeTime - notiOffset @onCreate
        finalAlarmTime = hwModeTime - alarmOffset// hwModeTime - alarmOffset @onCreate

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        todoViewModel =
            ViewModelProviders.of(this).get(TodoViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_todo, container, false)

        todoViewModel.getTodoItemsNotDone().observe(this,
            Observer<List<TodoItem>> { t -> todoAdapter.setTodoItems(t!!) })

        todoViewModel.getTodoItemsDone().observe(this,
            Observer<List<TodoItem>> { t -> todoAdapter2.setTodoItems(t!!) })

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // RecyclerView node initialized here

        todo_recycler_view.apply {
            // set a LinearLayoutManager to handle Android
            // RecyclerView behavior
            layoutManager = LinearLayoutManager(activity)
            // set the custom adapter to the RecyclerView
            adapter = todoAdapter
            setHasFixedSize(true)
        }

        // view model 을 통해 특정 todoItem 에 대한 데이터 변경 실행.
        todoAdapter.onItemClick = { todoItem ->
            //            Log.d("test","todoItem test "+todoItem.id.toString()+"/"+todoItem.done.toString())
            todoViewModel.updateTodoStatus(todoItem)
        }

        todoDone_recycler_view.apply {
            // set a LinearLayoutManager to handle Android
            // RecyclerView behavior
            layoutManager = LinearLayoutManager(activity)
            // set the custom adapter to the RecyclerView
            adapter = todoAdapter2
            setHasFixedSize(true)
        }

        todoAdapter2.onItemClick = { todoItem ->
            //            Log.d("test","todoItem test "+todoItem.id.toString()+"/"+todoItem.done.toString())
            todoViewModel.updateTodoStatus(todoItem)
        }

        container_hwmode.setOnClickListener(clickListener)

        todo_add_task.setOnClickListener { view ->
            //            getActivity().
            startActivityForResult(
                Intent(requireContext(), AddTodoActivity::class.java),
                ADD_TODO_REQUEST
            )
        }

        hw_start_btn.setOnClickListener{v ->
            startTimer()
            timerState =  TimerState.Running
            updateButtons()
        }

        hw_pause_btn.setOnClickListener { v ->
            timer.cancel()
            timerState = TimerState.Paused
            updateButtons()
        }
        hw_stop_btn.setOnClickListener { v ->
            timer.cancel()
            timerState = TimerState.Stopped
            updateButtons()
            if (isHomeworkMode) {

                var alertDialog = AlertDialog.Builder(requireContext()).create()
                val editText = EditText(requireContext())
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

                            turnHWModeOff()

                            Toast.makeText(
                                requireContext(),
                                "Nice job!",
                                Toast.LENGTH_SHORT
                            ).show()

                            alertDialog.dismiss()

                        } else {
                            Toast.makeText(
                                activity!!.applicationContext,
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
                            activity!!.applicationContext
                            , "Keep Working!",
                            Toast.LENGTH_SHORT
                        ).show()

                        alertDialog.dismiss()
                    })

                alertDialog.show()

            } else { // turning on the hw mode
                turnHWModeOn()
            }
        }

    }

//    override fun onStart() {
//        super.onStart()
////        shakeCount = 0
//
//        updateHwAndAlarmTime()
//        isHomeworkMode = preference.getBoolean(getString(R.string.hw_mode_bool), false)
//        currentHwClockTime =
//            preference.getLong(
//                getString(R.string.hw_time_current_clock_time),
//                SystemClock.elapsedRealtime() + hwModeTime
//            )
//
//        Log.d(
//            "HW:onStart:clockValue",
//            "HW onStart$currentHwClockTime ${currentHwClockTime / 1000 / 60} ${currentHwClockTime / 1000}"
//        )
//        Log.d("HW:onStart:modeBool", "HW onStart $isHomeworkMode")
//
//    }

    override fun onResume() {
        super.onResume()

        updateHwAndAlarmTime()
        isHomeworkMode = preference.getBoolean(getString(R.string.hw_mode_bool), false)
        currentHwClockTime =
            preference.getLong(
                getString(R.string.hw_time_current_clock_time),
                SystemClock.elapsedRealtime() + hwModeTime
            )

        Log.d(
            "HW:onResume:clockValue",
            "HW onResume $currentHwClockTime ${currentHwClockTime / 1000 / 60} ${currentHwClockTime / 1000}"
        )
        Log.d("HW:onResume:modeBool", "HW onResume $isHomeworkMode")

        if (isHomeworkMode) {
            turnHWModeOn()
        }

        initTimer()

        removeAlarm(requireContext())
        NotificationUtil.hideTimerNotification(requireContext())

    }

    override fun onPause() {
        super.onPause()
//        walkSensorManager.unregisterListener(walkSensorListener);
//        alarmManager.cancel(pendingIntent) // turn off alarm only when app itself is onDestroy

//        Log.d(
//            "HW:onPause:clockValue",
//            "HW onPause at ${view_timer.base} ${view_timer.base / 1000 / 60} ${view_timer.base / 1000}"
//        )
        Log.d("HW:onPause:modeBool", "HW onPause at $isHomeworkMode")

        //---change the shared preference---//
//        saveCurrentClockTime(view_timer.base)
        toggleHWMode(isHomeworkMode)

        if (timerState == TimerState.Running){
            timer.cancel()
            val wakeUpTime = setAlarm(requireContext(), nowSeconds, secondsRemaining)
            NotificationUtil.showTimerRunning(requireContext(), wakeUpTime)
        }
        else if (timerState == TimerState.Paused){
            NotificationUtil.showTimerPaused(requireContext())
        }

        PrefUtil.setPreviousTimerLengthSeconds(timerLengthSeconds, requireContext())
        PrefUtil.setSecondsRemaining(secondsRemaining, requireContext())
        PrefUtil.setTimerState(timerState, requireContext())

    }

    private fun initTimer(){
        timerState = PrefUtil.getTimerState(requireContext())

        //we don't want to change the length of the timer which is already running
        //if the length was changed in settings while it was backgrounded
        if (timerState == TimerState.Stopped)
            setNewTimerLength()
        else
            setPreviousTimerLength()

        secondsRemaining = if (timerState == TimerState.Running || timerState == TimerState.Paused)
            PrefUtil.getSecondsRemaining(requireContext())
        else
            timerLengthSeconds

        val alarmSetTime = PrefUtil.getAlarmSetTime(requireContext())
        if (alarmSetTime > 0)
            secondsRemaining -= nowSeconds - alarmSetTime

        if (secondsRemaining <= 0)
            onTimerFinished()
        else if (timerState == TimerState.Running)
            startTimer()

        updateButtons()
        updateCountdownUI()
    }
    private fun onTimerFinished(){
        timerState = TimerState.Stopped

        //set the length of the timer to be the one set in SettingsActivity
        //if the length was changed when the timer was running
        setNewTimerLength()

        progress_countdown.progress = 0

        PrefUtil.setSecondsRemaining(timerLengthSeconds, requireContext())
        secondsRemaining = timerLengthSeconds

        updateButtons()
        updateCountdownUI()
    }
    private fun startTimer(){
        timerState = TimerState.Running

        timer = object : CountDownTimer(secondsRemaining * 1000, 1000) {
            override fun onFinish() = onTimerFinished()

            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000
                updateCountdownUI()
            }
        }.start()
    }
    private fun setNewTimerLength(){
        val lengthInMinutes = PrefUtil.getTimerLength(requireContext())
        timerLengthSeconds = (lengthInMinutes * 60L)
        progress_countdown.max = timerLengthSeconds.toInt()
    }

    private fun setPreviousTimerLength(){
        timerLengthSeconds = PrefUtil.getPreviousTimerLengthSeconds(requireContext())
        progress_countdown.max = timerLengthSeconds.toInt()
    }

    private fun updateCountdownUI(){
        val minutesUntilFinished = secondsRemaining / 60
        val secondsInMinuteUntilFinished = secondsRemaining - minutesUntilFinished * 60
        val secondsStr = secondsInMinuteUntilFinished.toString()
        textView_countdown.text = "$minutesUntilFinished:${if (secondsStr.length == 2) secondsStr else "0" + secondsStr}"
        progress_countdown.progress = (timerLengthSeconds - secondsRemaining).toInt()
    }

    private fun updateButtons(){
        when (timerState) {
            TimerState.Running ->{
                hw_start_btn.isEnabled = false
                hw_pause_btn.isEnabled = true
                hw_stop_btn.isEnabled = true
            }
            TimerState.Stopped -> {
                hw_start_btn.isEnabled = true
                hw_pause_btn.isEnabled = false
                hw_stop_btn.isEnabled = false
            }
            TimerState.Paused -> {
                hw_start_btn.isEnabled = true
                hw_pause_btn.isEnabled = false
                hw_stop_btn.isEnabled = true
            }
        }
    }

    private fun updateHwAndAlarmTime() {
        hwModeTime = preference.getLong(getString(R.string.hw_time_value), 1000 * 60 * 45)
        notiAlarmTime = hwModeTime - notiOffset // hwModeTime - notiOffset @onCreate
        finalAlarmTime = hwModeTime - alarmOffset// hwModeTime - alarmOffset @onCreate
    }

    private val clickListener: View.OnClickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.container_hwmode -> {

                if (isHomeworkMode) {

                    var alertDialog = AlertDialog.Builder(requireContext()).create()
                    val editText = EditText(requireContext())
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

                                turnHWModeOff()

                                Toast.makeText(
                                    requireContext(),
                                    "Nice job!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                alertDialog.dismiss()

                            } else {
                                Toast.makeText(
                                    activity!!.applicationContext,
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
                                activity!!.applicationContext
                                , "Keep Working!",
                                Toast.LENGTH_SHORT
                            ).show()

                            alertDialog.dismiss()
                        })

                    alertDialog.show()

                } else { // turning on the hw mode
                    turnHWModeOn()
                }
            }
        }
    }

    private fun saveCurrentClockTime(time: Long) {
        editor.putLong(getString(R.string.hw_time_current_clock_time), time)
        editor.commit()
    }

    fun toggleHWMode(modeState: Boolean) { // stay
        editor.putBoolean(getString(R.string.hw_mode_bool), modeState)
        editor.commit()
    }

    fun turnHWModeOff(){
        container_hw_timer.visibility=View.GONE

        updateHwAndAlarmTime()
        //---Set Shared Preference of clock---//
        editor.putLong(
            getString(R.string.hw_time_current_clock_time),
            SystemClock.elapsedRealtime() + hwModeTime
        )
        editor.commit()


        isHomeworkMode = false

        //---change the shared preference---//
        toggleHWMode(isHomeworkMode)

        //---cancel the alarm---//
        hwAlarmReceiver.cancelAlarm(
            requireContext(),
            ALARM_NOTI_REQUEST_CODE
        )
        hwAlarmReceiver.cancelAlarm(
            requireContext(),
            ALARM_FINAL_REQUEST_CODE
        )

        //---UI---//
        text_homework.text = "Turn Homework Mode ON"
        text_homework.setBackgroundColor(Color.parseColor("#111111"))
        text_homework.setTextColor(Color.parseColor("#ffffff"))
        todo_ll_container.setBackgroundColor(Color.parseColor("#ffffff"))
        clock_homework.visibility = View.VISIBLE
//                text_hw_timer.visibility = View.GONE
//        view_timer.visibility = View.GONE
    }

    private fun turnHWModeOn() {
        // Initialize shakeCount every time hw mode turns on.
//        shakeCount = 0
        container_hw_timer.visibility=View.VISIBLE

        startTimer()

        Toast.makeText(requireContext(), "Good Luck!", Toast.LENGTH_SHORT).show()
//        view_timer.isCountDown = true

        updateHwAndAlarmTime()
        currentHwClockTime = preference.getLong(getString(R.string.hw_time_current_clock_time), 0)

//        if (currentHwClockTime > 0) { // if clock time already exists
//            view_timer.base = currentHwClockTime
//        } else {
//            view_timer.base = SystemClock.elapsedRealtime() + hwModeTime
//        }
//        view_timer.start()

        //---set the alarm---//
        hwAlarmReceiver.setNotiAlarm(requireContext(), notiAlarmTime)
        hwAlarmReceiver.setFinalAlarm(requireContext(), finalAlarmTime)

        //---change the shared preference---//
        isHomeworkMode = true
        toggleHWMode(isHomeworkMode)

        text_homework.text = "Turn Homework Mode OFF"
        text_homework.setBackgroundColor(Color.parseColor("#dddddd"))
        text_homework.setTextColor(Color.parseColor("#111111"))
        todo_ll_container.setBackgroundColor(Color.parseColor("#111111"))
        clock_homework.visibility = View.GONE
//                text_hw_timer.visibility = View.VISIBLE
//        view_timer.visibility = View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.todo_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item?.itemId) {
            R.id.todo_delete_all_tasks -> {
                todoViewModel.deleteAllTodoItems()
                Toast.makeText(requireContext(), "All To-do Items deleted!", Toast.LENGTH_SHORT)
                    .show()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_TODO_REQUEST && resultCode == Activity.RESULT_OK) {
            Log.d("TodoFragment:onActivityResult", "requestCode : $requestCode")
            val newTodoItem = TodoItem(
                data!!.getStringExtra(AddTodoActivity.TASK),
                data.getStringExtra(AddTodoActivity.DATETIME),
//                data.getStringExtra(AddTodoActivity.LOCATION),
                false
            )

            todoViewModel.insert(newTodoItem)

            Toast.makeText(requireContext(), "Todo saved!", Toast.LENGTH_SHORT).show()
        } else if(requestCode == SHAKE_COMPLETE_CODE && resultCode == 1){
            Log.d("TodoFragment:onActivityResult", "requestCode : $requestCode")
            turnHWModeOff()

        }else{
            Log.d("TodoFragment:onActivityResult", "[ERROR] Undefined requestCode : $requestCode")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("HW:onDestroy", "HW:onDestroy")
    }



}