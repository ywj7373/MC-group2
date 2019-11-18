package com.example.bluecatapp.data

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.LiveData

//connect database with livedata with repository
class LocationRepository(application: Application) {

    private var locationItemDao: LocationItemDao
    private var currentLocationDao: CurrentLocationDao
    private var alarmTimeDao: AlarmTimeDao
    private var statsDao: StatsDao
    private var allLocationItems: LiveData<List<LocationItemData>>

    init {
        val locationItemDatabase: LocationItemDatabase = LocationItemDatabase.getInstance(application.applicationContext)!!
        val currentLocationDatabase: CurrentLocationDatabase = CurrentLocationDatabase.getInstance(application.applicationContext)!!
        val alarmTimeDatabase: AlarmTimeDatabase = AlarmTimeDatabase.getInstance(application.applicationContext)!!
        val statsDatabase: StatsDatabase = StatsDatabase.getInstance(application.applicationContext)!!
        locationItemDao = locationItemDatabase.locationItemDao()
        currentLocationDao = currentLocationDatabase.currentLocationDao()
        alarmTimeDao = alarmTimeDatabase.alarmTimeDao()
        statsDao = statsDatabase.statsDao()
        allLocationItems = locationItemDao.getAllLocationItems()
    }

    fun insertLocationItem(locationItem: LocationItemData) {
        InsertLocationItemAsyncTask(locationItemDao).execute(locationItem)
    }

    fun insertCurrentLocation(currentLocation: CurrentLocationData) {
        InsertCurrentLocationAsyncTask(currentLocationDao).execute(currentLocation)
    }

    fun deleteAllLocationItems() {
        DeleteAllLocationITemAsyncTask(locationItemDao).execute()
    }

    fun getAllLocationItems(): LiveData<List<LocationItemData>> {
        return allLocationItems
    }

    fun getCurrentLocation(): LiveData<CurrentLocationData> {
        return currentLocationDao.getCurrentLocation()
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
        locationItemDao.updateAllNotDoneDays()
    }

    fun insertAlarmTime(alarmTime: AlarmTimeData) {
        InsertAlarmTimeAsyncTask(alarmTimeDao).execute(alarmTime)
    }

    fun getAlarmTime(): AlarmTimeData {
        return alarmTimeDao.getAlarmTime()
    }

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


    fun deleteLocationItem(id: Int) {
        DeleteLocationITemAsyncTask(locationItemDao, id).execute()
    }

    fun editLocationItem(name: String, x: String, y:String, time:String, isAlarmed:Boolean, done:Boolean, daysMode:Boolean, days:String, id: Int) {
        EditLocationItemAsyncTask(locationItemDao, name, x, y, time, isAlarmed, done, daysMode, days, id).execute()
    }

    private class InsertLocationItemAsyncTask(locationItemDao: LocationItemDao) : AsyncTask<LocationItemData, Unit, Unit>() {
        val locationItemDao = locationItemDao

        override fun doInBackground(vararg p0: LocationItemData?) {
            locationItemDao.insert(p0[0]!!)
        }
    }

    private class InsertCurrentLocationAsyncTask(currentLocationDao: CurrentLocationDao) : AsyncTask<CurrentLocationData, Unit, Unit>() {
        val currentLocationDao = currentLocationDao

        override fun doInBackground(vararg params: CurrentLocationData?) {
            currentLocationDao.insert(params[0]!!)
        }
    }

    private class InsertAlarmTimeAsyncTask(alarmTimeDao: AlarmTimeDao) : AsyncTask<AlarmTimeData, Unit, Unit>() {
        val alarmTimeDao = alarmTimeDao

        override fun doInBackground(vararg params: AlarmTimeData?) {
            alarmTimeDao.insert(params[0]!!)
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
}