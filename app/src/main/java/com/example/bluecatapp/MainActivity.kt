package com.example.bluecatapp

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
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
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.fragment.app.Fragment
import com.example.bluecatapp.ui.todo.TodoFragment


object FragmentToLoad {
    val TODO = 0
    val APPBLOCK = 1
    val LOCATION = 2
    val SETTINGS = 3
}

class MainActivity : AppCompatActivity() {
    companion object {
        val gson = Gson()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        preference = PreferenceManager.getDefaultSharedPreferences(this)
//        editor = preference.edit()

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

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Main:onDestroy", "Main:onDestroy")
    }

    override fun onStart() {
        super.onStart()
        Log.d("Main:onStart", "Main:onStart")
    }
}