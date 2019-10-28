package com.example.bluecatapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "location_items")
data class LocationItem(

    @field:SerializedName("name")
    val name: String,

    @field:SerializedName("timeStamp")
    val dateTime: String,

    @field:SerializedName("x")
    val x: String,

    @field:SerializedName("y")
    val y: String,

    @field:SerializedName("time")
    val time: String,

    @field:SerializedName("timeToDest")
    val timeToDest: String
)
{
    @PrimaryKey(autoGenerate = true)
    @field:SerializedName("id")
    var id: Int = 0
}