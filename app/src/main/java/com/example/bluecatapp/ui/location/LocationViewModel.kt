package com.example.bluecatapp.ui.location

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.bluecatapp.data.CurrentLocationData
import com.example.bluecatapp.data.LocationRepository
import com.example.bluecatapp.data.LocationItemData

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    //track changes in the database
    private var repository: LocationRepository = LocationRepository(application)
    private var allLocationItems: LiveData<List<LocationItemData>> = repository.getAllLocationItems()
    private var currentLocation: LiveData<CurrentLocationData> = repository.getCurrentLocation()

    fun insert(locationItem: LocationItemData) {
        repository.insertLocationItem(locationItem)
    }

    fun deleteAllLocationItems() {
        repository.deleteAllLocationItems()
    }

    fun getAllLocationItems(): LiveData<List<LocationItemData>> {
        return allLocationItems
    }

    fun getCurrentLocation(): LiveData<CurrentLocationData> {
        return currentLocation
    }
}