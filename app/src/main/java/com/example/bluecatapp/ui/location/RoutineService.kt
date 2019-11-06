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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

const val ODsayTimeout = 5000
const val LOCATION_TRACKER_INTERVAL: Long = 60000
const val LOCATION_TRACKER_FASTEST_INTERVAL: Long = 55000
const val alpha = 20

class RoutineService : Service {
    private val TAG = "Routine Service"
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var odsayService : ODsayService
    private val destination: LocationItemData? = null
    private var srcLong : Double = 0.0
    private var srcLat : Double = 0.0
    private val routineNotificationId  = "BLUECAT_ROUTINE_ALARM"
    private val locationNotificationId = "BLUE_CAT_LOCATION_ALARM"
    private val notificationTitle = "BLUECAT_APP"
    private lateinit var notificationManager: NotificationManager

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
        odsayService.setConnectionTimeout(ODsayTimeout)
        odsayService.setReadTimeout(ODsayTimeout)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Initialize notification manager
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            //Create new notification channel
            var channel: NotificationChannel? = notificationManager.getNotificationChannel(routineNotificationId)
            if (channel == null) {
                channel = NotificationChannel(routineNotificationId, notificationTitle, NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }

            var channel2 = notificationManager.getNotificationChannel(locationNotificationId)
            if (channel2 == null) {
                channel2 = NotificationChannel(locationNotificationId, notificationTitle, NotificationManager.IMPORTANCE_HIGH)
                channel2.enableVibration(true)
                channel2.enableLights(true)
                notificationManager.createNotificationChannel(channel2)
            }

            //start foreground notification
            val builder = NotificationCompat.Builder(this, routineNotificationId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentText("Tracking Location")
                .build()
            startForeground(1, builder)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        checkCurrentLocation()
        checkTime()
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

    //check if it is time to send notificaiton or alarm
    private fun checkTime() {
        //get priority location's estimated time
        thread(start = true) {
            val destination: LocationItemData? = LocationRepository(application).getPriorityDestination()
            if (destination != null) {
                val time = timeToSeconds(destination.time)
                val timeToDest = minToSeconds(destination.timeToDest)
                val currentTime = System.currentTimeMillis()
                val isAlarmed = destination.isAlarmed
                Log.d(TAG, "alarm rings at: " + Date(time - timeToDest - (alpha * 60 * 1000)).toString())
                Log.d(TAG, "current time: " + Date(currentTime).toString())

                //check if current time passed arrival time
                if (currentTime >= time) {
                    Log.d(TAG, "update estimated time")
                    //recalculate estimated time
                    updateEstimatedTime(srcLat, srcLong)
                }
                //check if current time passed alarm time
                else if (currentTime >= time - timeToDest - (alpha * 60 * 1000) && !isAlarmed) {
                    Log.d(TAG, "entered")
                    //send notification
                    val h = (destination.timeToDest.toInt())/60
                    val m = (destination.timeToDest.toInt())%60
                    val text = "You need to prepare to go to " + destination.name +
                            ".\n It takes about " + h + "hours and " + m + " minutes to go there!"

                    val builder = NotificationCompat.Builder(this, locationNotificationId)
                        .setContentTitle("BlueCat Alarm")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                        .build()
                    notificationManager.notify(1234, builder)

                    //set alarm to true to stop calling alarm
                    LocationRepository(application).updateIsAlarmed(true, destination.id)
                }
            }
        }
    }

    private fun timeToSeconds(time: String): Long {
        val sf = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.KOREA)
        val date: Date? = sf.parse(time)
        return date!!.time
    }

    private fun minToSeconds(time: String): Long {
        val t: Long = time.toLong()
        return t * 60 * 1000
    }

    //track current location
    private fun checkCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = LOCATION_TRACKER_INTERVAL
        mLocationRequest.fastestInterval = LOCATION_TRACKER_FASTEST_INTERVAL

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
    }

    //callback method that gets lastLocation
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location = locationResult.lastLocation
            val destLong = location.longitude
            val destLat = location.latitude

            val dist = getDistanceFromLatLonInKm(srcLat, srcLong, destLat, destLong)
            Log.d(TAG, dist.toString())
            Log.d(TAG, srcLat.toString() + " " + srcLong.toString())

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
        else Log.d(TAG, "No schedule!")
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
                //Need to handle error
                //-----------------------Not yet implemented--------------------------


            }
        }

        override fun onError(i: Int, s: String?, api: API?) {
            Log.d(TAG, "Connection to ODsay failed")
            Toast.makeText(this@RoutineService, "Connection failed!", Toast.LENGTH_LONG).show()
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
