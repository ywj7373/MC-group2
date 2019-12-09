package com.example.bluecatapp.ui.location

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.*
import android.widget.TextView
import androidx.core.text.bold
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluecatapp.R
import com.example.bluecatapp.data.LocationItemData
import kotlinx.android.synthetic.main.fragment_location.*

class LocationFragment : Fragment() {
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var locationAdapter: LocationAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private var isLocationModeEnabled: Boolean = false

    // Views
    private lateinit var nextUpView: TextView
    private lateinit var disabledTextView: TextView
    private lateinit var nextLocationView: TextView
    private lateinit var whatWhereView: TextView
    private lateinit var goalTimeView: TextView
    private lateinit var goalTimeLabelView: TextView
    private lateinit var dividerView: View
    private lateinit var divider2View: View
    private lateinit var statsTitleView: TextView
    private lateinit var gradeView: TextView
    private lateinit var statsView: TextView
    private lateinit var locationItemsTitleView: TextView
    private lateinit var locationRemindersView: RecyclerView


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize view model
        val root = inflater.inflate(R.layout.fragment_location, container, false)

        nextUpView = root.findViewById(R.id.next_text)
        disabledTextView = root.findViewById(R.id.location_disabled_text)
        nextLocationView = root.findViewById(R.id.next_loc)
        whatWhereView = root.findViewById(R.id.what_where)
        goalTimeView = root.findViewById(R.id.time_loc)
        goalTimeLabelView = root.findViewById(R.id.goal_time)
        dividerView = root.findViewById(R.id.locdivider1)
        divider2View = root.findViewById(R.id.locdivider2)
        statsTitleView = root.findViewById(R.id.stat_title)
        gradeView = root.findViewById(R.id.grade)
        statsView = root.findViewById(R.id.stat_label)
        locationItemsTitleView = root.findViewById(R.id.remind_text)
        locationRemindersView = root.findViewById(R.id.location_recycler_view)

        locationViewModel = ViewModelProviders.of(this).get(LocationViewModel::class.java)
        locationAdapter = LocationAdapter(locationViewModel)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        isLocationModeEnabled = sharedPreferences.getBoolean("Location Reminder", true)
        if (isLocationModeEnabled) {
            setHasOptionsMenu(true)

            locationViewModel.getAllLocationItems().observe(this,
                Observer<List<LocationItemData>> { t -> locationAdapter.setLocationItems(t!!) })

            locationViewModel.getNextSchedule().observe(this,
                Observer {
                    if (it != null) {
                        time_loc.text = it.time.split(" ")[1].substring(0, 5)
                        // Logic for limit number of characters
                        val nextLocationName = it.name
                        val numHangul = getNumberOfNonASCII(nextLocationName)
                        val numASCII = getNumberOfASCII(nextLocationName)
                        val displayLength =
                            2 * numHangul + numASCII    // Maximum 14 letters are appropriate

                        if (displayLength < 12) {   // add space to display aligned result
                            val space = repeat((13 - displayLength) * 3 / 4 + 1, " ")
                            next_loc.text = space + it.name
                        } else if (displayLength > 14) {
                            val slicePosition = getSlicePosition(nextLocationName, 14)
                            next_loc.text = nextLocationName.substring(0, slicePosition) + "..."
                        } else {
                            next_loc.text = it.name
                        }

                    } else {
                        next_loc.text = resources.getString(R.string.next_loc)
                        time_loc.text = resources.getString(R.string.time_loc)
                    }
                })

            // Statistics
            locationViewModel.getStats().observe(this,
                Observer {
                    if (it != null) {
                        val ontime = it.ontime
                        val absent = it.absent
                        val ratio =
                            if (ontime + absent != 0) (ontime.toDouble() + 3.0) / (ontime + absent + 3) else 1.0

                        if (ratio >= 0.9)
                            grade.text = "A+"
                        else if (ratio >= 0.8)
                            grade.text = "A0"
                        else if (ratio >= 0.7)
                            grade.text = "A-"
                        else if (ratio >= 0.6)
                            grade.text = "B+"
                        else if (ratio >= 0.5)
                            grade.text = "B0"
                        else if (ratio >= 0.4)
                            grade.text = "B-"
                        else if (ratio >= 0.3)
                            grade.text = "C+"
                        else
                            grade.text = "F"

                        stat_label.text =
                            "On-time: " + ontime + " times | Absent: " + absent + " time"
                    } else {
                        grade.text = "A+"
                        stat_label.text = "On-time: 0 times | Absent: 0 time"
                        locationViewModel.resetStats()
                    }
                })
        } else {
            LocationReminderForegroundService.stopService(context!!)
            locationModeDisabled()
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isLocationModeEnabled) {
            location_recycler_view.apply {
                // Set a LinearLayoutManager to handle Android
                // RecyclerView behavior
                layoutManager = LinearLayoutManager(activity)
                // Set the custom adapter to the RecyclerView
                adapter = locationAdapter
                setHasFixedSize(true)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.location_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    // Add new schedule
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.addItemButton -> {
                val intent = Intent(requireContext(), AddLocationActivity::class.java)
                intent.putExtra("Editmode", false)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun locationModeDisabled() {
        nextLocationView.visibility = View.GONE
        whatWhereView.visibility = View.GONE
        goalTimeView.visibility = View.GONE
        goalTimeLabelView.visibility = View.GONE
        dividerView.visibility = View.GONE
        divider2View.visibility = View.GONE
        statsTitleView.visibility = View.GONE
        gradeView.visibility = View.GONE
        statsView.visibility = View.GONE
        locationItemsTitleView.visibility = View.GONE
        locationRemindersView.visibility = View.GONE

        val message: SpannableStringBuilder = SpannableStringBuilder()
            .bold { append("LOCATION REMINDER MODE IS DISABLED") }
        nextUpView.text = message
        val nextUpViewParams = nextUpView.layoutParams as ViewGroup.MarginLayoutParams
        nextUpViewParams.topMargin = 200
        nextUpView.setTextColor(Color.DKGRAY)
        disabledTextView.visibility = View.VISIBLE
    }
}