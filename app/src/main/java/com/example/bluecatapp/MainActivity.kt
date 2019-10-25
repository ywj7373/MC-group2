package com.example.bluecatapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
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
    companion object {
        val gson = Gson()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
//        val recyclerView = findViewById<RecyclerView>(R.id.todo_recycler_view)
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == ADD_TODO_REQUEST && resultCode == Activity.RESULT_OK) {
//            val newTodoItem = TodoItem(
//                data!!.getStringExtra(AddTodoActivity.TASK),
//                data.getStringExtra(AddTodoActivity.DATETIME),
//                data.getStringExtra(AddTodoActivity.LOCATION),
//                false
//            );
//
//            todoViewModel.insert(newTodoItem)
//
//            Toast.makeText(this, "Todo saved!", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(this, "Todo not saved!", Toast.LENGTH_SHORT).show()
//        }
//
//
//    }
}
