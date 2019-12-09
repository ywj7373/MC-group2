package com.example.goldenpegasus.data

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

    @Query("DELETE FROM todo_items WHERE id = :value1")
    fun deleteTodoItem(value1: Int)

    @Query("SELECT * FROM todo_items")
    fun getAllTodoItems(): LiveData<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE done=0")
    fun getTodoItemsNotDone(): LiveData<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE done=1")
    fun getTodoItemsDone(): LiveData<List<TodoItem>>

    @Query("UPDATE todo_items SET done=1 WHERE id LIKE :id")
    fun makeDone(id: Int)

    @Query("UPDATE todo_items SET done=0 WHERE id LIKE :id")
    fun makeUnDone(id: Int)

}