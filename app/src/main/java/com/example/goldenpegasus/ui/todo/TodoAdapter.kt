package com.example.goldenpegasus.ui.todo

import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.goldenpegasus.R
import com.example.goldenpegasus.data.TodoItem

class TodoAdapter internal constructor(todoViewModel: TodoViewModel) : RecyclerView.Adapter<TodoAdapter.TodoItemHolder>() {

    var onItemClick: ((TodoItem) -> Unit)? = null
    private var todoItems: List<TodoItem> = ArrayList()
    private val todoViewModel = todoViewModel
    private val TIME: Long = 1000

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoItemHolder {

        var itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.todo_task_item, parent, false)
        if (parent.id == R.id.todoDone_recycler_view) {
            itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.todo_task_item_done, parent, false)
        }

        return TodoItemHolder(itemView)
    }

    override fun onBindViewHolder(holder: TodoItemHolder, position: Int) {
        val currentTodoItem = todoItems[position]
        holder.textViewTask.text = currentTodoItem.task
        holder.textViewDate.text = currentTodoItem.dateTime
//        holder.textViewLocation.text = currentTodoItem.location
        holder.checkBoxDone.isChecked = currentTodoItem.done

        holder.btn_delete.setOnClickListener {
            val todoItem = todoItems[position]
            todoViewModel.deleteTodoItem(todoItem.id)
            (todoItems as ArrayList).removeAt(position)
            notifyItemRemoved(position)
            holder.btn_delete.isEnabled = false
            //prevents fast double click
            val handler = Handler()
            val runnable = Runnable {
                holder.btn_delete.isEnabled = true
            }
            handler.postDelayed(runnable, TIME)
        }
    }

    override fun getItemCount(): Int = todoItems.size

    fun setTodoItems(todoItems: List<TodoItem>) {
        this.todoItems = todoItems
        notifyDataSetChanged()
    }

    fun removeItem(position:Int, todoViewModel: TodoViewModel) {
        val locationItem = todoItems[position]
        todoViewModel.deleteTodoItem(locationItem.id)
        (todoItems as ArrayList).removeAt(position)
        notifyItemRemoved(position)
    }


    inner class TodoItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textViewTask: TextView = itemView.findViewById(R.id.todo_item_task)
        var textViewDate: TextView = itemView.findViewById(R.id.todo_item_date)
        //        var textViewLocation: TextView = itemView.findViewById(R.id.todo_item_location)
        var checkBoxDone: CheckBox = itemView.findViewById(R.id.todo_item_done)
        var lineDone: View = itemView.findViewById(R.id.todo_item_line_done)
        var btn_delete: Button = itemView.findViewById(R.id.btn_todo_delete)

        init {
            checkBoxDone.setOnClickListener {
                if (checkBoxDone.isChecked) {
                    lineDone.visibility = View.GONE
                } else {
                    lineDone.visibility = View.VISIBLE
                }
                onItemClick?.invoke(todoItems[adapterPosition])
            }
//            itemView.setOnClickListener {
//                onItemClick?.invoke(todoItems[adapterPosition])
//            }
        }

    }

}
