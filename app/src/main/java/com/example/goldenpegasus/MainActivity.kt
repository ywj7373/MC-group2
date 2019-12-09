package com.example.goldenpegasus

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson


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