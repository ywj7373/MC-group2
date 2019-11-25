package com.example.bluecatapp.data

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import java.text.SimpleDateFormat
import java.util.*

//connect database with livedata with repository
class LocationRepository(application: Application) {

    private var locationItemDao: LocationItemDao
    private var currentLocationDao: CurrentLocationDao
    private var travelTimeDao: TravelTimeDao
    private var statsDao: StatsDao
    private var dateDao: DateDao
    private var allLocationItems: LiveData<List<LocationItemData>>

    init {
        val locationItemDatabase: LocationItemDatabase = LocationItemDatabase.getInstance(application.applicationContext)!!
        val currentLocationDatabase: CurrentLocationDatabase = CurrentLocationDatabase.getInstance(application.applicationContext)!!
        val travelTimeDatabase: TravelTimeDatabase = TravelTimeDatabase.getInstance(application.applicationContext)!!
        val statsDatabase: StatsDatabase = StatsDatabase.getInstance(application.applicationContext)!!
        val dateDatabase: DateDatabase = DateDatabase.getInstance(application.applicationContext)!!

        locationItemDao = locationItemDatabase.locationItemDao()
        currentLocationDao = currentLocationDatabase.currentLocationDao()
        travelTimeDao = travelTimeDatabase.travelTimeDao()
        statsDao = statsDatabase.statsDao()
        dateDao = dateDatabase.dateDao()
        allLocationItems = locationItemDao.getAllLocationItems()
    }

    //Location Item Database functions
    fun insertLocationItem(locationItem: LocationItemData) {
        InsertLocationItemAsyncTask(locationItemDao).execute(locationItem)
    }

    fun deleteAllLocationItems() {
        DeleteAllLocationITemAsyncTask(locationItemDao).execute()
    }

    fun getAllLocationItems(): LiveData<List<LocationItemData>> {
        return allLocationItems
    }

    fun getPriorityDestination(dayOfWeek: String): LiveData<LocationItemData> {
        return locationItemDao.getPriorityDestination(dayOfWeek)
    }

    fun updateIsAlarmed(toggle: Boolean, id: Int) {
        locationItemDao.updateIsAlarm(toggle, id)
    }

    fun updateDone(toggle: Boolean, id: Int) {
        locationItemDao.updateDone(toggle, id)
    }

    fun updateAllNotDoneDays() {
        UpdateAllNotDoneDaysAsyncTask(locationItemDao).execute()
    }

    fun deleteLocationItem(id: Int) {
        DeleteLocationITemAsyncTask(locationItemDao, id).execute()
    }

    fun updateToTodayDateDays(today: String, day: String) {
        UpdateToTodayDateAsyncTask(locationItemDao, today, day).execute()
    }

    fun editLocationItem(name: String, x: String, y:String, time:String, isAlarmed:Boolean, done:Boolean, daysMode:Boolean, days:String, id: Int) {
        EditLocationItemAsyncTask(locationItemDao, name, x, y, time, isAlarmed, done, daysMode, days, id).execute()
    }

    //Current Location Database functions
    fun getCurrentLocation(): LiveData<CurrentLocationData> {
        return currentLocationDao.getCurrentLocation()
    }

    fun insertCurrentLocation(currentLocation: CurrentLocationData) {
        InsertCurrentLocationAsyncTask(currentLocationDao).execute(currentLocation)
    }

    //Travel Time Database functions
    fun insertTravelTime(travelTime: TravelTimeData) {
        InsertTravelTimeAsyncTask(travelTimeDao).execute(travelTime)
    }

    fun getTravelTime(): TravelTimeData {
        return travelTimeDao.getTravelTime()
    }

    //Statistics Database functions
    fun resetStats() {
        ResetStatsAsyncTask(statsDao).execute()
    }

    fun getStats(): LiveData<StatsData> {
        return statsDao.getStatsData()
    }

    fun increaseAbsent() {
        statsDao.increaseAbsent()
    }

    fun increaseOntime() {
        statsDao.increaseOntime()
    }

    //Current Date Database functions
    fun getCurrentDate(): DateData {
        return dateDao.getCurrentDate()
    }
    fun updateCurrentDate() {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd").format(Date())
        val dateData = DateData(timeStamp)
        dateDao.insert(dateData)
    }

    private class InsertLocationItemAsyncTask(locationItemDao: LocationItemDao) : AsyncTask<LocationItemData, Unit, Unit>() {
        val locationItemDao = locationItemDao

        override fun doInBackground(vararg p0: LocationItemData?) {
            locationItemDao.insert(p0[0]!!)
        }
    }

    private class UpdateToTodayDateAsyncTask(val locationItemDao: LocationItemDao, val today: String, val day: String) : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg p0: Unit?) {
            locationItemDao.updateToTodayDateDays(today, day)
        }
    }

    private class InsertCurrentLocationAsyncTask(currentLocationDao: CurrentLocationDao) : AsyncTask<CurrentLocationData, Unit, Unit>() {
        val currentLocationDao = currentLocationDao

        override fun doInBackground(vararg params: CurrentLocationData?) {
            currentLocationDao.insert(params[0]!!)
        }
    }

    private class InsertTravelTimeAsyncTask(travelTimeDao: TravelTimeDao) : AsyncTask<TravelTimeData, Unit, Unit>() {
        val travelTimeDao = travelTimeDao

        override fun doInBackground(vararg params: TravelTimeData?) {
            travelTimeDao.insert(params[0]!!)
        }
    }

    private class DeleteAllLocationITemAsyncTask(val locationItemDao: LocationItemDao) : AsyncTask<Unit, Unit, Unit>() {

        override fun doInBackground(vararg p0: Unit?) {
            locationItemDao.deleteAllLocationItems()
        }
    }

    private class DeleteLocationITemAsyncTask(val locationItemDao: LocationItemDao, var id: Int) : AsyncTask<Unit, Unit, Unit>() {

        override fun doInBackground(vararg p0: Unit?) {
            locationItemDao.deleteLocationItem(id)
        }
    }

    private class EditLocationItemAsyncTask(val locationItemDao: LocationItemDao, var name: String, var x: String, var y:String, var time:String, var isAlarmed:Boolean, var done:Boolean, var daysMode:Boolean, var days:String, var id: Int) : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg p0: Unit?) {
            locationItemDao.editLocationItem(name, x, y, time, isAlarmed, done, daysMode, days, id)
        }
    }

    private class ResetStatsAsyncTask(val statsDao: StatsDao) : AsyncTask<Unit, Unit, Unit>() {

        override fun doInBackground(vararg p0: Unit?) {
            statsDao.insert(StatsData(0,0))
        }
    }

    private class UpdateAllNotDoneDaysAsyncTask(val locationItemDao: LocationItemDao) : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg  p0: Unit?) {
            locationItemDao.updateAllNotDoneDays()
        }
    }
}