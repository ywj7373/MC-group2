package com.example.bluecatapp.data

import android.app.Application
import android.os.AsyncTask
import android.util.Log
import androidx.lifecycle.LiveData

class TodoItemRepository(application: Application) {

    private var todoItemDao: TodoItemDao

    private var allTodoItems: LiveData<List<TodoItem>>
    private var doneTodoItems: LiveData<List<TodoItem>>
    private var notDoneTodoItems: LiveData<List<TodoItem>>

    init {
        val database: TodoItemDatabase = TodoItemDatabase.getInstance(
            application.applicationContext
        )!!

        todoItemDao = database.todoItemDao()
        allTodoItems = todoItemDao.getAllTodoItems()
        doneTodoItems = todoItemDao.getTodoItemsDone()
        notDoneTodoItems = todoItemDao.getTodoItemsNotDone()
    }

    fun insert(todoItem: TodoItem) {
        val insertTodosAsyncTask = InsertTodoAsyncTask(todoItemDao).execute(todoItem)
    }

    fun deleteAllTodoItems() {
        val deleteAllTodosAsyncTask = DeleteAllTodosAsyncTask(todoItemDao).execute()
    }

    fun deleteTodoItem(id: Int) {
        DeleteTodoAsyncTask(todoItemDao, id).execute()
    }

    fun getAllTodoItems(): LiveData<List<TodoItem>> {
        return allTodoItems
    }

    fun getTodoItemsNotDone(): LiveData<List<TodoItem>> {
        return notDoneTodoItems
    }

    fun getTodosItemsDone(): LiveData<List<TodoItem>> {
        return doneTodoItems
    }

    fun updateTodoStatus(todoItem : TodoItem){
        val updateTodoStatusAsyncTask = UpdateTodoStatusAsyncTask(todoItemDao).execute(todoItem)
    }

    private class InsertTodoAsyncTask(todoItemDao: TodoItemDao) : AsyncTask<TodoItem, Unit, Unit>() {
        val todoItemDao = todoItemDao

        override fun doInBackground(vararg p0: TodoItem?) {
            todoItemDao.insert(p0[0]!!)
        }
    }

    private class DeleteAllTodosAsyncTask(todoItemDao: TodoItemDao) : AsyncTask<Unit, Unit, Unit>() {
        val todoItemDao = todoItemDao

        override fun doInBackground(vararg p0: Unit?) {
            todoItemDao.deleteAllTodoItems()
        }
    }

    private class DeleteTodoAsyncTask(todoItemDao: TodoItemDao, var id: Int) : AsyncTask<Unit, Unit, Unit>() {
        val todoItemDao = todoItemDao

        override fun doInBackground(vararg p0: Unit?) {
            todoItemDao.deleteTodoItem(id)
        }
    }

    private class UpdateTodoStatusAsyncTask(todoItemDao: TodoItemDao) : AsyncTask<TodoItem, Unit, Unit>() {
        val todoItemDao = todoItemDao

        override fun doInBackground(vararg p0: TodoItem?) {
            Log.d("test","ababa"+p0)
            Log.d("test","ababa"+p0[0]!!)
            Log.d("test","ababa"+p0[0])
            Log.d("test","ababa "+p0[0]!!.id)
            Log.d("test","ababa "+p0[0]!!.done)
            if(p0[0]!!.done.toString().equals("true")){
                todoItemDao.makeUnDone(p0[0]!!.id)
            }else{
                todoItemDao.makeDone(p0[0]!!.id)
            }

        }
    }

}