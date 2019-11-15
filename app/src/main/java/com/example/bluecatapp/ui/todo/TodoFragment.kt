package com.example.bluecatapp.ui.todo

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluecatapp.AddTodoActivity
import com.example.bluecatapp.R
import com.example.bluecatapp.data.TodoItem
import kotlinx.android.synthetic.main.fragment_todo.*
import android.content.DialogInterface
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.Chronometer
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getSystemService
import androidx.preference.PreferenceManager
import com.example.bluecatapp.HwAlarmReceiver

class TodoFragment : Fragment() {

    //================================== Common ==================================//
    private lateinit var preference: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val _5Min: Long = 1000 * 60 * 5
    private val _1Sec: Long = 1000

    //================================== Notification ==================================//
    private var notiTime: Long = _5Min

    //================================== Alarms ==================================//

    private var alarmTime: Long = _1Sec
    private val ALARM_REQUEST_CODE = 100
    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent

    //================================== Motion Sensors ==================================//
    private lateinit var shakeSensorManager: SensorManager
    private lateinit var walkSensorManager: SensorManager

    private var shakeAccel: Float = 0.toFloat() // acceleration apart from gravity
    private var shakeAccelCurrent: Float = 0.toFloat() // current acceleration including gravity
    private var shakeAccelLast: Float = 0.toFloat() // last acceleration including gravity

    private var shakeLimit: Int = 0// (at onCreate) set shake count limit from shared preferences
    private var alarmCount: Int = 0
    private var shakeCount: Int = 0

    private var walkAccel: Float = 0.toFloat()
    private var walkAccelCurrent: Float = 0.toFloat()
    private var walkAccelLast: Float = 0.toFloat()

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

            if (shakeAccel > 6) {
                shakeCount++
                val toast = Toast.makeText(
                    requireContext(),
                    "alarmCount : $alarmCount" +
                            "Device has shaken $shakeCount times",
                    Toast.LENGTH_LONG
                )
                toast.show()
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

    //================================== Homework mode general ==================================//
    val hwDoneDisplayText = "I have done my homework."
    val hwDoneConfirmText = hwDoneDisplayText.replace("\\s".toRegex(), "").toLowerCase()

    var currentHwClockTime: Long = 0 // to track the time when a user gets out of the fragment

    private var isHomeworkMode = false

//    private val testNotificationTime = _5Min
//        1000 * 60 * 44 + 1000 * 57// test : 3 seconds
//        1000*60*5 // 5 minutes

//    private val testAlarmTime = 1000
//        1000 * 60 * 44 * +1000 * 54 // test : 6 seconds
//        1000 // 1 second

    private var hwModeTime: Long =
        0// at onCreate, initialized following the value of shared preference

//    private var hwModeTime2: Long = 0// test

    private var hwModeClockBaseTime: Long = 0

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

        alarmManager = activity!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), HwAlarmReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )


        hwModeTime = preference.getLong(getString(R.string.hw_time_value), 1000 * 60 * 45)
        Log.d("HW:onCreate:hwModeTime", "value : $hwModeTime")

        //================================== Motion Sensors ==================================//
        //================================== Detect Shaking ==================================//
        shakeLimit = preference.getInt(getString(R.string.hw_shake_value), 30)
        Log.d("HW:onCreate:shakeLimit", "value : $shakeLimit")

        this.shakeSensorManager =
            activity!!.getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        this.walkSensorManager = activity!!.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        shakeSensorManager.registerListener(
            shakeSensorListener,
            shakeSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        );
        shakeAccel = 0.00f;
        shakeAccelCurrent = SensorManager.GRAVITY_EARTH;
        shakeAccelLast = SensorManager.GRAVITY_EARTH;

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
                                //---Set Shared Preference of clock---//
                                editor.putLong(
                                    getString(R.string.hw_time_current_clock_time),
                                    view_timer.base
                                )
                                view_timer.isCountDown = false
                                view_timer.base = SystemClock.elapsedRealtime() + hwModeTime
                                view_timer.stop()

                                isHomeworkMode = false;
                                editor.putBoolean(getString(R.string.hw_mode_bool), isHomeworkMode)
                                editor.commit()

                                //---cancel the alarm---//
                                alarmManager.cancel(pendingIntent)

                                //---UI---//
                                text_homework.text = "Turn Homework Mode ON"
                                text_homework.setBackgroundColor(Color.parseColor("#111111"))
                                text_homework.setTextColor(Color.parseColor("#ffffff"))
                                todo_ll_container.setBackgroundColor(Color.parseColor("#ffffff"))
                                clock_homework.visibility = View.VISIBLE
//                text_hw_timer.visibility = View.GONE
                                view_timer.visibility = View.GONE

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
                            //                        val inputText = editText.text.toString()
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

    private fun turnHWModeOn() {
        // Initialize shakeCount every time hw mode turns on.
        shakeCount = 0

        Toast.makeText(requireContext(), "Good Luck!", Toast.LENGTH_LONG).show()
        view_timer.isCountDown = true
        if(currentHwClockTime>0){ // if clock time already exists
            view_timer.base = currentHwClockTime
        }else{
            view_timer.base = SystemClock.elapsedRealtime() + hwModeTime
        }
        view_timer.start()
        view_timer.setOnChronometerTickListener { view ->

            if (alarmCount == 0) {

                if (view.base - SystemClock.elapsedRealtime() <= notiTime) {
                    alarmCount++
                    Toast.makeText(
                        requireContext(),
                        "This is when notification goes off" +
                                "${view.base} ${SystemClock.elapsedRealtime()} ${view.base - SystemClock.elapsedRealtime()} $notiTime",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } else if (alarmCount == 1) {

                if (view.base - SystemClock.elapsedRealtime() <= alarmTime) {
                    alarmCount++
                    shakeCount = 0
                }

            } else if (alarmCount == 2) {


                if (shakeCount >= shakeLimit) {
                    Toast.makeText(
                        requireContext(),
                        "Hope you are awake. Back to HW mode",
                        Toast.LENGTH_LONG
                    ).show()
                    alarmCount = 0
                    shakeCount = 0
                    view_timer.base = SystemClock.elapsedRealtime() + hwModeTime
                }

            }

        }

        isHomeworkMode = true
        editor.putBoolean(getString(R.string.hw_mode_bool), isHomeworkMode)
        editor.commit()

        text_homework.text = "Turn Homework Mode OFF"
        text_homework.setBackgroundColor(Color.parseColor("#dddddd"))
        text_homework.setTextColor(Color.parseColor("#111111"))
        todo_ll_container.setBackgroundColor(Color.parseColor("#111111"))
        clock_homework.visibility = View.GONE
//                text_hw_timer.visibility = View.VISIBLE
        view_timer.visibility = View.VISIBLE
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
            val newTodoItem = TodoItem(
                data!!.getStringExtra(AddTodoActivity.TASK),
                data.getStringExtra(AddTodoActivity.DATETIME),
//                data.getStringExtra(AddTodoActivity.LOCATION),
                false
            )

            todoViewModel.insert(newTodoItem)

            Toast.makeText(requireContext(), "Todo saved!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Todo not saved!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        shakeCount = 0

        isHomeworkMode = preference.getBoolean(getString(R.string.hw_mode_bool), false)
        currentHwClockTime =
            preference.getLong(getString(R.string.hw_time_current_clock_time), SystemClock.elapsedRealtime()+hwModeTime)


        Log.d(
            "HW:onStart:clockValue",
            "HW onStart$currentHwClockTime ${currentHwClockTime / 1000 / 60} ${currentHwClockTime / 1000}"
        )
        Log.d("HW:onStart:modeBool", "HW onStart $isHomeworkMode")

        shakeSensorManager.registerListener(
            shakeSensorListener,
            shakeSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )

//        walkSensorManager.registerListener(
//            walkSensorListener,
//            walkSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
//            SensorManager.SENSOR_DELAY_NORMAL)

    }

    override fun onPause() {
        super.onPause()
        shakeSensorManager.unregisterListener(shakeSensorListener);
//        walkSensorManager.unregisterListener(walkSensorListener);
//        alarmManager.cancel(pendingIntent) // turn off alarm only when app itself is onDestroy

        Log.d(
            "HW:onPause:clockValue",
            "HW onPause at ${view_timer.base} ${view_timer.base / 1000 / 60} ${view_timer.base / 1000}"
        )
        Log.d("HW:onPause:modeBool", "HW onPause at $isHomeworkMode")

        editor.putLong(getString(R.string.hw_time_current_clock_time), view_timer.base)
        editor.putBoolean(getString(R.string.hw_mode_bool), isHomeworkMode)
        editor.commit()


    }

    override fun onResume() {
        super.onResume()
        shakeCount = 0
        isHomeworkMode = preference.getBoolean(getString(R.string.hw_mode_bool), false)
        currentHwClockTime =
            preference.getLong(getString(R.string.hw_time_current_clock_time), SystemClock.elapsedRealtime()+hwModeTime)

        Log.d(
            "HW:onResume:clockValue",
            "HW onResume $currentHwClockTime ${currentHwClockTime / 1000 / 60} ${currentHwClockTime / 1000}"
        )
        Log.d("HW:onResume:modeBool", "HW onResume $isHomeworkMode")

        shakeSensorManager.registerListener(
            shakeSensorListener,
            shakeSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )

        if (isHomeworkMode) {
            turnHWModeOn()
        }

//        walkSensorManager.registerListener(
//            walkSensorListener,
//            walkSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
//            SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onDestroy() {
        super.onDestroy()
        shakeSensorManager.unregisterListener(shakeSensorListener);
        //        walkSensorManager.unregisterListener(walkSensorListener);
//        alarmManager.cancel(pendingIntent) // turn off alarm only when app is onDestroy
    }


}