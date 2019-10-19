package com.example.bluecatapp.ui.location

import android.Manifest
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
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.bluecatapp.R

class LocationFragment : Fragment() {

    private lateinit var locationViewModel: LocationViewModel
    private val PERMISSION_ID = 270
    private lateinit var latAndLong: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //initialize
        val root = inflater.inflate(R.layout.fragment_location, container, false)
        latAndLong = root.findViewById(R.id.text_latAndLong)
        locationViewModel = ViewModelProviders.of(this).get(LocationViewModel::class.java)

        //Start receiving current location
        getCurrentLocation()
        return root
    }

    private fun getCurrentLocation() {
        if (checkLocationPermissions()) {
            if (isLocationEnabled()) {
                locationViewModel.getLocationData().observe(this, Observer {
                    latAndLong.text = it.longitude.toString() + " " + it.latitude.toString()
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
}