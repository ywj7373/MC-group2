package com.example.bluecatapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluecatapp.data.TodoItem
import com.example.bluecatapp.ui.todo.TodoViewModel
import com.example.bluecatapp.ui.settings.SettingsFragment
import com.google.gson.Gson

object FragmentToLoad {
    val TODO = 0
    val APPBLOCK = 1
    val LOCATION = 2
    val SETTINGS = 3
}

class MainActivity : AppCompatActivity() {
    private val ADD_TODO_REQUEST = 1
    private lateinit var todoViewModel: TodoViewModel

    private lateinit var preference: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

//    private lateinit var sensorManager: SensorManager

    companion object {
        val gson = Gson()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preference = PreferenceManager.getDefaultSharedPreferences(this)
        editor = preference.edit()

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_todo,
                R.id.navigation_appblocking,
                R.id.navigation_location,
                R.id.navigation_settings
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        when (intent.extras?.getInt("frgToLoad")) {
            0 -> navController.navigate(R.id.navigation_todo)
            1 -> navController.navigate(R.id.navigation_appblocking)
            2 -> navController.navigate(R.id.navigation_location)
            3 -> navController.navigate(R.id.navigation_settings)
        }

//        this.sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
//
//        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
//            this.accelerometer = it
//        }
//
//        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)?.let {
//            this.gravity = it
//        }
//
//        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
//            this.gyroscope = it
//        }
//
//        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.let {
//            this.linearAcceleration = it
//        }
//
//        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let {
//            this.rotationVector = it
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        editor.putBoolean(getString(R.string.hw_mode_bool),false) // set hw_mode_bool to false when app turns off
    }
}