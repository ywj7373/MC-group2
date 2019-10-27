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
    val time: String
    /*
    @field:SerializedName("year")
    val year: Int,

    @field:SerializedName("month")
    val month: Int,

    @field:SerializedName("day")
    val day: Int,

    @field:SerializedName("hour")
    val hour: Int,

    @field:SerializedName("minute")
    val minute: Int

     */
)
{
    @PrimaryKey(autoGenerate = true)
    @field:SerializedName("id")
    var id: Int = 0
}


//package com.example.bluecatapp.data
/*
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class LocationItem(@PrimaryKey(autoGenerate = true) var id: Int? = null,
                   @ColumnInfo(name = "name")var name: String?,
                   @ColumnInfo(name = "latitude") var latitude: Int?,
                   @ColumnInfo(name = "longitude") var longitude: Int?,
                   @ColumnInfo(name = " ")) {


}*/