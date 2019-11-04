package com.example.bluecatapp.ui.location

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Build
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.bluecatapp.R
import com.example.bluecatapp.data.CurrentLocationData
import com.example.bluecatapp.data.LocationItemData
import com.example.bluecatapp.data.LocationRepository
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.odsay.odsayandroidsdk.API
import com.odsay.odsayandroidsdk.ODsayData
import com.odsay.odsayandroidsdk.ODsayService
import com.odsay.odsayandroidsdk.OnResultCallbackListener
import org.json.JSONException
import org.json.JSONObject
import java.lang.Math.*
import kotlin.concurrent.thread

const val TAG = "RoutineService"
class RoutineService : Service, LifecycleOwner {

    override fun getLifecycle(): Lifecycle {
        return lifecycle
    }

    private lateinit var locationViewModel: LocationViewModel
    private lateinit var odsayService : ODsayService
    private val destination: LocationItemData? = null
    private var srcLong : Double = 0.0
    private var srcLat : Double = 0.0

    constructor() : super()

    override fun onCreate() {
        super.onCreate()

        locationViewModel = LocationViewModel(application)
        locationViewModel.getCurrentLocation().observeForever( object : Observer<CurrentLocationData> {
            override fun onChanged(t: CurrentLocationData?) {
                if (t != null) {
                    srcLong = t.longitude
                    srcLat = t.latitude
                }
            }
        })

        //Initialize ODsayService
        odsayService = ODsayService.init(this, getString(R.string.odsay_key))
        odsayService.setConnectionTimeout(5000)
        odsayService.setReadTimeout(5000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val strId = "BLUECAT_CHANNEL"
            val strTitle = "BLUECAT_APP"
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            var channel: NotificationChannel? = notificationManager.getNotificationChannel(strId)
            if (channel == null) {
                channel = NotificationChannel(strId, strTitle, NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(this, strId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentText("Tracking Location")
                .build()
            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Using FusedLocation
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return START_NOT_STICKY
        }
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 60000
        mLocationRequest.fastestInterval = 50000

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    //callback method that gets lastLocation
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location = locationResult.lastLocation
            val destLong = location.longitude
            val destLat = location.latitude

            val dist = getDistanceFromLatLonInKm(srcLat, srcLong, destLat, destLong)
            Log.d(TAG, dist.toString())

            //If the user moved more than 100m, update current location
            if (dist > 0.1) {
                val currentLocation = CurrentLocationData(destLat, destLong)
                thread(start=true) {
                    updateCurrentLocation(currentLocation)
                    updateEstimatedTime(srcLong, srcLat)
                }
            }
        }
    }

    //update current location
    private fun updateCurrentLocation(location: CurrentLocationData) {
        LocationRepository(application).insertCurrentLocation(location)
        Log.d(TAG, "Current Location Updated")
    }

    //update estimatedTime based on current location
    private fun updateEstimatedTime(srcLong: Double, srcLat: Double) {
        //get the destination address that needs to be alarmed
        val destination: LocationItemData? = LocationRepository(application).getPriorityDestination()
        if (destination != null) {
            Log.d(TAG, destination.name)

            //calculate estimated time
            estimateTravelTime(srcLong.toString(), srcLat.toString(), destination.x, destination.y)
        }
    }

    //callback method to get json data from ODsay
    private val onEstimateTimeResultCallbackListener = object : OnResultCallbackListener {
        override fun onSuccess(odsayData: ODsayData?, api: API?) {
            Log.d(TAG, "Connection to ODsay successful")
            try {
                if (api == API.SEARCH_PUB_TRANS_PATH) {
                    val inquiryResult = (odsayData!!.json.getJSONObject("result").getJSONArray("path").get(0) as JSONObject).getJSONObject("info")
                    //update estimated time to the database
                    Log.d(TAG, "Estimated Time of " + destination!!.name + " changed to " + inquiryResult.getInt("totalTime").toString())
                    LocationRepository(application).updateEstimatedTime(inquiryResult.getInt("totalTime").toString(), destination.id)
                }
            } catch (e: JSONException) {
                Log.d(TAG, "JSONException")
                Toast.makeText(this@RoutineService, "Unable to calculate time distance", Toast.LENGTH_LONG).show()
            }
        }

        override fun onError(i: Int, s: String?, api: API?) {
            Log.d(TAG, "Connection to ODsay failed")
            Toast.makeText(this@RoutineService, "Unable to calculate time distance", Toast.LENGTH_LONG).show()
        }
    }

    //call ODsay to estimate Travel time
    private fun estimateTravelTime(sx: String, sy: String, ex: String, ey: String) {
        odsayService.requestSearchPubTransPath(sx, sy, ex, ey, "0", "0", "0", onEstimateTimeResultCallbackListener)
    }

    //get distance between two coordinates in KM
    private fun getDistanceFromLatLonInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // Radius of the earth in km
        val dLat = deg2rad(lat2-lat1) // deg2rad below
        val dLon = deg2rad(lon2-lon1)
        val a = sin(dLat/2) * sin(dLat/2) +
                    cos(deg2rad(lat1)) * cos(deg2rad(lat2)) *
                    sin(dLon/2) * sin(dLon/2)
        return  R * 2 * atan2(sqrt(a), sqrt(1-a)) // Distance in km
    }

    private fun deg2rad(deg: Double): Double {
        return deg * (PI/180)
    }
}
