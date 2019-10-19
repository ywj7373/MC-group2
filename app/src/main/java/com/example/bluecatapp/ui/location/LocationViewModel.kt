package com.example.bluecatapp.ui.location

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val locationData : LiveData<Location> = TrackLocation(application)
    fun getLocationData() = locationData
}