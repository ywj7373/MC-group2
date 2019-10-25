package com.example.bluecatapp.ui.location

import android.content.ContentValues
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

class TrackLocation(context: Context) : LiveData<Location>() {
    private var fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    //when app is closed, close location update
    override fun onInactive() {
        super.onInactive()
        fusedLocationClient.removeLocationUpdates(mLocationCallback)
    }

    //start receiving location update whenever app is on
    override fun onActive() {
        super.onActive()
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.also {
                value = it
                Log.d(ContentValues.TAG, it.latitude.toString())
                Log.d(ContentValues.TAG, it.longitude.toString())
            }
        }
        requestLocationData()
    }

    //request current location every 5 to 10 seconds
    private fun requestLocationData() {
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 10000
        mLocationRequest.fastestInterval = 5000
        //mLocationRequest.numUpdates = 1

        fusedLocationClient!!.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    //callback method that gets lastLocation
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var mLastLocation: Location = locationResult.lastLocation
            value = mLastLocation
            Log.d(ContentValues.TAG, mLastLocation.latitude.toString())
            Log.d(ContentValues.TAG, mLastLocation.longitude.toString())
        }
    }
}