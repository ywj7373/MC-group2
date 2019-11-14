package com.example.bluecatapp.ui.location

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.bluecatapp.R
import com.example.bluecatapp.data.CurrentLocationData
import com.example.bluecatapp.data.LocationItemData
import com.mancj.materialsearchbar.MaterialSearchBar
import kotlinx.android.synthetic.main.activity_add_location.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

const val default_Y = "37.459553"
const val default_X = "126.952162"

class AddLocationActivity: AppCompatActivity(), View.OnClickListener, MaterialSearchBar.OnSearchActionListener {
    private val TAG = "AddLocationActivity"
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var dateText: TextView
    private lateinit var dateButton: Button
    private lateinit var addButton: Button
    private lateinit var dayButtons : Array<ToggleButton>
    private lateinit var searchBar: MaterialSearchBar
    private lateinit var loc1: TextView
    private lateinit var loc2: TextView
    private lateinit var loc3: TextView
    private lateinit var loc4: TextView
    private lateinit var loc5: TextView
    private lateinit var results: List<SearchPlacePlaces>
    private var location: String = "$default_X,$default_Y"
    private var place : SearchPlacePlaces? = null
    private var year: Int = 0
    private var monthOfYear: Int = 0
    private var dayOfMonth: Int = 0
    private var days_mode_set: Boolean = false
    private var picked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_location)
        locationViewModel = ViewModelProviders.of(this).get(LocationViewModel::class.java)

        //set current location
        locationViewModel.getCurrentLocation().observe(this,
            Observer<CurrentLocationData> {
                if (it != null) {
                    location = it.longitude.toString() + "," + it.latitude.toString()
                }
            })

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }

        //initialize layout components
        searchBar = findViewById(R.id.searchBar)
        searchBar.setHint("Search")
        searchBar.setOnSearchActionListener(this);

        loc1 = findViewById(R.id.loc1)
        loc2 = findViewById(R.id.loc2)
        loc3 = findViewById(R.id.loc3)
        loc4 = findViewById(R.id.loc4)
        loc5 = findViewById(R.id.loc5)
        dateText = findViewById(R.id.dateText)
        dateButton = findViewById(R.id.dateButton)
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
        dateText.text = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(current.time)

        //set click listener
        dateButton.setOnClickListener(this)
        addButton.setOnClickListener(this)
        loc1.setOnClickListener(this)
        loc2.setOnClickListener(this)
        loc3.setOnClickListener(this)
        loc4.setOnClickListener(this)
        loc5.setOnClickListener(this)

        for (tb in dayButtons)
            tb.setOnClickListener(this)
    }

    override fun onSearchConfirmed(text: CharSequence?) {
        clearAll()
        requestSearch(text.toString())
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.dateButton -> openDatePickDialog()
            R.id.addButton -> {
                if (picked) addNewSchedule()
                else Toast.makeText(this@AddLocationActivity, "Please select a valid location!", Toast.LENGTH_SHORT).show()
            }
            R.id.toggleButton1, R.id.toggleButton2, R.id.toggleButton3, R.id.toggleButton4,
            R.id.toggleButton5, R.id.toggleButton6, R.id.toggleButton7 -> daysMode()
            R.id.loc1 -> saveLocation(1, results[0])
            R.id.loc2 -> saveLocation(2, results[1])
            R.id.loc3 -> saveLocation(3, results[2])
            R.id.loc4 -> saveLocation(4, results[3])
            R.id.loc5 -> saveLocation(5, results[4])
        }
    }

    //search for place name using naver api
    private fun requestSearch(query: String) {
        NaverRetrofit.getService().requestSearchPlace(query, location).enqueue(object: Callback<SearchPlaceData> {
            override fun onFailure(call: Call<SearchPlaceData>, t: Throwable) {
                Log.e(TAG, "Connection failed")
                Toast.makeText(this@AddLocationActivity, "Connection failed! Please check wifi!", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<SearchPlaceData>, response: Response<SearchPlaceData>) {
                if (response.body()!!.status == "OK") {
                    var len = response.body()!!.meta.totalCount - 1
                    if (len == -1) Toast.makeText(this@AddLocationActivity, "No result!", Toast.LENGTH_SHORT).show()
                    else {
                        if (len > 4) len = 4

                        results = response.body()!!.places
                        for (i in 0..len) {
                            when (i) {
                                0 -> loc1.text = results[i].name
                                1 -> loc2.text = results[i].name
                                2 -> loc3.text = results[i].name
                                3 -> loc4.text = results[i].name
                                4 -> loc5.text = results[i].name
                            }
                        }
                    }
                }
                else if (response.body()!!.status == "INVALID_REQUEST")
                    Toast.makeText(this@AddLocationActivity, "Bad request!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun clearAll() {
        loc1.setBackgroundColor(Color.WHITE)
        loc2.setBackgroundColor(Color.WHITE)
        loc3.setBackgroundColor(Color.WHITE)
        loc4.setBackgroundColor(Color.WHITE)
        loc5.setBackgroundColor(Color.WHITE)
    }

    //highlight clicked text
    private fun saveLocation(text: Int, place: SearchPlacePlaces) {
        when (text) {
            1 -> {
                loc1.setBackgroundColor(Color.GREEN)
                loc2.setBackgroundColor(Color.WHITE)
                loc3.setBackgroundColor(Color.WHITE)
                loc4.setBackgroundColor(Color.WHITE)
                loc5.setBackgroundColor(Color.WHITE)
            }
            2 -> {
                loc1.setBackgroundColor(Color.WHITE)
                loc2.setBackgroundColor(Color.GREEN)
                loc3.setBackgroundColor(Color.WHITE)
                loc4.setBackgroundColor(Color.WHITE)
                loc5.setBackgroundColor(Color.WHITE)
            }
            3 -> {
                loc1.setBackgroundColor(Color.WHITE)
                loc2.setBackgroundColor(Color.WHITE)
                loc3.setBackgroundColor(Color.GREEN)
                loc4.setBackgroundColor(Color.WHITE)
                loc5.setBackgroundColor(Color.WHITE)
            }
            4 -> {
                loc1.setBackgroundColor(Color.WHITE)
                loc2.setBackgroundColor(Color.WHITE)
                loc3.setBackgroundColor(Color.WHITE)
                loc4.setBackgroundColor(Color.GREEN)
                loc5.setBackgroundColor(Color.WHITE)
            }
            5 -> {
                loc1.setBackgroundColor(Color.WHITE)
                loc2.setBackgroundColor(Color.WHITE)
                loc3.setBackgroundColor(Color.WHITE)
                loc4.setBackgroundColor(Color.WHITE)
                loc5.setBackgroundColor(Color.GREEN)
            }
        }
        //set current place to user's place
        this.place = place
        picked = true
    }

    //open date pick dialog
    private fun openDatePickDialog() {
        val current = SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(Date())
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

    //clear all weekdays
    private fun clearDays() {
        for (tb in dayButtons) {
            tb.setChecked(false)
        }
        days_mode_set = false
    }

    //configure weekdays
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
            dateText.text = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(current.time)
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
        val isAlarmed = false
        val done = false

        //add data to the database
        val time = String.format("%04d-%02d-%02d %02d:%02d:00", year, monthOfYear+1, dayOfMonth,
            location_edit_time.hour, location_edit_time.minute)

        val newLocationItem = LocationItemData(
            place?.name ?: "Unknown",
            place?.x ?: "0", place?.y ?: "0",
            time, isAlarmed, done,
            days_mode_set, dateText.text.toString())

        locationViewModel.insert(newLocationItem)
        finish()
    }

    override fun onButtonClicked(buttonCode: Int) {}

    override fun onSearchStateChanged(enabled: Boolean) {}

}
