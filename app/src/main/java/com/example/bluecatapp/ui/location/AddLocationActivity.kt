package com.example.bluecatapp.ui.location

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.bluecatapp.R
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapReverseGeoCoder

class AddLocationActivity: AppCompatActivity() {
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var titleEdit: EditText
    private lateinit var startLocText: TextView
    private lateinit var endLocText: TextView
    private lateinit var dateText: TextView
    private lateinit var timeText: TextView
    private lateinit var cancelButton: Button
    private lateinit var addButton: Button

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
        cancelButton = findViewById(R.id.cancelButton)
        addButton = findViewById(R.id.addButton)

        var mapPoint = MapPoint.mapPointWithGeoCoord(37.53737528, 127.00557633)
        val reverseGeoCoder = MapReverseGeoCoder(
            "b1237dcf2b12fab3b9ef06d50f04990d",
            mapPoint,
            reverseGeoCodingResultListener,
            this
        )
        reverseGeoCoder.startFindingAddress()

        //observe current location change
        locationViewModel.getLocationData().observe(this, Observer {
            /*
            latAndLong.text = it.longitude.toString() + " " + it.latitude.toString()
            latitude = it.latitude
            longitude = it.longitude*/
        })

        startLocText.setOnClickListener() {

        }

        endLocText.setOnClickListener() {

        }

        dateText.setOnClickListener() {

        }

        timeText.setOnClickListener() {

        }

        addButton.setOnClickListener() {

        }

        cancelButton.setOnClickListener() {

        }
    }


    private val reverseGeoCodingResultListener =
        object : MapReverseGeoCoder.ReverseGeoCodingResultListener {
            override fun onReverseGeoCoderFoundAddress(p0: MapReverseGeoCoder?, p1: String?) {
                println(p1)
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onReverseGeoCoderFailedToFindAddress(p0: MapReverseGeoCoder?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }
}