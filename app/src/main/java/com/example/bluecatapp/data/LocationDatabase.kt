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
@Database(entities = [LocationItemData::class], version = 14)
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
                PopulateDbAsyncTask(instance).execute()
            }
        }

        class PopulateDbAsyncTask(db: CurrentLocationDatabase?) : AsyncTask<Unit, Unit, Unit>() {
            private val currentLocationDao = db?.currentLocationDao()
            override fun doInBackground(vararg p0: Unit?) {
                currentLocationDao?.insert(CurrentLocationData(37.459553, 126.952162))
            }
        }
    }
}

@Database(entities = [TravelTimeData::class], version = 2)
abstract class TravelTimeDatabase : RoomDatabase() {

    abstract fun travelTimeDao(): TravelTimeDao

    companion object {
        private var instance: TravelTimeDatabase? = null

        fun getInstance(context: Context): TravelTimeDatabase? {
            if (instance == null) {
                synchronized(TravelTimeDatabase::class) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        TravelTimeDatabase::class.java, "travel_time"
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
                PopulateDbAsyncTask(instance).execute()
            }
        }

        class PopulateDbAsyncTask(db: TravelTimeDatabase?) : AsyncTask<Unit, Unit, Unit>() {
            private val travelTimeDao = db?.travelTimeDao()
            override fun doInBackground(vararg p0: Unit?) {
                travelTimeDao?.insert(TravelTimeData("20"))
            }
        }
    }
}

@Database(entities = [StatsData::class], version = 1)
abstract class StatsDatabase : RoomDatabase() {

    abstract fun statsDao(): StatsDao

    companion object {
        private var instance: StatsDatabase? = null

        fun getInstance(context: Context): StatsDatabase? {
            if (instance == null) {
                synchronized(StatsDatabase::class) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        StatsDatabase::class.java, "stats"
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

@Database(entities = [DateData::class], version = 1)
abstract class DateDatabase : RoomDatabase() {

    abstract fun dateDao(): DateDao

    companion object {
        private var instance: DateDatabase? = null

        fun getInstance(context: Context): DateDatabase? {
            if (instance == null) {
                synchronized(DateDatabase::class) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        DateDatabase::class.java, "mcurrent_date_table"
                    )
                        .allowMainThreadQueries()
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