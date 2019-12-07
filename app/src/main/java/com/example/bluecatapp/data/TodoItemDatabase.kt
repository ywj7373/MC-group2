package com.example.bluecatapp.data

import android.content.Context
import android.os.AsyncTask
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import java.text.SimpleDateFormat
import java.util.*

// version should be increased everytime scheme is changed.
@Database(entities = [TodoItem::class], version = 4)
@TypeConverters(Converters::class)
abstract class TodoItemDatabase : RoomDatabase() {

    abstract fun todoItemDao(): TodoItemDao

    companion object {
        private var instance: TodoItemDatabase? = null

        fun getInstance(context: Context): TodoItemDatabase? {
            if (instance == null) {
                synchronized(TodoItemDatabase::class) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        TodoItemDatabase::class.java, "todo_items"
                    )
                        .fallbackToDestructiveMigration()
                        .addCallback(roomCallback)
                        .build()
                }
            }
            return instance
        }

        fun destroyInstance() {
            instance = null
        }

        private val roomCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                PopulateDbAsyncTask(instance)
                    .execute()
            }
        }
    }

    class PopulateDbAsyncTask(db: TodoItemDatabase?) : AsyncTask<Unit, Unit, Unit>() {
        private val todoItemDao = db?.todoItemDao()
        private val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date());

        override fun doInBackground(vararg p0: Unit?) {
//            todoItemDao?.insert(TodoItem("Task 1", timeStamp,false))
//            todoItemDao?.insert(TodoItem("Task 2", timeStamp,false))
//            todoItemDao?.insert(TodoItem("Task 3", timeStamp,false))
//            todoItemDao?.insert(TodoItem("Task 4", timeStamp,true))
        }
    }

}