package com.example.bluecatapp.data

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.LiveData

class TodoItemRepository(application: Application) {

    private var todoItemDao: TodoItemDao

    private var allTodoItems: LiveData<List<TodoItem>>

    init {
        val database: TodoItemDatabase = TodoItemDatabase.getInstance(
            application.applicationContext
        )!!
        todoItemDao = database.todoItemDao()
        allTodoItems = todoItemDao.getAllTodoItems()
    }

    fun insert(todoItem: TodoItem) {
        val insertNoteAsyncTask = InsertNoteAsyncTask(todoItemDao).execute(todoItem)
    }

    fun deleteAllTodoItems() {
        val deleteAllNotesAsyncTask = DeleteAllNotesAsyncTask(
            todoItemDao
        ).execute()
    }

    fun getAllTodoItems(): LiveData<List<TodoItem>> {
        return allTodoItems
    }

    private class InsertNoteAsyncTask(todoItemDao: TodoItemDao) : AsyncTask<TodoItem, Unit, Unit>() {
        val todoItemDao = todoItemDao

        override fun doInBackground(vararg p0: TodoItem?) {
            todoItemDao.insert(p0[0]!!)
        }
    }


    private class DeleteAllNotesAsyncTask(val todoItemDao: TodoItemDao) : AsyncTask<Unit, Unit, Unit>() {

        override fun doInBackground(vararg p0: Unit?) {
            todoItemDao.deleteAllTodoItems()
        }
    }

}