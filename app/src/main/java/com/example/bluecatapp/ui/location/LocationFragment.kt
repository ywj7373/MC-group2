package com.example.bluecatapp.ui.location

import android.Manifest
import android.app.Application
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.bluecatapp.R
import com.odsay.odsayandroidsdk.API
import com.odsay.odsayandroidsdk.ODsayData
import com.odsay.odsayandroidsdk.ODsayService
import com.odsay.odsayandroidsdk.OnResultCallbackListener
import kotlinx.android.synthetic.main.fragment_location.*
import org.json.JSONException
import org.json.JSONObject


class LocationFragment : Fragment() {

    private lateinit var locationViewModel: LocationViewModel
    private val PERMISSION_ID = 270
    private lateinit var latAndLong: TextView
    private lateinit var estimatedTime: TextView
    private lateinit var odsayService : ODsayService

    private var latitude : Double = 0.0
    private var longitude : Double = 0.0

    // Views for testing
    private lateinit var inputEX: EditText
    private lateinit var inputEY: EditText
    private lateinit var calcButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //initialize
        val root = inflater.inflate(R.layout.fragment_location, container, false)
        latAndLong = root.findViewById(R.id.text_latAndLong)
        estimatedTime = root.findViewById(R.id.text_estimatedTime)
        locationViewModel = ViewModelProviders.of(this).get(LocationViewModel::class.java)

        //For testing
        //////////////////////////////////////////////////////////////////////////////////
        inputEX = root.findViewById(R.id.editText_EX)
        inputEY = root.findViewById(R.id.editText_EY)
        calcButton = root.findViewById(R.id.calc_travel_time_button)
        calcButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                Log.d("Mouse clicked", "clicked!")
                estimateTravelTime(longitude.toString(), latitude.toString(),inputEX.text.toString(),inputEY.text.toString())
            }
        } )
        ////////////////////////////////////////////////////////////////////

        //Initialize ODsayService
        odsayService = ODsayService.init(requireContext(), getString(R.string.odsay_key))
        Log.d("ODSAY api key", getString(R.string.odsay_key))
        odsayService.setConnectionTimeout(5000)
        odsayService.setReadTimeout(5000)

        //Start receiving current location
        getCurrentLocation()
        return root
    }

    private fun getCurrentLocation() {
        if (checkLocationPermissions()) {
            if (isLocationEnabled()) {
                locationViewModel.getLocationData().observe(this, Observer {
                    latAndLong.text = it.longitude.toString() + " " + it.latitude.toString()
                    latitude = it.latitude
                    longitude = it.longitude
                })
            }
            else {
                Toast.makeText(requireActivity(), "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        else {
            requestLocationPermission()
        }
    }

    //Check if the location tracker is enabled in the setting
    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    //Check if the user needs permission for location
    private fun checkLocationPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    //Request for permission for location
    private fun requestLocationPermission() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale")
        }
        else {
            Log.i(TAG, "Requesting Permission")
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_ID)
        }
    }

    //Called after the user allows or denies our requested permission
    override fun onRequestPermissionsResult(requestCode: Int, permission: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getCurrentLocation()
            }
        }
    }


    private val onEstimateTimeResultCallbackListener = object : OnResultCallbackListener {

        override fun onSuccess(odsayData: ODsayData?, api: API?) {
            Log.d("ODSAY onSuccess called", "Good")
            try {
                if (api == API.SEARCH_PUB_TRANS_PATH) {
                    val inquiryResult = (odsayData!!.getJson().getJSONObject("result").getJSONArray("path").get(0) as JSONObject).getJSONObject("info")
                    estimatedTime.text = ( inquiryResult.toString()
                            )
                    Log.d("Parsed ", estimatedTime.text.toString())
                    Log.d("JSONObject Parsing", "Successful")
                    //estimatedTime.text = inquiryResult.getInt("totalTime").toString()
                }
            } catch (e: JSONException) {
                Log.d("JSONException Occurred", "JSONException")
                e.printStackTrace()
            }
        }

        override fun onError(i: Int, s: String?, api: API?) {
            Log.d("ODSAY onError called", (i.toString()+", "+s) )
        }
    }


    private fun estimateTravelTime(sx: String, sy: String, ex: String, ey: String) {
        odsayService.requestSearchPubTransPath(sx, sy, ex, ey, "0", "0", "0", onEstimateTimeResultCallbackListener)
        //odsayService.requestBusStationInfo("107475", onEstimateTimeResultCallbackListener)
    }
}