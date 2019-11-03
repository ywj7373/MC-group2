package com.example.bluecatapp.ui.location

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluecatapp.R
import com.example.bluecatapp.data.LocationItemData
import kotlinx.android.synthetic.main.fragment_location.*

class LocationFragment : Fragment() {
    private val TAG = "Location Fragment"
    private lateinit var locationViewModel: LocationViewModel
    private val PERMISSION_ID = 270
    private val locationAdapter = LocationAdapter()
    private lateinit var alarmManager: AlarmManager
    private val mAlarmIntent = Intent("")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        //initialize view model
        val root = inflater.inflate(R.layout.fragment_location, container, false)
        locationViewModel = ViewModelProviders.of(this).get(LocationViewModel::class.java)

        locationViewModel.getAllLocationItems().observe(this,
            Observer<List<LocationItemData>> { t -> locationAdapter.setLocationItems(t!!) })

        startLocationService()

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

        //call add location activity
        addItemButton.setOnClickListener {
            val intent = Intent(requireContext(), AddLocationActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.location_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    //implement delete all items function
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.location_delete_all_locations -> {
                locationViewModel.deleteAllLocationItems()
                Toast.makeText(requireContext(), "All Location Items deleted!", Toast.LENGTH_SHORT).show()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
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
                RoutineReceiver().setRoutine(requireContext())
            }
        }
        else {
            requestLocationPermission()
        }
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
            //-----------------------permission rationale not yet implemented -------------------------------

        }
        else {
            Log.d(TAG, "Requesting Permission")
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_ID)
        }
    }

    //Called after the user allows or denies our requested permission
    override fun onRequestPermissionsResult(requestCode: Int, permission: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                RoutineReceiver().setRoutine(requireContext())
            }
        }
    }
}