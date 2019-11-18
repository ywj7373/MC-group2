package com.example.bluecatapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "location_items")
data class LocationItemData(

    @field:SerializedName("name")
    val name: String,

    @field:SerializedName("x")
    val x: String,

    @field:SerializedName("y")
    val y: String,

    @field:SerializedName("time")
    val time: String,

    @field:SerializedName("isAlarmed")
    val isAlarmed: Boolean,

    @field:SerializedName("done")
    val done: Boolean,

    @field:SerializedName("daysMode")
    val daysMode: Boolean,

    @field:SerializedName("days")
    val days: String

)
{
    @PrimaryKey(autoGenerate = true)
    @field:SerializedName("id")
    var id: Int = 0
}

@Entity(tableName = "current_location")
data class CurrentLocationData(
    @field:SerializedName("latitude")
    val latitude: Double,

    @field:SerializedName("longitude")
    val longitude: Double
)
{
    @PrimaryKey
    @field:SerializedName("id")
    var id: Int = 0
}

@Entity(tableName = "alarm_time")
data class AlarmTimeData(
    @field:SerializedName("time")
    val time: String
)
{
    @PrimaryKey
    @field:SerializedName("id")
    var id: Int = 0
}

@Entity(tableName = "stats")
data class StatsData(
    @field:SerializedName("ontime")
    val ontime: Int,

    @field:SerializedName("absent")
    val absent: Int
)
{
    @PrimaryKey
    @field:SerializedName("id")
    var id: Int = 0
}