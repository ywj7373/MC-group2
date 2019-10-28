package com.example.bluecatapp.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LocationItemDao {

    @Insert
    fun insert(locationItem: LocationItem)

    @Query("DELETE FROM location_items")
    fun deleteAllLocationItems()

    @Query("SELECT * FROM location_items ")
    fun getAllLocationItems(): LiveData<List<LocationItem>>

}