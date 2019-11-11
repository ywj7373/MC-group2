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
import com.example.bluecatapp.data.AlarmTimeData
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
const val LOCATION_TRACKER_INTERVAL: Long = 60000 //60s
const val LOCATION_TRACKER_FASTEST_INTERVAL: Long = 55000 //55s
const val alpha = 20 //min

class RoutineService : Service {
    private val TAG = "Routine Service"
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var odsayService : ODsayService
    private lateinit var notificationManager: NotificationManager
    private var srcLong : Double = 0.0
    private var srcLat : Double = 0.0
    private val routineNotificationId  = "BLUECAT_ROUTINE_ALARM"
    private val locationNotificationId = "BLUE_CAT_LOCATION_ALARM"
    private val notificationTitle = "BLUECAT_APP"
    private var destination: LocationItemData? = null

    constructor() : super()

    override fun onCreate() {
        super.onCreate()

        //observe change in current location
        locationViewModel = LocationViewModel(application)
        locationViewModel.getCurrentLocation().observeForever( object : Observer<CurrentLocationData> {
            override fun onChanged(t: CurrentLocationData?) {
                if (t != null) {
                    srcLat = t.latitude
                    srcLong = t.longitude
                }
            }
        })

        //observe next schedule and update estimated time whenever next alarm changes
        locationViewModel.getNextSchedule().observeForever( object: Observer<LocationItemData> {
            override fun onChanged(t: LocationItemData?) {
                if (t != null) {
                    destination = t
                    updateEstimatedTime(srcLat, srcLong)
                }
                else destination = null
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

    //check if it is time to send notificaton or alarm
    private fun checkTime() {
        //get priority location's estimated time
        thread(start = true) {
            if (destination != null) {
                val alarmTime = LocationRepository(application).getAlarmTime().time
                val time = timeToSeconds(destination!!.time)
                val timeToDest = minToSeconds(alarmTime)
                val currentTime = System.currentTimeMillis()
                val isAlarmed = destination!!.isAlarmed
                val daysMode = destination!!.daysMode
                Log.d(TAG, "alarm rings at: " + Date(time - timeToDest - (alpha * 60 * 1000)).toString())
                Log.d(TAG, "current time: " + Date(currentTime).toString())

                val simpleTimeFormat = SimpleDateFormat("hh:mm:ss", Locale.KOREA)

                //check if current time passed arrival time
                if ((!daysMode && currentTime >= time) || (daysMode && (simpleTimeFormat.format(Date(currentTime))>=(simpleTimeFormat.format(Date(time)))) )) {
                    //set the schedule to done
                    LocationRepository(application).updateDone(true, destination!!.id)
                    Log.d(TAG, "schedule time passed! " + destination!!.x)

                    //check if the user is around schedule's location
                    if (getDistanceFromLatLonInKm(srcLat, srcLong, destination!!.y.toDouble(), destination!!.x.toDouble()) <= 0.1 ) {
                        //------------------------Not yet implemented----------------------


                    }
                    else {
                        Log.d(TAG, "Missed schedule")
                        val text = "You are late!"
                        callNotification(text, 100)
                    }
                }
                //check if current time passed alarm time
                else if (currentTime >= time - timeToDest - (alpha * 60 * 1000) && !isAlarmed) {
                    Log.d(TAG, "entered")
                    //send notification
                    val h = (alarmTime.toInt())/60
                    val m = (alarmTime.toInt())%60
                    val text = "You need to prepare to go to " + destination!!.name +
                            ".\n It takes about " + h + "hours and " + m + " minutes to go there!"

                    callNotification(text, 101)

                    //set alarm to true to stop calling alarm
                    LocationRepository(application).updateIsAlarmed(true, destination!!.id)
                }
            }
        }
    }

    private fun callNotification(text: String, id: Int) {
        val builder = NotificationCompat.Builder(this, locationNotificationId)
            .setContentTitle("BlueCat Alarm")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .build()
        notificationManager.notify(id, builder)
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

    //update current location
    private fun updateCurrentLocation(location: CurrentLocationData) {
        LocationRepository(application).insertCurrentLocation(location)
        Log.d(TAG, "Current Location Updated")
    }

    //update estimatedTime if current location changes
    private fun updateEstimatedTime(srcLong: Double, srcLat: Double) {
        if (destination != null) {
            //calculate estimated time
            estimateTravelTime(srcLat.toString(), srcLong.toString(), destination!!.x, destination!!.y)
        }
        else Log.d(TAG, "No schedule!")
    }

    //call ODsay to estimate Travel time
    private fun estimateTravelTime(sx: String, sy: String, ex: String, ey: String) {
        Log.d(TAG, "$sx $sy $ex $ey")
        odsayService.requestSearchPubTransPath(sx, sy, ex, ey, "0", "0", "0", onEstimateTimeResultCallbackListener)
    }

    //callback method to get json data from ODsay
    private val onEstimateTimeResultCallbackListener = object : OnResultCallbackListener {
        override fun onSuccess(odsayData: ODsayData?, api: API?) {
            Log.d(TAG, "Connection to ODsay successful")
            try {
                if (api == API.SEARCH_PUB_TRANS_PATH) {
                    //update estimated time to the database
                    val inquiryResult = (odsayData!!.json.getJSONObject("result")
                        .getJSONArray("path").get(0) as JSONObject).getJSONObject("info")
                        .getInt("totalTime").toString()

                    thread(start=true) {
                        LocationRepository(application).insertAlarmTime(AlarmTimeData(inquiryResult))
                    }
                    Log.d(TAG, "Estimated Time of " + destination!!.name + " changed to " + inquiryResult)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        override fun onError(i: Int, s: String?, api: API?) {
            Log.d(TAG, i.toString())
        }
    }
}
