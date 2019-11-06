package com.example.bluecatapp.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface LocationItemDao {

    @Insert
    fun insert(locationItem: LocationItemData)

    @Query("DELETE FROM location_items")
    fun deleteAllLocationItems()

    @Query("SELECT * FROM location_items ")
    fun getAllLocationItems(): LiveData<List<LocationItemData>>

    @Query("SELECT * FROM location_items WHERE done == 0 ORDER BY dateTime(time) ASC Limit 1")
    fun getPriorityDestination(): LocationItemData

    @Query("UPDATE location_items SET timeToDest = :value1 WHERE id = :value2")
    fun updateEstimatedTime(value1: String, value2: Int)

    @Query("UPDATE location_items SET isAlarmed = :value1 WHERE id = :value2")
    fun updateIsAlarm(value1: Boolean, value2: Int)

    @Query("UPDATE location_items SET done = :value1 WHERE id = :value2")
    fun updateDone(value1: Boolean, value2: Int)
}

@Dao
interface CurrentLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(currentLocation: CurrentLocationData)

    @Query("SELECT * FROM current_location")
    fun getCurrentLocation(): LiveData<CurrentLocationData>
}