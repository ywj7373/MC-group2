package com.example.bluecatapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bluecatapp.db.TodoDTO

class TodoAdapter(private val list: List<TodoDTO>)
    : RecyclerView.Adapter<TodoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return TodoViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo: TodoDTO= list[position]
        holder.bind(todo)
    }

    override fun getItemCount(): Int = list.size

}
