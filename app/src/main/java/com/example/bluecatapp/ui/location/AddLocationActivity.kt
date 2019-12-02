package com.example.bluecatapp.ui.location

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private lateinit var displayNameEdit: EditText
    private lateinit var wordCountText: TextView
    private lateinit var dateEdit: TextView
    private lateinit var timeEdit: TextView
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
    private var hour: Int = 0
    private var min: Int = 0
    private var days_mode_set: Boolean = false
    private var picked = false
    private var editmode = false

    private var intentx: String = ""
    private var intenty: String = ""
    private var intentname: String = ""
    private var intenttime: String = ""
    private var intentdays: String = ""
    private var intentdaysMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_location)
        locationViewModel = ViewModelProviders.of(this).get(LocationViewModel::class.java)
        editmode = intent.getBooleanExtra("Editmode", false)

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
        title = "Add Schedule"

        //initialize layout components
        searchBar = findViewById(R.id.searchBar)
        searchBar.setHint("Search")
        searchBar.setOnSearchActionListener(this)

        loc1 = findViewById(R.id.loc1)
        loc2 = findViewById(R.id.loc2)
        loc3 = findViewById(R.id.loc3)
        loc4 = findViewById(R.id.loc4)
        loc5 = findViewById(R.id.loc5)
        displayNameEdit = findViewById(R.id.displayNameEdit)
        wordCountText = findViewById(R.id.wordCountText)
        dateEdit = findViewById(R.id.dateEdit)
        timeEdit = findViewById(R.id.timeEdit)
        dayButtons = arrayOf(findViewById(R.id.toggleButton1),
            findViewById(R.id.toggleButton2),
            findViewById(R.id.toggleButton3),
            findViewById(R.id.toggleButton4),
            findViewById(R.id.toggleButton5),
            findViewById(R.id.toggleButton6),
            findViewById(R.id.toggleButton7))

        // set date and time to current date
        val current = Calendar.getInstance()
        year = current.get(Calendar.YEAR)
        monthOfYear = current.get(Calendar.MONTH)
        dayOfMonth = current.get(Calendar.DAY_OF_MONTH)
        hour = current.get(Calendar.HOUR_OF_DAY)
        min = current.get(Calendar.MINUTE)
        dateEdit.text = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(current.time)
        timeEdit.text = String.format("%02d:%02d", hour, min)

        // Edit Mode Initialization
        if (editmode) {
            intentname = intent.getStringExtra("name")
            intentx = intent.getStringExtra("x")
            intenty = intent.getStringExtra("y")
            intenttime = intent.getStringExtra("time")
            intentdays = intent.getStringExtra("days")
            intentdaysMode = intent.getBooleanExtra("daysMode", false)

            year = intenttime.substring(0,4).toInt()
            monthOfYear = intenttime.substring(5,7).toInt() - 1
            dayOfMonth = intenttime.substring(8, 10).toInt()
            hour = intenttime.substring(11, 13).toInt()
            min = intenttime.substring(14, 16).toInt()
            displayNameEdit.setText(intentname)
            dateEdit.text = intenttime.substring(0, 10)
            timeEdit.text = intenttime.substring(11, 16)

            if(intentdaysMode) {
                dateEdit.text = intentdays

                if(intentdays.contains("SUN")) dayButtons[0].setChecked(true)
                if(intentdays.contains("MON")) dayButtons[1].setChecked(true)
                if(intentdays.contains("TUE")) dayButtons[2].setChecked(true)
                if(intentdays.contains("WED")) dayButtons[3].setChecked(true)
                if(intentdays.contains("THU")) dayButtons[4].setChecked(true)
                if(intentdays.contains("FRI")) dayButtons[5].setChecked(true)
                if(intentdays.contains("SAT")) dayButtons[6].setChecked(true)

                for (tb in dayButtons) {
                    if(tb.isChecked) {
                        tb.setBackgroundResource(R.drawable.button_bg_round_2)
                        tb.setTextColor(Color.WHITE)
                    }
                    else {
                        tb.setBackgroundResource(R.drawable.button_bg_round)
                        tb.setTextColor(ContextCompat.getColor(this, R.color.darker_gray))
                    }
                }
            }
            days_mode_set = intentdaysMode
        }

        //set onclick listener
        displayNameEdit.setOnClickListener(this)
        dateEdit.setOnClickListener(this)
        timeEdit.setOnClickListener(this)
        for (tb in dayButtons)
            tb.setOnClickListener(this)

        displayNameEdit.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length.toString()
                val text = "$len / 17"
                wordCountText.text = text
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.add_location_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onSearchConfirmed(text: CharSequence) {
        clearAll()
        requestSearch(text.trim().toString())
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.saveLocation -> {
                if (picked or editmode) {
                    addNewSchedule()
                    //prevents fast double click
                    item.isEnabled = false
                }
                else Toast.makeText(this@AddLocationActivity, "Please select a valid location!", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.loc1 -> if (results.isNotEmpty()) saveLocation(1, results[0])
            R.id.loc2 -> if (results.size >= 2) saveLocation(2, results[1])
            R.id.loc3 -> if (results.size >= 3) saveLocation(3, results[2])
            R.id.loc4 -> if (results.size >= 4) saveLocation(4, results[3])
            R.id.loc5 -> if (results.size >= 5) saveLocation(5, results[4])
            R.id.dateEdit -> openDatePickDialog()
            R.id.timeEdit -> openTimePickDialog()
            R.id.toggleButton1, R.id.toggleButton2, R.id.toggleButton3, R.id.toggleButton4,
            R.id.toggleButton5, R.id.toggleButton6, R.id.toggleButton7 -> daysMode()
        }
    }

    //search for place name using Naver api
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
                        //convert len to array index length; max is 4
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
                        loc1.setOnClickListener(this@AddLocationActivity)
                        loc2.setOnClickListener(this@AddLocationActivity)
                        loc3.setOnClickListener(this@AddLocationActivity)
                        loc4.setOnClickListener(this@AddLocationActivity)
                        loc5.setOnClickListener(this@AddLocationActivity)
                    }
                }
                else if (response.body()!!.status == "INVALID_REQUEST")
                    Toast.makeText(this@AddLocationActivity, "Bad request!", Toast.LENGTH_SHORT).show()
                else if (response.body()!!.status == "SYSTEM_ERROR")
                    Toast.makeText(this@AddLocationActivity, "System Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    //clear all highlight
    private fun clearAll() {
        loc1.setBackgroundColor(Color.WHITE)
        loc2.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey))
        loc3.setBackgroundColor(Color.WHITE)
        loc4.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey))
        loc5.setBackgroundColor(Color.WHITE)
    }

    //highlight clicked text
    private fun saveLocation(text: Int, place: SearchPlacePlaces) {
        when (text) {
            1 -> {
                loc1.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                loc2.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey))
                loc3.setBackgroundColor(Color.WHITE)
                loc4.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey))
                loc5.setBackgroundColor(Color.WHITE)
            }
            2 -> {
                loc1.setBackgroundColor(Color.WHITE)
                loc2.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                loc3.setBackgroundColor(Color.WHITE)
                loc4.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey))
                loc5.setBackgroundColor(Color.WHITE)
            }
            3 -> {
                loc1.setBackgroundColor(Color.WHITE)
                loc2.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey))
                loc3.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                loc4.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey))
                loc5.setBackgroundColor(Color.WHITE)
            }
            4 -> {
                loc1.setBackgroundColor(Color.WHITE)
                loc2.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey))
                loc3.setBackgroundColor(Color.WHITE)
                loc4.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                loc5.setBackgroundColor(Color.WHITE)
            }
            5 -> {
                loc1.setBackgroundColor(Color.WHITE)
                loc2.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey))
                loc3.setBackgroundColor(Color.WHITE)
                loc4.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey))
                loc5.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
            }
        }
        //set current place to user's place
        this.place = place
        displayNameEdit.setText(place.name)
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
                dateEdit.text = String.format("%04d-%02d-%02d", year, monthOfYear+1, dayOfMonth)
                clearDays()
            }
        }, Integer.parseInt(current.substring(0,4)), Integer.parseInt(current.substring(4,6))-1, Integer.parseInt(current.substring(6,8)) )
        dialog.show()
    }

    //open time pick dialog
    private fun openTimePickDialog() {
        val cal = Calendar.getInstance()
        val dialog = TimePickerDialog(this, object: TimePickerDialog.OnTimeSetListener {
            override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
                this@AddLocationActivity.hour = hourOfDay
                this@AddLocationActivity.min = minute
                timeEdit.text = String.format("%02d:%02d", hourOfDay, minute)
            }
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
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
                tb.setBackgroundResource(R.drawable.button_bg_round_2)
                tb.setTextColor(Color.WHITE)
                str = str + tb.textOn + ","
            }
            else {
                tb.setBackgroundResource(R.drawable.button_bg_round)
                tb.setTextColor(ContextCompat.getColor(this, R.color.darker_gray))
            }
        }

        // if there is no day checked
        if(str == "") {
            val current = Calendar.getInstance()
            year = current.get(Calendar.YEAR)
            monthOfYear = current.get(Calendar.MONTH)
            dayOfMonth = current.get(Calendar.DAY_OF_MONTH)
            dateEdit.text = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(current.time)
            days_mode_set = false
        }
        // if there is at least one day checked
        else {
            dateEdit.text = str.substring(0, str.length-1)
            days_mode_set = true

            val calendar = Calendar.getInstance()
            var dayOfToday_encoded = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            var dayOfToday:String = ""
            var count = 0                       // To prevent infinite loop

            // Iteration for get closest time
            while(true) {
                dayOfToday = when (dayOfToday_encoded) {
                    1 -> "SUN"
                    2 -> "MON"
                    3 -> "TUE"
                    4 -> "WED"
                    5 -> "THU"
                    6 -> "FRI"
                    7 -> "SAT"
                    else -> ""
                }
                if (dateEdit.text.contains(dayOfToday) || count > 10)
                    break
                dayOfToday_encoded = dayOfToday_encoded + 1
                calendar.add(Calendar.DATE, 1)
                if(dayOfToday_encoded > 7)
                    dayOfToday_encoded = 1
                count++
            }
            year = calendar.get(Calendar.YEAR)
            monthOfYear = calendar.get(Calendar.MONTH)
            dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        }
    }

    // Save data to database
    private fun addNewSchedule() {
        val isAlarmed = false
        val done = false

        //add data to the database
        val time = String.format("%04d-%02d-%02d %02d:%02d:00", year, monthOfYear+1, dayOfMonth, hour, min)

        val newLocationItem =
            if (!picked and editmode)
                LocationItemData(displayNameEdit.text.toString(),
                intentx, intenty, time, isAlarmed, done,
                days_mode_set, dateEdit.text.toString())
            else
                LocationItemData(displayNameEdit.text.toString(),
                place?.x ?: "0", place?.y ?: "0", time, isAlarmed, done,
                days_mode_set, dateEdit.text.toString())

        if (editmode) {
            val id = intent.getIntExtra("Id", 0)
            locationViewModel.editLocationItem(newLocationItem.name,
                newLocationItem.x, newLocationItem.y, newLocationItem.time,
                newLocationItem.isAlarmed, newLocationItem.done,
                newLocationItem.daysMode, newLocationItem.days, id)
        }
        else {
            locationViewModel.insert(newLocationItem)
        }

        finish()
    }

    override fun onButtonClicked(buttonCode: Int) {}

    override fun onSearchStateChanged(enabled: Boolean) {}

}
