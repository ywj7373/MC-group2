package com.example.bluecatapp.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

// b. “Dao”
//The Data Access Object (DAO) is an interface annotated with Dao.
// This is where the database CRUD (create, read, update and delete) operations are defined.
// Each method is annotated with “@Insert”, “@Delete”, “@Query(SELECT * FROM)”.

@Dao
interface TodoItemDao {

    @Insert
    fun insert(todoItem: TodoItem)

    @Query("DELETE FROM todo_items")
    fun deleteAllTodoItems()

    @Query("SELECT * FROM todo_items ")
    fun getAllTodoItems(): LiveData<List<TodoItem>>

}