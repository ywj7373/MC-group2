package com.example.bluecatapp.ui.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluecatapp.R
import com.example.bluecatapp.data.LocationItemData
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_location.*

class LocationFragment : Fragment() {
    private val TAG = "Location Fragment"
    private lateinit var locationViewModel: LocationViewModel
    private val PERMISSION_ID = 270
    private lateinit var locationAdapter: LocationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        //initialize view model
        val root = inflater.inflate(R.layout.fragment_location, container, false)
        locationViewModel = ViewModelProviders.of(this).get(LocationViewModel::class.java)
        locationAdapter = LocationAdapter(locationViewModel)
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
                    val displayLength = 2 * numHangul + numASCII            // maximum 14 letters are appropriate

                    if(displayLength < 12) {                                // add space to display aligned result
                        val space = repeat((13 - displayLength)*3/4 + 1 , " ")
                        next_loc.text = space + it.name
                    }
                    else if(displayLength > 14) {
                        val slicePosition = getSlicePosition(nextLocationName, 14)
                        next_loc.text = nextLocationName.substring(0, slicePosition) + "..."
                    }
                    else {
                        next_loc.text = it.name
                    }

                }
                else {
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
                    val ratio = if( ontime+absent != 0 ) (ontime.toDouble()+3.0)/(ontime+absent+3) else 1.0

                    if(ratio >= 0.9)
                        grade.text = "A+"
                    else if(ratio >= 0.8)
                        grade.text = "A0"
                    else if(ratio >= 0.7)
                        grade.text = "A-"
                    else if(ratio >= 0.6)
                        grade.text = "B+"
                    else if(ratio >= 0.5)
                        grade.text = "B0"
                    else if(ratio >= 0.4)
                        grade.text = "B-"
                    else if(ratio >= 0.3)
                        grade.text = "C+"
                    else
                        grade.text = "F"

                    stat_label.text = "On-time: " + ontime + " times | Absent: " + absent + " time"
                }
                else {
                    grade.text = "A+"
                    stat_label.text = "On-time: 0 times | Absent: 0 time"
                    locationViewModel.resetStats()
                }
            })

        // startLocationService()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        location_recycler_view.apply {
            // set a LinearLayoutManager to handle Android
            // RecyclerView behavior
            layoutManager = LinearLayoutManager(activity)
            // set the custom adapter to the RecyclerView
            adapter = locationAdapter
            setHasFixedSize(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.location_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    //add new schedule
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

    //start location service
    private fun startLocationService() {
        if (checkLocationPermissions()) {
            if (!isLocationEnabled()) {
                Toast.makeText(requireActivity(), "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            else {
                LocationReminderForegroundService.startService(requireContext())
            }
        }
        else requestLocationPermission()
    }

    //Check if the location tracker is enabled in the setting
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    //Check if the user needs permission for location
    private fun checkLocationPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    //Request for permission for location
    private fun requestLocationPermission() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
        if (shouldProvideRationale) {
            Log.d(TAG, "Displaying permission rationale")
            Toast.makeText(requireContext(),"We need permission to enable location reminder", Toast.LENGTH_LONG).show()
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_ID)

        }
        else {
            Log.d(TAG, "Requesting Permission")
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_ID)
        }
    }

    //Called after the user allows or denies our requested permission
    override fun onRequestPermissionsResult(requestCode: Int, permission: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (!isLocationEnabled()) {
                Toast.makeText(requireActivity(), "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            else LocationReminderForegroundService.startService(requireContext())
        }
        else {
            //turn off location reminder
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val editor = sharedPreferences.edit()
            editor.putBoolean("Location Based Reminder", false)
            editor.apply()
        }
    }
}