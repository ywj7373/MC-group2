package com.example.bluecatapp.ui.location

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.bluecatapp.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
            location = it.longitude.toString() + "," + it.latitude.toString()
            Log.e(TAG, location)
            NaverRetrofit.getService().requestReverseGeocode(location).enqueue(object : Callback<CoordToAddrData>{
                override fun onFailure(call: Call<CoordToAddrData>, t: Throwable) {
                    Log.e(TAG, t.message.toString())
                }

                override fun onResponse(call: Call<CoordToAddrData>, response: Response<CoordToAddrData>) {
                    Log.e(TAG, response.body().toString())
                    if (response.body()!!.status.code == 0)
                        startLocText.text = response.body()!!.results[0].land.addition0.value
                }

            })
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
            R.id.dateText -> {}
            R.id.timeText -> {}
            R.id.addButton -> {}
            R.id.cancelButton -> {}
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
            startLocText.text = place.name
        }
        else {
            endLocText.text = place.name
        }
    }

}

interface OnButtonClick {
    fun onDialogClickListener(isStart: Int, place: SearchPlacePlaces)
}