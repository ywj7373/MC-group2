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
import com.odsay.odsayandroidsdk.API
import com.odsay.odsayandroidsdk.ODsayData
import com.odsay.odsayandroidsdk.ODsayService
import com.odsay.odsayandroidsdk.OnResultCallbackListener
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

interface OnButtonClick {
    fun onDialogClickListener(isStart: Int, place: SearchPlacePlaces)
}

class AddLocationActivity: AppCompatActivity(), View.OnClickListener, OnButtonClick {
    private val TAG = "AddLocationActivity"
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var odsayService : ODsayService
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
    private var srcX: String = ""
    private var srcY: String = ""
    private var desX: String = ""
    private var desY: String = ""
    private var timeToDest : String? = null

    private var year:Int = 0
    private var monthOfYear:Int = 0
    private var dayOfMonth:Int = 0
    private var hourOfDay:Int = 0
    private var minute: Int = 0

    private var userEditStartLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_location)
        locationViewModel = ViewModelProviders.of(this).get(LocationViewModel::class.java)

        //Initialize ODsayService
        odsayService = ODsayService.init(this, getString(R.string.odsay_key))
        odsayService.setConnectionTimeout(5000)
        odsayService.setReadTimeout(5000)

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

        //get address of my current location
        locationViewModel.getLocationData().observe(this, Observer {
            //if the user doesn't edit his or her location, keep receiving
            if (!userEditStartLocation) {
                //set longitude and latitude of the current location
                srcX = it.longitude.toString()
                srcY = it.latitude.toString()

                //Get address of user's current location
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
            R.id.dateText -> openDatePickDialog()
            R.id.timeText -> openTimePickDialog()
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
            userEditStartLocation = true
            startLocText.text = place.name
            startPlace = place
            srcX = place.x
            srcY = place.y
        }
        else {
            endLocText.text = place.name
            endPlace = place
            desX = place.x
            desY = place.y
        }
    }

    //open date pick dialog
    private fun openDatePickDialog() {
        val current = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
        val dialog = DatePickerDialog(this, object:DatePickerDialog.OnDateSetListener {
            override fun onDateSet(view: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
                this@AddLocationActivity.year = year
                this@AddLocationActivity.monthOfYear = monthOfYear
                this@AddLocationActivity.dayOfMonth = dayOfMonth
                dateText.text = (String.format("%04d%02d%02d", year, monthOfYear, dayOfMonth))
            }
        }, Integer.parseInt(current.substring(0,4)), Integer.parseInt(current.substring(4,6)), Integer.parseInt(current.substring(6,8)) )
        dialog.show()
    }

    //open time pick dialog
    private fun openTimePickDialog() {
        val dialog = TimePickerDialog(this, object:TimePickerDialog.OnTimeSetListener {
            override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
                this@AddLocationActivity.hourOfDay = hourOfDay
                this@AddLocationActivity.minute = minute
                timeText.text = (String.format("%02d%02d", hourOfDay, minute))
            }
        }, 12, 0, true)
        dialog.show()
    }

    // Save data to database
    private fun addNewSchedule() {
        //get estimated travel time
        if (srcX != "" && srcY != "" && desX != "" && desY != "") {
            estimateTravelTime(srcX, srcY, desX, desY)
        }
        else {
            Toast.makeText(this, "Please enter correct address!", Toast.LENGTH_LONG).show()
        }
    }

    //callback method to get json data from ODsay
    private val onEstimateTimeResultCallbackListener = object : OnResultCallbackListener {
        override fun onSuccess(odsayData: ODsayData?, api: API?) {
            Log.d(TAG, "Connection to ODsay successful")
            try {
                if (api == API.SEARCH_PUB_TRANS_PATH) {
                    val inquiryResult = (odsayData!!.json.getJSONObject("result").getJSONArray("path").get(0) as JSONObject).getJSONObject("info")
                    timeToDest = inquiryResult.getInt("totalTime").toString()

                    //add data to the database
                    val current = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
                    val time = String.format("%04d%02d%02d%02d%02d", year, monthOfYear, dayOfMonth, hourOfDay, minute)
                    val newLocationItem = LocationItem(
                        endPlace?.name ?: "Unknown", current,
                        endPlace?.x ?: "Unknown", endPlace?.y ?: "Unknown",
                        time, timeToDest ?: "Unknown")

                    locationViewModel.insert(newLocationItem)
                    Toast.makeText(this@AddLocationActivity, "Location saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: JSONException) {
                Log.d(TAG, "JSONException")
                Toast.makeText(this@AddLocationActivity, "Unable to calculate time distance", Toast.LENGTH_LONG).show()
            }
        }

        override fun onError(i: Int, s: String?, api: API?) {
            Log.d(TAG, "Connection to ODsay failed")
            Toast.makeText(this@AddLocationActivity, "Unable to calculate time distance", Toast.LENGTH_LONG).show()
        }
    }

    //call ODsay to estimate Travel time
    private fun estimateTravelTime(sx: String, sy: String, ex: String, ey: String) {
        odsayService.requestSearchPubTransPath(sx, sy, ex, ey, "0", "0", "0", onEstimateTimeResultCallbackListener)
    }
}
