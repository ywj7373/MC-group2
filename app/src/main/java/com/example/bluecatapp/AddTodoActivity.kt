package com.example.bluecatapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_add_todo.*
import kotlinx.android.synthetic.main.fragment_todo.*

class AddTodoActivity : AppCompatActivity() {
    companion object {
        const val TASK = "TASK"
        const val DATETIME = "DATETIME"
        const val LOCATION = "LOCATION"
        const val ISDONE = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_todo)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.add_todo_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.todo_save_task -> {
                saveTodo()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveTodo() {
        if (todo_edit_task.text.toString().trim().isBlank()
            || todo_edit_datetime.text.toString().trim().isBlank()
            || todo_edit_location.text.toString().trim().isBlank()) {
            Toast.makeText(this, "Can not insert empty item!", Toast.LENGTH_SHORT).show()
            return
        }

        val data = Intent().apply {
            putExtra(TASK, todo_edit_task.text.toString())
            putExtra(DATETIME, todo_edit_datetime.text.toString())
            putExtra(LOCATION, todo_edit_location.text.toString())
        }

        setResult(Activity.RESULT_OK, data)
        finish()
    }
}