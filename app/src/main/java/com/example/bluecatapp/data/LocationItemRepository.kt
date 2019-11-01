package com.example.bluecatapp.data

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.LiveData

//connect database with livedata with repository
class LocationItemRepository(application: Application) {

    private var locationItemDao: LocationItemDao

    private var allLocationItems: LiveData<List<LocationItem>>

    init {
        val database: LocationItemDatabase = LocationItemDatabase.getInstance(application.applicationContext)!!
        locationItemDao = database.locationItemDao()
        allLocationItems = locationItemDao.getAllLocationItems()
    }

    fun insert(locationItem: LocationItem) {
        InsertTodoAsyncTask(locationItemDao).execute(locationItem)
    }

    fun deleteAllLocationItems() {
        DeleteAllTodosAsyncTask(locationItemDao).execute()
    }

    fun getAllLocationItems(): LiveData<List<LocationItem>> {
        return allLocationItems
    }

    private class InsertTodoAsyncTask(locationItemDao: LocationItemDao) : AsyncTask<LocationItem, Unit, Unit>() {
        val locationItemDao = locationItemDao

        override fun doInBackground(vararg p0: LocationItem?) {
            locationItemDao.insert(p0[0]!!)
        }
    }

    private class DeleteAllTodosAsyncTask(val locationItemDao: LocationItemDao) : AsyncTask<Unit, Unit, Unit>() {

        override fun doInBackground(vararg p0: Unit?) {
            locationItemDao.deleteAllLocationItems()
        }
    }
}