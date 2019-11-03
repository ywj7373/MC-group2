package com.example.bluecatapp.data

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.LiveData

//connect database with livedata with repository
class LocationRepository(application: Application) {

    private var locationItemDao: LocationItemDao
    private var currentLocationDao: CurrentLocationDao
    private var allLocationItems: LiveData<List<LocationItemData>>

    init {
        val locationItemDatabase: LocationItemDatabase = LocationItemDatabase.getInstance(application.applicationContext)!!
        val currentLocationDatabase: CurrentLocationDatabase = CurrentLocationDatabase.getInstance(application.applicationContext)!!
        locationItemDao = locationItemDatabase.locationItemDao()
        currentLocationDao = currentLocationDatabase.currentLocationDao()
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

    fun getCurrentLocation(): CurrentLocationData {
        return currentLocationDao.getCurrentLocation()
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

    private class DeleteAllLocationITemAsyncTask(val locationItemDao: LocationItemDao) : AsyncTask<Unit, Unit, Unit>() {

        override fun doInBackground(vararg p0: Unit?) {
            locationItemDao.deleteAllLocationItems()
        }
    }
}