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

    @Query("SELECT * FROM (SELECT * FROM location_items WHERE daysMode == 0 AND done == 0 UNION SELECT * FROM location_items WHERE daysMode == 1 AND days LIKE :value1 AND done == 0) ORDER BY time(time) ASC LIMIT 1")
    fun getPriorityDestination(value1: String): LiveData<LocationItemData>

    @Query("UPDATE location_items SET isAlarmed = :value1 WHERE id = :value2")
    fun updateIsAlarm(value1: Boolean, value2: Int)

    @Query("UPDATE location_items SET done = :value1 WHERE id = :value2")
    fun updateDone(value1: Boolean, value2: Int)

    @Query("UPDATE location_items SET done = 0 WHERE daysMode == 1")
    fun updateAllNotDoneDays()

    @Query("DELETE FROM location_items WHERE id = :value1")
    fun deleteLocationItem(value1: Int)

    @Query("UPDATE location_items SET name = :value1, x = :value2, y = :value3, time = :value4, isAlarmed = :value5, done = :value6, daysMode = :value7, days = :value8 WHERE id = :value9")
    fun editLocationItem(value1: String, value2: String, value3: String, value4: String, value5: Boolean, value6: Boolean, value7: Boolean, value8: String, value9: Int)
}

@Dao
interface CurrentLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(currentLocation: CurrentLocationData)

    @Query("SELECT * FROM current_location")
    fun getCurrentLocation(): LiveData<CurrentLocationData>
}

@Dao
interface TravelTimeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(travelTime: TravelTimeData)

    @Query("SELECT * FROM travel_time")
    fun getTravelTime(): TravelTimeData
}

@Dao
interface StatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(statsData: StatsData)

    @Query("SELECT * FROM stats")
    fun getStatsData(): LiveData<StatsData>

    @Query("UPDATE stats SET ontime = ontime + 1")
    fun increaseOntime()

    @Query("UPDATE stats SET absent = absent + 1")
    fun increaseAbsent()
}

@Dao
interface DateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dateData: DateData)

    @Query("SELECT * FROM mcurrent_date_table")
    fun getCurrentDate(): DateData
}