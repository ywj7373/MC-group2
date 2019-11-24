package com.example.bluecatapp.ui.location

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.os.Build
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.example.bluecatapp.R
import com.example.bluecatapp.data.*
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
const val NOTIF_ID = 1
const val NOTIF_ID2 = 2

class RoutineService : Service {
    private val TAG = "Routine Service"
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var odsayService : ODsayService
    private lateinit var notificationManager: NotificationManager
    private lateinit var sharedPreferences: SharedPreferences
    private var srcLong : Double = 0.0
    private var srcLat : Double = 0.0
    private val routineNotificationId  = "BLUECAT_ROUTINE_ALARM"
    private val locationNotificationId = "BLUE_CAT_LOCATION_ALARM"
    private val notificationTitle = "BLUECAT_APP"
    private var destination: LocationItemData? = null

    constructor() : super()

    override fun onCreate() {
        super.onCreate()

        startNotification()

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val on = sharedPreferences.getBoolean("Location Based Reminder", true)

        //observe change in current location
        locationViewModel = LocationViewModel(application)
        locationViewModel.getCurrentLocation().observeForever( object : Observer<CurrentLocationData> {
            override fun onChanged(t: CurrentLocationData?) {
                if (t != null) {
                    srcLat = t.latitude
                    srcLong = t.longitude
                    updateEstimatedTime(srcLat, srcLong)
                }
            }
        })

        //observe next schedule and update estimated time whenever next alarm changes
        locationViewModel.getNextSchedule().observeForever( object: Observer<LocationItemData> {
            override fun onChanged(t: LocationItemData?) {
                if (t != null) {
                    destination = t
                    if (srcLat != 0.0 && srcLong != 0.0)
                        updateEstimatedTime(srcLat, srcLong)
                    if (on) checkTime()
                    else updateNotification("Alarm Disabled")
                }
                else {
                    destination = null
                    updateNotification("No Alarm")
                }
            }
        })

        //Initialize ODsayService
        odsayService = ODsayService.init(this, getString(R.string.odsay_key))
        odsayService.setConnectionTimeout(ODsayTimeout)
        odsayService.setReadTimeout(ODsayTimeout)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val on = sharedPreferences.getBoolean("Location Based Reminder", true)

        if (on) {
            checkDate()
            checkCurrentLocation()
            checkTime()
        }
        else {
            updateNotification("Alarm Disabled")
        }
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

    private fun startNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Initialize notification manager
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            //Routine notification channel
            var channel: NotificationChannel? = notificationManager.getNotificationChannel(routineNotificationId)
            if (channel == null) {
                channel = NotificationChannel(routineNotificationId, notificationTitle, NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }

            //Alarm Notification Channel
            var channel2 = notificationManager.getNotificationChannel(locationNotificationId)
            if (channel2 == null) {
                channel2 = NotificationChannel(locationNotificationId, notificationTitle, NotificationManager.IMPORTANCE_HIGH)
                channel2.enableVibration(true)
                channel2.enableLights(true)
                notificationManager.createNotificationChannel(channel2)
            }

            startForeground(NOTIF_ID, callMainNotification("No Alarm"))
        }
    }

    private fun callMainNotification(text: String): Notification {
        val contentIntent= PendingIntent.getActivity(this, 0, Intent(this, LocationFragment::class.java),0)

        return NotificationCompat.Builder(this,routineNotificationId)
            .setContentText(text)
            .setOnlyAlertOnce(true) // so when data is updated don't make sound and alert in android 8.0+
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification: Notification = callMainNotification(text)
        notificationManager.notify(NOTIF_ID, notification)
    }

    private fun callAlarmNotification(text: String, id: Int) {
        val builder = NotificationCompat.Builder(this, locationNotificationId)
            .setContentTitle("BlueCat Alarm")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .build()
        notificationManager.notify(id, builder)
    }

    //check if it is time to send notificaton or alarm
    private fun checkTime() {
        //get priority location's estimated time
        thread(start = true) {
            if (destination != null) {
                val travelTime = LocationRepository(application).getTravelTime().time
                Log.d(TAG, travelTime)
                Log.d(TAG, destination!!.name)
                Log.d(TAG, destination!!.time)

                //when destination time is 12 -> the time is measured as 24
                val time = timeToSeconds(destination!!.time)
                val timeToDest = minToSeconds(travelTime)
                val currentTime = System.currentTimeMillis()
                val isAlarmed = destination!!.isAlarmed
                val daysMode = destination!!.daysMode
                val preparationTime = sharedPreferences.getString("Preparation_time", "20")!!.toInt()
                val alarmTime = time - timeToDest - (preparationTime * 60 * 1000)

                val alarmText = "Alarm rings at: " + Date(alarmTime).toString()
                updateNotification(alarmText)

                Log.d(TAG, "preparation time: " + preparationTime.toString())
                Log.d(TAG, "alarm rings at: " + Date(alarmTime).toString())
                Log.d(TAG, "current time: " + Date(currentTime).toString())

                val simpleTimeFormat = SimpleDateFormat("hh:mm:ss", Locale.KOREA)

                //check if current time passed arrival time
                if ((!daysMode && currentTime >= time) || (daysMode && (simpleTimeFormat.format(Date(currentTime))>=(simpleTimeFormat.format(Date(time)))) )) {
                    //set the schedule to done
                    LocationRepository(application).updateDone(true, destination!!.id)
                    Log.d(TAG, "schedule time passed! " + destination!!.x)

                    //check if the user is around schedule's location
                    if (getDistanceFromLatLonInKm(srcLat, srcLong, destination!!.y.toDouble(), destination!!.x.toDouble()) <= 0.1 ) {
                        Log.d(TAG, "Made on time")
                        LocationRepository(application).increaseOntime()
                    }
                    else {
                        Log.d(TAG, "Missed schedule")
                        val text = "You are late!"
                        callAlarmNotification(text, NOTIF_ID2)
                        LocationRepository(application).increaseAbsent()
                    }
                }
                //check if current time passed alarm time
                else if (currentTime >= alarmTime && !isAlarmed) {
                    Log.d(TAG, "entered")
                    //send notification
                    val h = (travelTime.toInt())/60
                    val m = (travelTime.toInt())%60
                    val text = "You need to prepare to go to " + destination!!.name +
                            ".\n It takes about " + h + "hours and " + m + " minutes to go there!"

                    callAlarmNotification(text, 101)

                    //set alarm to true to stop calling alarm
                    LocationRepository(application).updateIsAlarmed(true, destination!!.id)
                }
            }
        }
    }

    //check if date has changed or not and if changed reset repeated schedule
    private fun checkDate() {
        val dateData:DateData = LocationRepository(application).getCurrentDate()
        val current_date: String = SimpleDateFormat("yyyyMMdd").format(Date())
        if(dateData==null) {
            LocationRepository(application).updateCurrentDate()
            return
        }

        if(dateData.mcurrent_date!=current_date) {              // if date has changed
            var calendar = Calendar.getInstance()
            calendar.add(Calendar.DATE, 6)
            var future_date = SimpleDateFormat("yyyy-MM-dd").format(calendar.time)
            val dayOfToday_encoded = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            var iterating_day = (dayOfToday_encoded + 6) % 7
            var dayOfToday:String = ""

            // Iteration for sorting
            while(iterating_day != dayOfToday_encoded) {
                dayOfToday   = when(iterating_day) {
                    1 -> "%SUN%"
                    2 -> "%MON%"
                    3 -> "%TUE%"
                    4 -> "%WED%"
                    5 -> "%THU%"
                    6 -> "%FRI%"
                    7 -> "%SAT%"
                    else -> ""
                }

                LocationRepository(application).updateToTodayDateDays(future_date, dayOfToday)

                iterating_day = iterating_day - 1
                if(iterating_day == -1)
                    iterating_day = 7
                calendar.add(Calendar.DATE, -1)
                future_date = SimpleDateFormat("yyyy-MM-dd").format(calendar.time)
            }
            run {           // one more for today
                dayOfToday   = when(iterating_day) {
                    1 -> "%SUN%"
                    2 -> "%MON%"
                    3 -> "%TUE%"
                    4 -> "%WED%"
                    5 -> "%THU%"
                    6 -> "%FRI%"
                    7 -> "%SAT%"
                    else -> ""
                }

                LocationRepository(application).updateToTodayDateDays(future_date, dayOfToday)

                iterating_day = iterating_day - 1
                if(iterating_day == -1)
                    iterating_day = 7
                calendar.add(Calendar.DATE, -1)
                future_date = SimpleDateFormat("yyyy-MM-dd").format(calendar.time)
            }

            LocationRepository(application).updateToTodayDateDays(current_date, dayOfToday)
            LocationRepository(application).updateAllNotDoneDays()
            LocationRepository(application).updateCurrentDate()
            Log.d("checkDate", "Date has changed : " + dateData.mcurrent_date + ", " + current_date)
        }
        else {
            Log.d("checkDate", "Date not changed : " + dateData.mcurrent_date + ", " + current_date)
        }

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
                thread(start=true) {
                    val currentLocation = CurrentLocationData(destLat, destLong)
                    updateCurrentLocation(currentLocation)
                }
            }
        }
    }

    //update current location
    private fun updateCurrentLocation(location: CurrentLocationData) {
        LocationRepository(application).insertCurrentLocation(location)
        Log.d(TAG, "Current Location Updated")
    }

    //update estimatedTime if current location changes
    private fun updateEstimatedTime(long: Double, lat: Double) {
        if (destination != null) {
            //calculate estimated time
            estimateTravelTime(lat.toString(), long.toString(), destination!!.x, destination!!.y)
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
                    val response = odsayData!!.json
                    //handle error
                    if (response.has("error")) {
                        val msg = response.getJSONObject("error").getString("msg")
                        Log.d(TAG, msg)
                        thread(start = true) {
                            LocationRepository(application).insertTravelTime(TravelTimeData("10"))
                        }
                    }
                    //update estimated time
                    else {
                        val inquiryResult = (response.getJSONObject("result")
                            .getJSONArray("path").get(0) as JSONObject).getJSONObject("info")
                            .getInt("totalTime").toString()

                        thread(start = true) {
                            LocationRepository(application).insertTravelTime(TravelTimeData(inquiryResult))
                        }
                        Log.d(TAG, "Estimated Time of " + destination!!.name + " changed to " + inquiryResult)
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        override fun onError(i: Int, s: String?, api: API?) {}
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

    //convert degree to radian
    private fun deg2rad(deg: Double): Double {
        return deg * (PI/180)
    }

    //convert date to seconds
    private fun timeToSeconds(time: String): Long {
        val sf = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.KOREA)
        val date: Date? = sf.parse(time)
        return date!!.time
    }

    //convert min to seconds
    private fun minToSeconds(time: String): Long {
        val t: Long = time.toLong()
        return t * 60 * 1000
    }
}
