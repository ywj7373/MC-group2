package com.example.bluecatapp.data

import android.content.Context
import android.os.AsyncTask
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.text.SimpleDateFormat
import java.util.*

//Create Room Database
@Database(entities = [LocationItem::class], version = 2)
abstract class LocationItemDatabase : RoomDatabase() {

    abstract fun locationItemDao(): LocationItemDao

    companion object {
        private var instance: LocationItemDatabase? = null

        fun getInstance(context: Context): LocationItemDatabase? {
            if (instance == null) {
                synchronized(LocationItemDatabase::class) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        LocationItemDatabase::class.java, "location_items"
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
            }
        }
    }
}