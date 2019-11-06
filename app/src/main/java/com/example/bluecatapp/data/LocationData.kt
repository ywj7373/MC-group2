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

    @field:SerializedName("timeToDest")
    val timeToDest: String,

    @field:SerializedName("isAlarmed")
    val isAlarmed: Boolean,

    @field:SerializedName("done")
    val done: Boolean
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