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
}

@Dao
interface CurrentLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(currentLocation: CurrentLocationData)

    @Query("SELECT * FROM current_location")
    fun getCurrentLocation(): LiveData<CurrentLocationData>
}

@Dao
interface AlarmTimeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(alarmTime: AlarmTimeData)

    @Query("SELECT * FROM alarm_time")
    fun getAlarmTime(): AlarmTimeData
}