package com.example.bluecatapp.ui.location

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.bluecatapp.data.LocationItemRepository
import com.example.bluecatapp.data.LocationItem

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    //track changes in current location
    private val locationData : LiveData<Location> = TrackLocation(application)
    fun getLocationData() = locationData

    //track changes in the database
    private var repository: LocationItemRepository = LocationItemRepository(application)
    private var allLocationItems: LiveData<List<LocationItem>> = repository.getAllLocationItems()

    fun insert(locationItem: LocationItem) {
        repository.insert(locationItem)
    }

    fun deleteAllLocationItems() {
        repository.deleteAllLocationItems()
    }

    fun getAllLocationItems(): LiveData<List<LocationItem>> {
        return allLocationItems
    }
}