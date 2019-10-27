package com.example.bluecatapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "todo_items")
data class TodoItem(

    @field:SerializedName("task")
    val task: String,

    @field:SerializedName("dateTime")
    val dateTime: String,

//    @field:SerializedName("location")
//    val location: String,

    @field:SerializedName("isDone")
    val done: Boolean
){
    @PrimaryKey(autoGenerate = true)
    @field:SerializedName("id")
    var id: Int = 0
}
