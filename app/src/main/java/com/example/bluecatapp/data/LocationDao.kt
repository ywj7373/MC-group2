package com.example.bluecatapp.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocationItemDao {

    @Insert
    fun insert(locationItem: LocationItemData)

    @Query("DELETE FROM location_items")
    fun deleteAllLocationItems()

    @Query("SELECT * FROM location_items ")
    fun getAllLocationItems(): LiveData<List<LocationItemData>>

}

@Dao
interface CurrentLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(currentLocation: CurrentLocationData)

    @Query("SELECT * FROM current_location")
    fun getCurrentLocation(): CurrentLocationData
}