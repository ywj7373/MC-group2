package com.example.bluecatapp.ui.todo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bluecatapp.R
import com.example.bluecatapp.TodoAdapter
import com.example.bluecatapp.db.TodoDTO
import kotlinx.android.synthetic.main.fragment_todo.*
import android.view.MenuInflater



class TodoFragment : Fragment() {

    private lateinit var todoViewModel: TodoViewModel

    private val sampleTasks = listOf(
        TodoDTO(0, "sample1", "2019.10.10", "Seoul",false),
        TodoDTO(1, "sample2", "2019.10.10", "Seoul",false)

    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true);
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        todoViewModel =
            ViewModelProviders.of(this).get(TodoViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_todo, container, false)
//        val textView: TextView = root.findViewById(R.id.text_todo)
//        todoViewModel.text.observe(this, Observer {
//            textView.text = it
//        })

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // RecyclerView node initialized here
        todo_recycler_view.apply {
            // set a LinearLayoutManager to handle Android
            // RecyclerView behavior
            layoutManager = LinearLayoutManager(activity)
            // set the custom adapter to the RecyclerView
            adapter = TodoAdapter(sampleTasks)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.todo_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    companion object {
        fun newInstance(): TodoFragment = TodoFragment()
    }
}