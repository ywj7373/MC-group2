package com.example.bluecatapp

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bluecatapp.db.TodoDTO


class TodoViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
    RecyclerView.ViewHolder(inflater.inflate(R.layout.todo_task_item, parent, false)) {
    private var mDescriptionView: TextView? = null
    private var mDateView: TextView? = null
    private var mLocationView: TextView? = null
    private var mCheckView: CheckBox? = null


    init {
        mDescriptionView = itemView.findViewById(R.id.todo_task_item_description)
        mDateView = itemView.findViewById(R.id.todo_task_item_date)
        mLocationView = itemView.findViewById(R.id.todo_task_item_location)
        mCheckView = itemView.findViewById(R.id.todo_task_item_done)
    }

    fun bind(task: TodoDTO) {
        mDescriptionView?.text = task.task.toString()
        mDateView?.text = task.date.toString()
        mLocationView?.text = task.location.toString()
        mCheckView?.isChecked= task.done

    }

}