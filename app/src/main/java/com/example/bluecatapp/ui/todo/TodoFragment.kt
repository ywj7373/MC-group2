package com.example.bluecatapp.ui.todo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluecatapp.AddTodoActivity
import com.example.bluecatapp.R
import com.example.bluecatapp.TodoAdapter
import com.example.bluecatapp.data.TodoItem
import kotlinx.android.synthetic.main.fragment_todo.*

class TodoFragment : Fragment() {

    private val ADD_TODO_REQUEST = 1
    private lateinit var todoViewModel: TodoViewModel
    private val todoAdapter = TodoAdapter()

//    private val sampleTasks = listOf(
//        TodoItem(0, "sample1", "2019.10.10", "Seoul",false),
//        TodoItem(1, "sample2", "2019.10.10", "Seoul",false)
//
//    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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

        todoViewModel.getAllTodoItems().observe(this,
            Observer<List<TodoItem>> { t -> todoAdapter.setTodoItems(t!!) })

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
            adapter = todoAdapter
            setHasFixedSize(true)

        }

        todo_add_task.setOnClickListener {
                view->
            //            getActivity().
            startActivityForResult(
            Intent(requireContext(), AddTodoActivity::class.java),
            ADD_TODO_REQUEST
        )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.todo_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item?.itemId) {
            R.id.todo_delete_all_tasks -> {
                todoViewModel.deleteAllTodoItems()
                Toast.makeText(requireContext(), "All To-do Items deleted!", Toast.LENGTH_SHORT).show()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_TODO_REQUEST && resultCode == Activity.RESULT_OK) {
            val newTodoItem = TodoItem(
                data!!.getStringExtra(AddTodoActivity.TASK),
                data.getStringExtra(AddTodoActivity.DATETIME),
                data.getStringExtra(AddTodoActivity.LOCATION),
                false
            )

            todoViewModel.insert(newTodoItem)

            Toast.makeText(requireContext(), "Todo saved!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Todo not saved!", Toast.LENGTH_SHORT).show()
        }


    }

//    companion object {
//        fun newInstance(): TodoFragment = TodoFragment()
//    }
}