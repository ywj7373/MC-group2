package com.example.bluecatapp.ui.location

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.bluecatapp.R
import com.example.bluecatapp.data.LocationItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


interface OnButtonClick {
    fun onDialogClickListener(isStart: Int, place: SearchPlacePlaces)
}

class AddLocationActivity: AppCompatActivity(), View.OnClickListener, OnButtonClick {
    private val TAG = "AddLocationActivity"
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var titleEdit: EditText
    private lateinit var startLocText: TextView
    private lateinit var endLocText: TextView
    private lateinit var dateText: TextView
    private lateinit var timeText: TextView
    private lateinit var changeStartLoc: Button
    private lateinit var changeDestLoc: Button
    private lateinit var cancelButton: Button
    private lateinit var addButton: Button
    private lateinit var location: String
    private var startPlace : SearchPlacePlaces? = null
    private var endPlace : SearchPlacePlaces? = null

    private var year:Int = 0
    private var monthOfYear:Int = 0
    private var dayOfMonth:Int = 0
    private var hourOfDay:Int = 0
    private var minute: Int = 0

    private var userEditText = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_location)
        locationViewModel = ViewModelProviders.of(this).get(LocationViewModel::class.java)

        //initialize layout components
        titleEdit = findViewById(R.id.titleEdit)
        startLocText = findViewById(R.id.startLocText)
        endLocText = findViewById(R.id.endLocText)
        dateText = findViewById(R.id.dateText)
        timeText = findViewById(R.id.timeText)
        changeStartLoc = findViewById(R.id.changeStartLoc)
        changeDestLoc = findViewById(R.id.changeDestLoc)
        cancelButton = findViewById(R.id.cancelButton)
        addButton = findViewById(R.id.addButton)

        //get address of my location
        locationViewModel.getLocationData().observe(this, Observer {
            //if user didn't edited his or her location
            if (!userEditText) {
                location = it.longitude.toString() + "," + it.latitude.toString()
                Log.e(TAG, location)
                NaverRetrofit.getService().requestReverseGeocode(location)
                    .enqueue(object : Callback<CoordToAddrData> {
                        override fun onFailure(call: Call<CoordToAddrData>, t: Throwable) {
                            Log.e(TAG, "Error requesting reverse geocode")
                            Toast.makeText(
                                this@AddLocationActivity,
                                "Unavailable to connect! Please check wifi",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onResponse(
                            call: Call<CoordToAddrData>,
                            response: Response<CoordToAddrData>
                        ) {
                            Log.e(TAG, response.body().toString())
                            //set result as my current location address if the status code is 0
                            if (response.body()!!.status.code == 0)
                                startLocText.text =
                                    response.body()!!.results[0].land.addition0.value
                            else
                                Toast.makeText(
                                    this@AddLocationActivity,
                                    "Unavailable to get current address!",
                                    Toast.LENGTH_SHORT
                                ).show()
                        }

                    })
            }
        })

        //set click listener
        changeStartLoc.setOnClickListener(this)
        changeDestLoc.setOnClickListener(this)
        dateText.setOnClickListener(this)
        timeText.setOnClickListener(this)
        addButton.setOnClickListener(this)
        cancelButton.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.changeStartLoc -> openSearchPlaceDialog(0)
            R.id.changeDestLoc -> openSearchPlaceDialog(1)
            //Unimplemented-----------------------------Need to implement-----------------
            R.id.dateText -> {
                var dialog = object:DatePickerDialog(this, object:DatePickerDialog.OnDateSetListener {
                    override fun onDateSet(view: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
                        this@AddLocationActivity.year = year
                        this@AddLocationActivity.monthOfYear = monthOfYear
                        this@AddLocationActivity.dayOfMonth = dayOfMonth
                    }
                }, 2019, 10, 27 ) {}
                dialog.show()
            }
            R.id.timeText -> {
                var dialog = object:TimePickerDialog(this, object:TimePickerDialog.OnTimeSetListener {
                    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
                        this@AddLocationActivity.hourOfDay = hourOfDay
                        this@AddLocationActivity.minute = minute
                    }
                }, 12, 0, true) { }
                dialog.show()
            }
            R.id.addButton -> addNewSchedule()
            R.id.cancelButton -> finish()
        }
    }

    //show search place dialog
    private fun openSearchPlaceDialog(isStart: Int) {
        val searchPlaceDialog = SearchPlaceDialog.newInstance(location, isStart)
        searchPlaceDialog.show(supportFragmentManager, null)
    }

    //create click listener to pass data from dialog to activity
    override fun onDialogClickListener(isStart: Int, place: SearchPlacePlaces) {
        if (isStart == 0) {
            userEditText = true
            startLocText.text = place.name
            startPlace = place
        }
        else {
            endLocText.text = place.name
            endPlace = place
        }
    }

    // Save data to database
    //Implement database functions here!!!!
    private fun addNewSchedule() {
        val time = year.toString() + monthOfYear.toString() + dayOfMonth.toString() +
                hourOfDay.toString() + minute.toString()
        val newLocationItem = LocationItem(endPlace!!.name, "", endPlace!!.x, endPlace!!.y, time)
        locationViewModel.insert(newLocationItem)
        Toast.makeText(this@AddLocationActivity, "Location saved!", Toast.LENGTH_SHORT).show()
        Log.d("LOGGING", "" + year +", "+ monthOfYear +", " + hourOfDay)
        //if the user didn't modify start location, use current location
        //check if each place is still null -> user didn't acquired preferred location -> alert message
        finish()
    }
}