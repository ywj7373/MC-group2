package com.example.bluecatapp.ui.todo

import android.app.*
import android.content.*
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluecatapp.data.TodoItem
import kotlinx.android.synthetic.main.fragment_todo.*
import android.os.*
import android.util.Log
import androidx.preference.PreferenceManager
import com.example.bluecatapp.*
import com.example.bluecatapp.ui.location.LocationAdapter

class TodoFragment : Fragment() {

    //================================== Common ==================================//
    private lateinit var preference: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    //================================== Notification ==================================//

    //================================== Alarms ==================================//

    private var isHomeworkMode = false

    //================================== To-do List ==================================//
    private val ADD_TODO_REQUEST = 1
    private val HW_DONE = 2
    private lateinit var todoViewModel: TodoViewModel
    private lateinit var todoAdapter : TodoAdapter
    private lateinit var todoAdapter2 : TodoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //================================== General ==================================//
        setHasOptionsMenu(true)
        retainInstance = true

        preference = PreferenceManager.getDefaultSharedPreferences(requireContext())
        editor = preference.edit()

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.todo_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item?.itemId) {
            R.id.todo_delete_all_tasks -> {
                todoViewModel.deleteAllTodoItems()
                Toast.makeText(requireContext(), "All To-do Items deleted!", Toast.LENGTH_SHORT)
                    .show()
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
            Log.d("TodoFragment:onActivityResult", "requestCode : $requestCode")
            val newTodoItem = TodoItem(
                data!!.getStringExtra(AddTodoActivity.TASK),
                data.getStringExtra(AddTodoActivity.DATETIME),
//                data.getStringExtra(AddTodoActivity.LOCATION),
                false
            )

            todoViewModel.insert(newTodoItem)

            Toast.makeText(requireContext(), "Todo saved!", Toast.LENGTH_SHORT).show()
        } else if (requestCode == HW_DONE && resultCode == Activity.RESULT_OK) {

            if(data!!.action == "TIMER_CANCELED"){

                Toast.makeText(
                    requireContext(),
                    "Nice Job!",
                    Toast.LENGTH_SHORT
                ).show()

            }else if(data!!.action == "NORMAL_BACKBUTTON"){

                Toast.makeText(
                    requireContext(),
                    "HW mode canceled",
                    Toast.LENGTH_SHORT
                ).show()

            }else{
                Log.d("TodoFragment:onActivityResult:HW_DONE", "[ERROR] Undefined action : ${data!!.action}")
            }

        } else {
            Log.d("TodoFragment:onActivityResult", "[ERROR] Undefined requestCode : $requestCode")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_todo, container, false)

        todoViewModel =
            ViewModelProviders.of(this).get(TodoViewModel::class.java)
        todoAdapter = TodoAdapter(todoViewModel)
        todoAdapter2 = TodoAdapter(todoViewModel)

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

        btn_homework.setOnClickListener(clickListener)

        todo_add_task.setOnClickListener { view ->
            //            getActivity().
            startActivityForResult(
                Intent(requireContext(), AddTodoActivity::class.java),
                ADD_TODO_REQUEST
            )
        }

    }

    override fun onResume() {
        super.onResume()

        Log.d("HW:onResume:modeBool", "HW onResume $isHomeworkMode")

        isHomeworkMode = preference.getBoolean(getString(R.string.hw_mode_bool),false)
        if (isHomeworkMode) {
            turnHWModeOn()
        }

    }

    override fun onPause() {
        super.onPause()
        Log.d("HW:onPause:modeBool", "HW onPause at $isHomeworkMode")

        //---change the shared preference---//
        toggleHWMode(isHomeworkMode)

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("HW:onDestroy", "HW:onDestroy")
    }

    private val clickListener: View.OnClickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.btn_homework -> {

                turnHWModeOn()

//                if (isHomeworkMode) {
//                    Log.d("TodoFragment:clickListener","[ERROR] isHomeworkMode : $isHomeworkMode")
//                } else { // turning on the hw mode
//                    turnHWModeOn()
//                }
            }
        }
    }

    fun toggleHWMode(modeState: Boolean) { // stay
        editor.putBoolean(getString(R.string.hw_mode_bool), modeState)
        editor.commit()
    }

    private fun turnHWModeOn() {
        Toast.makeText(requireContext(), "Good Luck!", Toast.LENGTH_SHORT).show()

        //---change the shared preference---//
        isHomeworkMode = true
        toggleHWMode(isHomeworkMode)

        // start the TimerActivity
        startActivityForResult(
            Intent(requireContext(), TimerActivity::class.java),
            HW_DONE
        )
    }

}