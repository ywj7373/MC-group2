package com.example.bluecatapp.ui.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bluecatapp.data.TodoItem
import com.example.bluecatapp.data.TodoItemRepository


//ViewModel
//This is also the part of lifecycle library;
// this will help you to provide data between repository and UI.
// This survives the data on configuration changes
// and gets the existing ViewModel to reconnect with the new instance of the owner.

//Why use ViewModel?
//The ViewModel is lifecycle aware so that it will survive the configuration change. It will outlive the Activity or Fragment.
//Easier communications between fragments, instead of relying on the hosting Activity passing the communications.
//Works pretty well with LiveData, an observable data holder.
//You can use RxJava instead of LiveData.

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    //    Here, we are using AndroidViewModel because we need an application context.

    private var repository: TodoItemRepository = TodoItemRepository(application)
    private var allTodoItems: LiveData<List<TodoItem>> = repository.getAllTodoItems()
    private var todoItemsNotDone: LiveData<List<TodoItem>> = repository.getTodoItemsNotDone()
    private var todoItemsDone: LiveData<List<TodoItem>> = repository.getTodosItemsDone()

    fun insert(todoItem: TodoItem) {
        repository.insert(todoItem)
    }

    fun deleteAllTodoItems() {
        repository.deleteAllTodoItems()
    }

    fun getAllTodoItems(): LiveData<List<TodoItem>> {
        return allTodoItems
    }

    fun getTodoItemsNotDone(): LiveData<List<TodoItem>> {
        return todoItemsNotDone
    }

    fun getTodoItemsDone(): LiveData<List<TodoItem>> {
        return todoItemsDone
    }

    fun updateTodoStatus(todoItem: TodoItem){
        repository.updateTodoStatus(todoItem)
    }
}