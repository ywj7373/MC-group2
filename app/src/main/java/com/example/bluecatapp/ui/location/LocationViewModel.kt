package com.example.bluecatapp.ui.location

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bluecatapp.data.CurrentLocationData
import com.example.bluecatapp.data.LocationRepository
import com.example.bluecatapp.data.LocationItemData
import java.util.*

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val dayOfToday = when(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
        1 -> "%SUN%"
        2 -> "%MON%"
        3 -> "%TUE%"
        4 -> "%WED%"
        5 -> "%THU%"
        6 -> "%FRI%"
        7 -> "%SAT%"
        else -> ""
    }

    //track changes in the database
    private var repository: LocationRepository = LocationRepository(application)
    private var allLocationItems: LiveData<List<LocationItemData>> = repository.getAllLocationItems()
    private var currentLocation: LiveData<CurrentLocationData> = repository.getCurrentLocation()
    private var nextSchedule: LiveData<LocationItemData> = repository.getPriorityDestination(dayOfToday)

    fun getNextSchedule(): LiveData<LocationItemData> {
        return nextSchedule
    }

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