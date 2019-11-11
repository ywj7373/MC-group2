package com.example.bluecatapp.ui.location

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.bluecatapp.R
import com.example.bluecatapp.data.CurrentLocationData
import com.example.bluecatapp.data.LocationItemData
import com.example.bluecatapp.data.LocationRepository
import com.odsay.odsayandroidsdk.API
import com.odsay.odsayandroidsdk.ODsayData
import com.odsay.odsayandroidsdk.ODsayService
import com.odsay.odsayandroidsdk.OnResultCallbackListener
import kotlinx.android.synthetic.main.activity_add_location.*
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.concurrent.thread

interface OnButtonClick {
    fun onDialogClickListener(place: SearchPlacePlaces)
}

class AddLocationActivity: AppCompatActivity(), View.OnClickListener, OnButtonClick {
    private val TAG = "AddLocationActivity"
    private lateinit var odsayService : ODsayService
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var titleEdit: TextView
    private lateinit var endLocText: TextView
    private lateinit var dateText: TextView
    private lateinit var dateButton: ImageButton
    private lateinit var changeDestLoc: Button
    private lateinit var cancelButton: Button
    private lateinit var addButton: Button
    private var srcX: String = "126.952162"
    private var srcY: String = "37.459553"
    private var desX: String = ""
    private var desY: String = ""
    private var location: String = "126.952162" + "," + "37.459553"
    private lateinit var dayButtons : Array<ToggleButton>
    private var endPlace : SearchPlacePlaces? = null
    private var timeToDest : String? = null

    private var year:Int = 0
    private var monthOfYear:Int = 0
    private var dayOfMonth:Int = 0

    private var days_mode_set:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_location)
        locationViewModel = ViewModelProviders.of(this).get(LocationViewModel::class.java)

        //set current location
        locationViewModel.getCurrentLocation().observe(this,
            Observer<CurrentLocationData> {
                if (it != null) {
                    srcX = it.longitude.toString()
                    srcY = it.latitude.toString()
                    location = it.longitude.toString() + "," + it.latitude.toString()
                }
            })

        //Initialize ODsayService
        odsayService = ODsayService.init(this, getString(R.string.odsay_key))
        odsayService.setConnectionTimeout(ODsayTimeout)
        odsayService.setReadTimeout(ODsayTimeout)

        //initialize layout components
        titleEdit = findViewById(R.id.titleEdit)
        endLocText = findViewById(R.id.endLocText)
        dateText = findViewById(R.id.dateText)
        dateButton = findViewById(R.id.dateButton)
        changeDestLoc = findViewById(R.id.changeDestLoc)
        cancelButton = findViewById(R.id.cancelButton)
        addButton = findViewById(R.id.addButton)
        dayButtons = arrayOf(findViewById(R.id.toggleButton1),
            findViewById(R.id.toggleButton2),
            findViewById(R.id.toggleButton3),
            findViewById(R.id.toggleButton4),
            findViewById(R.id.toggleButton5),
            findViewById(R.id.toggleButton6),
            findViewById(R.id.toggleButton7))

        // At first, date is today
        val current = Calendar.getInstance()
        year = current.get(Calendar.YEAR)
        monthOfYear = current.get(Calendar.MONTH)
        dayOfMonth = current.get(Calendar.DAY_OF_MONTH)
        dateText.text = SimpleDateFormat("yyyy-MM-dd").format(current.time)

        //set click listener
        changeDestLoc.setOnClickListener(this)
        dateButton.setOnClickListener(this)
        addButton.setOnClickListener(this)
        cancelButton.setOnClickListener(this)
        for (tb in dayButtons)
            tb.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.changeDestLoc -> openSearchPlaceDialog()
            R.id.dateButton -> openDatePickDialog()
            R.id.addButton -> addNewSchedule()
            R.id.cancelButton -> finish()
            R.id.toggleButton1, R.id.toggleButton2, R.id.toggleButton3, R.id.toggleButton4,
            R.id.toggleButton5, R.id.toggleButton6, R.id.toggleButton7 -> daysMode()
        }
    }

    //show search place dialog
    private fun openSearchPlaceDialog() {
        thread(start=true) {
            Log.d(TAG, location)
            val searchPlaceDialog = SearchPlaceDialog.newInstance(location)
            searchPlaceDialog.show(supportFragmentManager, null)
        }
    }

    //create click listener to pass data from dialog to activity
    override fun onDialogClickListener(place: SearchPlacePlaces) {
        endLocText.text = place.name
        endPlace = place
        desX = place.x
        desY = place.y
    }

    //open date pick dialog
    private fun openDatePickDialog() {
        val current = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
        val dialog = DatePickerDialog(this, object:DatePickerDialog.OnDateSetListener {
            override fun onDateSet(view: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
                this@AddLocationActivity.year = year
                this@AddLocationActivity.monthOfYear = monthOfYear
                this@AddLocationActivity.dayOfMonth = dayOfMonth
                dateText.text = (String.format("%04d-%02d-%02d", year, monthOfYear+1, dayOfMonth))
                clearDays()
            }
        }, Integer.parseInt(current.substring(0,4)), Integer.parseInt(current.substring(4,6))-1, Integer.parseInt(current.substring(6,8)) )
        dialog.show()
    }
    private fun clearDays() {
        for (tb in dayButtons) {
            tb.setChecked(false)
        }
        days_mode_set = false
    }
    private fun daysMode() {
        var str = ""
        for (tb in dayButtons) {
            if(tb.isChecked) {
                str = str + tb.textOn + ","
            }
        }

        // if there is no day checked
        if(str == "") {
            val current = Calendar.getInstance()
            year = current.get(Calendar.YEAR)
            monthOfYear = current.get(Calendar.MONTH)
            dayOfMonth = current.get(Calendar.DAY_OF_MONTH)
            dateText.text = SimpleDateFormat("yyyy-MM-dd").format(current.time)
            days_mode_set = false
        }
        // if there is at least one day checked
        else {
            dateText.text = str.substring(0, str.length-1)
            days_mode_set = true
        }
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
                    val time = String.format("%04d-%02d-%02d %02d:%02d:00", year, monthOfYear+1, dayOfMonth,
                        location_edit_time.hour, location_edit_time.minute)
                    val newLocationItem = LocationItemData(
                        endPlace?.name ?: "Unknown",
                        endPlace?.x ?: "Unknown", endPlace?.y ?: "Unknown",
                        time, timeToDest ?: "Unknown", false, false, days_mode_set, dateText.text.toString())

                    locationViewModel.insert(newLocationItem)
                    Log.d(TAG, timeToDest)
                    Toast.makeText(this@AddLocationActivity, "Location saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: JSONException) {
                Log.d(TAG, "JSONException")
                Toast.makeText(this@AddLocationActivity, "Unable to calculate time distance", Toast.LENGTH_LONG).show()
                //Need to figure out if it failed because the user was too close or too far from the destination
            }
        }

        override fun onError(i: Int, s: String?, api: API?) {
            Log.d(TAG, "Connection to ODsay failed")
            Toast.makeText(this@AddLocationActivity, "Connection failed", Toast.LENGTH_LONG).show()
        }
    }

    //call ODsay to estimate Travel time
    private fun estimateTravelTime(sx: String, sy: String, ex: String, ey: String) {
        odsayService.requestSearchPubTransPath(sx, sy, ex, ey, "0", "0", "0", onEstimateTimeResultCallbackListener)
    }
}
