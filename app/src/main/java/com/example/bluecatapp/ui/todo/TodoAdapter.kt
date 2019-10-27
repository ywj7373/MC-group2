package com.example.bluecatapp.ui.todo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bluecatapp.R
import com.example.bluecatapp.data.TodoItem

class TodoAdapter : RecyclerView.Adapter<TodoAdapter.TodoItemHolder>() {

    private var todoItems: List<TodoItem> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoItemHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.todo_task_item, parent, false)
        return TodoItemHolder(itemView)
    }

    override fun onBindViewHolder(holder: TodoItemHolder, position: Int) {
        val currentTodoItem = todoItems[position]
        holder.textViewTask.text = currentTodoItem.task
        holder.textViewDate.text = currentTodoItem.dateTime
//        holder.textViewLocation.text = currentTodoItem.location
        holder.checkBoxDone.isChecked = currentTodoItem.done
    }

    override fun getItemCount(): Int = todoItems.size

    fun setTodoItems(todoItems: List<TodoItem>) {
        this.todoItems = todoItems
        notifyDataSetChanged()
    }

    inner class TodoItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textViewTask: TextView = itemView.findViewById(R.id.todo_item_task)
        var textViewDate: TextView = itemView.findViewById(R.id.todo_item_date)
//        var textViewLocation: TextView = itemView.findViewById(R.id.todo_item_location)
        var checkBoxDone: CheckBox = itemView.findViewById(R.id.todo_item_done)

    }

}
