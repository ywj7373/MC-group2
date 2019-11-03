package com.example.bluecatapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

//Create Room Database
@Database(entities = [LocationItemData::class], version = 4)
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

@Database(entities = [CurrentLocationData::class], version = 1)
abstract class CurrentLocationDatabase : RoomDatabase() {

    abstract fun currentLocationDao(): CurrentLocationDao

    companion object {
        private var instance: CurrentLocationDatabase? = null

        fun getInstance(context: Context): CurrentLocationDatabase? {
            if (instance == null) {
                synchronized(CurrentLocationDatabase::class) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        CurrentLocationDatabase::class.java, "current_location"
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