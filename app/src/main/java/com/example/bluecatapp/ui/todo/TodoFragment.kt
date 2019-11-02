package com.example.bluecatapp.ui.todo

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluecatapp.AddTodoActivity
import com.example.bluecatapp.R
import com.example.bluecatapp.data.TodoItem
import kotlinx.android.synthetic.main.fragment_todo.*
import android.content.DialogInterface
import android.text.Editable
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.example.bluecatapp.MainActivity


class TodoFragment : Fragment() {

    val hwDoneDisplayText = "I have done my homework."
    val hwDoneConfirmText = hwDoneDisplayText.replace("\\s".toRegex(), "").toLowerCase()

    private val ADD_TODO_REQUEST = 1
    private lateinit var todoViewModel: TodoViewModel
    private val todoAdapter = TodoAdapter()
    private val todoAdapter2 = TodoAdapter()

    private var isHomeworkMode = false;

    private var hwModeTime =
        1000 * 60 * 45 // 45 minutes @todo should be changed as set on the settings page

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

        todoViewModel.getTodoItemsNotDone().observe(this,
            Observer<List<TodoItem>> { t -> todoAdapter.setTodoItems(t!!) })

        todoViewModel.getTodoItemsDone().observe(this,
            Observer<List<TodoItem>> { t -> todoAdapter2.setTodoItems(t!!) })

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

        // view model 을 통해 특정 todoItem 에 대한 데이터 변경 실행.
        todoAdapter.onItemClick = { todoItem ->
            //            Log.d("test","todoItem test "+todoItem.id.toString()+"/"+todoItem.done.toString())
            todoViewModel.updateTodoStatus(todoItem)
        }

        todoDone_recycler_view.apply {
            // set a LinearLayoutManager to handle Android
            // RecyclerView behavior
            layoutManager = LinearLayoutManager(activity)
            // set the custom adapter to the RecyclerView
            adapter = todoAdapter2
            setHasFixedSize(true)
        }

        todoAdapter2.onItemClick = { todoItem ->
            //            Log.d("test","todoItem test "+todoItem.id.toString()+"/"+todoItem.done.toString())
            todoViewModel.updateTodoStatus(todoItem)
        }

        container_hwmode.setOnClickListener(clickListener)

//        container_hwmode.setOnClickListener { view ->
//            if (isHomeworkMode) {
//                view_timer.isCountDown = false
//                view_timer.base = SystemClock.elapsedRealtime() + hwModeTime // 45 minutes
//                view_timer.stop()
//
//                isHomeworkMode = false;
//                text_homework.text = "Turn Homework Mode ON"
//                text_homework.setBackgroundColor(Color.parseColor("#111111"))
//                text_homework.setTextColor(Color.parseColor("#ffffff"))
//                todo_ll_container.setBackgroundColor(Color.parseColor("#ffffff"))
//                clock_homework.visibility = View.VISIBLE
////                text_hw_timer.visibility = View.GONE
//                view_timer.visibility = View.GONE
//
//            } else {
//                view_timer.isCountDown = true
//                view_timer.base = SystemClock.elapsedRealtime() + hwModeTime
//                view_timer.start()
//
//                isHomeworkMode = true
//                text_homework.text = "Turn Homework Mode OFF"
//                text_homework.setBackgroundColor(Color.parseColor("#dddddd"))
//                text_homework.setTextColor(Color.parseColor("#111111"))
//                todo_ll_container.setBackgroundColor(Color.parseColor("#111111"))
//                clock_homework.visibility = View.GONE
////                text_hw_timer.visibility = View.VISIBLE
//                view_timer.visibility = View.VISIBLE
//
//            }
//        }

        todo_add_task.setOnClickListener { view ->
            //            getActivity().
            startActivityForResult(
                Intent(requireContext(), AddTodoActivity::class.java),
                ADD_TODO_REQUEST
            )
        }
    }

    private val clickListener: View.OnClickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.container_hwmode -> {

                if (isHomeworkMode) {

                    var alertDialog = AlertDialog.Builder(requireContext()).create()
                    val editText = EditText(requireContext())
                    alertDialog.setMessage("Have you finished your homework?\n"+
                            "Type in\n"+
                            "\""+hwDoneDisplayText+"\"")
                    alertDialog.setTitle("Turn Off HW Mode")

                    alertDialog.setView(editText)

                    alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,"Yes!", DialogInterface.OnClickListener { dialog, whichButton ->

                        val inputText = editText.text.toString().replace("\\s".toRegex(), "").toLowerCase()
                        if(inputText.equals(hwDoneConfirmText)){
                            view_timer.isCountDown = false
                            view_timer.base = SystemClock.elapsedRealtime() + hwModeTime // 45 minutes
                            view_timer.stop()

                            isHomeworkMode = false;
                            text_homework.text = "Turn Homework Mode ON"
                            text_homework.setBackgroundColor(Color.parseColor("#111111"))
                            text_homework.setTextColor(Color.parseColor("#ffffff"))
                            todo_ll_container.setBackgroundColor(Color.parseColor("#ffffff"))
                            clock_homework.visibility = View.VISIBLE
//                text_hw_timer.visibility = View.GONE
                            view_timer.visibility = View.GONE

                            Toast.makeText(requireContext(),"Nice job!",Toast.LENGTH_LONG)
                            alertDialog.dismiss()
                        }else{
                            Toast.makeText(requireContext(),"Type in the given sentence properly",Toast.LENGTH_LONG)
                        }
                    })

                    alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE,"No...", DialogInterface.OnClickListener { dialog, whichButton ->
//                        val inputText = editText.text.toString()
                        Toast.makeText(requireContext(),"Keep Working!",Toast.LENGTH_LONG)
                        alertDialog.dismiss()
                    })

                    alertDialog.show()

                } else {
                    Toast.makeText(requireContext(), "Good Luck!", Toast.LENGTH_LONG).show()
                    view_timer.isCountDown = true
                    view_timer.base = SystemClock.elapsedRealtime() + hwModeTime
                    view_timer.start()

                    isHomeworkMode = true
                    text_homework.text = "Turn Homework Mode OFF"
                    text_homework.setBackgroundColor(Color.parseColor("#dddddd"))
                    text_homework.setTextColor(Color.parseColor("#111111"))
                    todo_ll_container.setBackgroundColor(Color.parseColor("#111111"))
                    clock_homework.visibility = View.GONE
//                text_hw_timer.visibility = View.VISIBLE
                    view_timer.visibility = View.VISIBLE
                }
            }
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
//                data.getStringExtra(AddTodoActivity.LOCATION),
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