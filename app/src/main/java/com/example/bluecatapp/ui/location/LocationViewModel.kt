package com.example.bluecatapp.ui.location

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bluecatapp.data.*
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
    private var statsData: LiveData<StatsData> = repository.getStats()
    private var travelTime: LiveData<TravelTimeData> = repository.getTravelTime()

    fun getNextSchedule(): LiveData<LocationItemData> {
        return nextSchedule
    }

    fun insert(locationItem: LocationItemData) {
        repository.insertLocationItem(locationItem)
    }

    fun getAllLocationItems(): LiveData<List<LocationItemData>> {
        return allLocationItems
    }

    fun getCurrentLocation(): LiveData<CurrentLocationData> {
        return currentLocation
    }

    fun getTravelTime(): LiveData<TravelTimeData> {
        return travelTime
    }

    fun deleteLocationItem(id: Int) {
        repository.deleteLocationItem(id)
    }

    fun editLocationItem(name: String, x: String, y:String, time:String, isAlarmed:Boolean, done:Boolean, daysMode:Boolean, days:String, id: Int) {
        repository.editLocationItem(name, x, y, time, isAlarmed, done, daysMode, days, id)
    }

    fun getStats(): LiveData<StatsData> {
        return statsData
    }

    fun resetStats() {
        repository.resetStats()
    }

}