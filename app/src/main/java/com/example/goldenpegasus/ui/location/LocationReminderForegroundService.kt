package com.example.goldenpegasus.ui.location

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.media.AudioAttributes
import android.media.AudioAttributes.CONTENT_TYPE_MUSIC
import android.media.AudioAttributes.USAGE_ALARM
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.example.goldenpegasus.R
import com.example.goldenpegasus.data.*
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.odsay.odsayandroidsdk.API
import com.odsay.odsayandroidsdk.ODsayData
import com.odsay.odsayandroidsdk.ODsayService
import com.odsay.odsayandroidsdk.OnResultCallbackListener
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.*

const val ODsayTimeout = 5000
const val LOCATION_TRACKER_INTERVAL: Long = 60000 //60s
const val LOCATION_TRACKER_FASTEST_INTERVAL: Long = 55000 //55s
const val NOTIF_ID = 1
const val NOTIF_ID2 = 2
const val NOTIF_ID3 = 3
const val ROUTINE_INTERVAL: Long = 60000
const val DEFAULT_TRAVEL_TIME = "20"
const val VIBRATION_TIME: Long = 1000
var ringtone: Ringtone? = null

object ringtoneInfo {
    var isPlaying = false
}

class LocationReminderForegroundService : Service() {
    private val TAG = "Routine Service"
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var odsayService: ODsayService
    private lateinit var notificationManager: NotificationManager
    private lateinit var sharedPreferences: SharedPreferences
    private var srcLong: Double = 0.0
    private var srcLat: Double = 0.0
    private val routineNotificationId = "Location reminder Routine alarm"
    private val locationNotificationId = "Location reminder alarm"
    private val notificationTitle = "Location reminder"
    private var destination: LocationItemData? = null
    private var travelTime: String = DEFAULT_TRAVEL_TIME
    private val simpleTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.KOREA)

    companion object {
        fun startService(context: Context) {
            val startIntent = Intent(context, LocationReminderForegroundService::class.java)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, LocationReminderForegroundService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        startNotification()

        // Observe change in current location
        locationViewModel = LocationViewModel(application)
        locationViewModel.getCurrentLocation()
            .observeForever(object : Observer<CurrentLocationData> {
                override fun onChanged(t: CurrentLocationData?) {
                    if (t != null) {
                        srcLat = t.latitude
                        srcLong = t.longitude
                        updateEstimatedTime(srcLat, srcLong)
                    }
                }
            })

        // Observe next schedule and update estimated time whenever next alarm changes
        locationViewModel.getNextSchedule().observeForever(object : Observer<LocationItemData> {
            override fun onChanged(t: LocationItemData?) {
                if (t != null) {
                    destination = t
                    if (srcLat != 0.0 && srcLong != 0.0)
                        updateEstimatedTime(srcLat, srcLong)
                } else {
                    destination = null
                    updateNotification("No Alarm")
                }
            }
        })

        locationViewModel.getTravelTime().observeForever(object : Observer<TravelTimeData> {
            override fun onChanged(t: TravelTimeData?) {
                if (t != null) {
                    travelTime = t.time
                    checkTime()
                }
            }
        })

        // Initialize ODsayService
        odsayService = ODsayService.init(this, getString(R.string.odsay_key))
        odsayService.setConnectionTimeout(ODsayTimeout)
        odsayService.setReadTimeout(ODsayTimeout)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler = Handler()
        runnable = Runnable {
            Log.d(TAG, "New routine!")
            if (destination != null) {
                val destinationTime = timeToSeconds(destination!!.time)
                Log.d(TAG, destination!!.time)
                val currentTime = System.currentTimeMillis()
                val diff = (destinationTime - currentTime) / 60000
                Log.d(TAG, diff.toString())
                if (diff <= 180) {
                    Log.d(TAG, "Start getting current location")
                    checkDate()
                    checkCurrentLocation()
                    checkTime()
                }
            }
            else {
                updateNotification("No Alarm")
            }
            handler.postDelayed(runnable, ROUTINE_INTERVAL)
        }
        handler.post(runnable)

        return START_NOT_STICKY
    }

    private fun startNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Initialize notification manager
            notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Routine notification channel
            var channel: NotificationChannel? =
                notificationManager.getNotificationChannel(routineNotificationId)
            if (channel == null) {
                channel = NotificationChannel(
                    routineNotificationId,
                    notificationTitle,
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            // Alarm Notification Channel
            var channel2 = notificationManager.getNotificationChannel(locationNotificationId)
            if (channel2 == null) {
                channel2 = NotificationChannel(
                    locationNotificationId,
                    notificationTitle,
                    NotificationManager.IMPORTANCE_HIGH
                )
                channel2.enableVibration(true)
                channel2.enableLights(true)
                notificationManager.createNotificationChannel(channel2)
            }

            startForeground(NOTIF_ID, callMainNotification("No active alarm"))
        }
    }

    private fun callMainNotification(text: String): Notification {
        val contentIntent =
            PendingIntent.getActivity(this, 0, Intent(this, LocationFragment::class.java), 0)

        return NotificationCompat.Builder(this, routineNotificationId)
            .setContentText(text)
            .setOnlyAlertOnce(true) // So when data is updated don't make sound and alert in android 8.0+
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_location_on_black_24dp)
            .setContentIntent(contentIntent)
            .build()
    }

    // Update text in foreground notification
    private fun updateNotification(text: String) {
        val notification: Notification = callMainNotification(text)
        notificationManager.notify(NOTIF_ID, notification)
    }

    private fun callAlarmNotification(text: String, id: Int) {
        val intent = Intent(this, AlarmNotificationDeletedReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)
        val builder = NotificationCompat.Builder(this, locationNotificationId)
            .setContentTitle("Reminder!")
            .setSmallIcon(R.drawable.ic_location_on_black_24dp)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setDeleteIntent(pendingIntent)
            .build()
        //builder.flags = (builder.flags or Notification.FLAG_AUTO_CANCEL)
        if (id == NOTIF_ID3) {
            val alarmSoundEnabled = sharedPreferences.getBoolean("Enable Alarm Sound", false)
            if (alarmSoundEnabled) {          // When alarm sound preference is on
                soundAlarm()
            }
        }
        notificationManager.notify(id, builder)
    }

    // Check if it is time to send notification or alarm
    private fun checkTime() {
        // Get priority location's estimated time
        thread(start = true) {
            if (destination != null) {
                Log.d(TAG, travelTime)
                val time = timeToSeconds(destination!!.time)
                val timeToDest = minToSeconds(travelTime)
                val currentTime = System.currentTimeMillis()
                val isAlarmed = destination!!.isAlarmed
                val daysMode = destination!!.daysMode
                val preparationTime =
                    sharedPreferences.getString("Preparation_time", "20")!!.toInt()
                val alarmTime = time - timeToDest - (preparationTime * 60 * 1000)
                val destinationTime = simpleTimeFormat.format(Date(time))
                val msg = getTravelTimeMsg(time)
                val alarmText = "Alarm rings at: " + SimpleDateFormat(
                    "yyyy MMM dd, EEE, h:mm a\n", Locale.KOREA
                ).format(Date(alarmTime))

                Log.d(TAG, isAlarmed.toString())
                if (!isAlarmed) updateNotification(alarmText)

                // Check if current time passed arrival time
                if ((!daysMode && currentTime >= time)
                    || (daysMode && (simpleTimeFormat.format(Date(currentTime)) >= (simpleTimeFormat.format(
                        Date(time)
                    ))))
                ) {
                    // Set the schedule to done
                    LocationRepository(application).updateDone(true, destination!!.id)
                    Log.d(TAG, "schedule time passed! " + destination!!.x)

                    // Check if the user is around schedule's location
                    if (getDistanceFromLatLonInKm(
                            srcLat,
                            srcLong,
                            destination!!.y.toDouble(),
                            destination!!.x.toDouble()
                        ) <= 0.3
                    ) {
                        Log.d(TAG, "Made on time")
                        LocationRepository(application).increaseOntime()
                    } else {
                        Log.d(TAG, "Missed schedule")
                        val text = "You are late!"
                        callAlarmNotification(text, NOTIF_ID2)
                        LocationRepository(application).increaseAbsent()
                    }
                }
                // Check if current time passed alarm time
                else if (currentTime >= alarmTime && !isAlarmed) {
                    Log.d(TAG, "passed alarm time")
                    // Vibrate
                    val v: Vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        v.vibrate(
                            VibrationEffect.createOneShot(
                                VIBRATION_TIME,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        );
                    } else {
                        // Deprecated in API 26
                        v.vibrate(VIBRATION_TIME)
                    }

                    // Send notification
                    callAlarmNotification(msg, NOTIF_ID3)
                    updateNotification(
                        "Next Schedule: " + destination!!.name + " at " + destinationTime.substring(
                            0,
                            5
                        )
                    )

                    // Set alarm to true to stop calling alarm
                    LocationRepository(application).updateIsAlarmed(true, destination!!.id)
                }
            }
        }
    }

    private fun getTravelTimeMsg(time: Long): String {
        val destinationTime = simpleTimeFormat.format(Date(time))
        val h = (travelTime.toInt()) / 60
        val m = (travelTime.toInt()) % 60
        val firstText =
            "Schedule: " + destination!!.name + " at " + destinationTime.substring(0, 5) + "\n"
        var secondText = "Travel Time: " + h + "hours and " + m + " minutes"
        if (h == 0) {
            secondText = "Travel Time: $m minutes"
        }
        if (travelTime.toInt() == 10) {
            secondText = "Travel Time: less than 10 minutes"
        }
        return firstText + secondText
    }

    // Check if date has changed or not and if changed reset repeated schedule
    private fun checkDate() {
        val dateData: DateData? = LocationRepository(application).getCurrentDate()
        val currentDate: String = SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(Date())
        if (dateData == null) {
            LocationRepository(application).updateCurrentDate()
            return
        }

        if (dateData.mcurrent_date != currentDate) {   // If date has changed
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DATE, 6)
            var futureDate = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(calendar.time)
            val dayOfTodayEncoded = calendar.get(Calendar.DAY_OF_WEEK)
            var iteratingDay = (dayOfTodayEncoded + 6) % 7
            var dayOfToday = ""

            // Iteration for sorting
            while (iteratingDay != dayOfTodayEncoded) {
                dayOfToday = when (iteratingDay) {
                    1 -> "%SUN%"
                    2 -> "%MON%"
                    3 -> "%TUE%"
                    4 -> "%WED%"
                    5 -> "%THU%"
                    6 -> "%FRI%"
                    7 -> "%SAT%"
                    else -> ""
                }

                LocationRepository(application).updateToTodayDateDays(futureDate, dayOfToday)

                iteratingDay--
                if (iteratingDay == -1)
                    iteratingDay = 7
                calendar.add(Calendar.DATE, -1)
                futureDate = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(calendar.time)
            }
            run {
                //One more for today
                dayOfToday = when (iteratingDay) {
                    1 -> "%SUN%"
                    2 -> "%MON%"
                    3 -> "%TUE%"
                    4 -> "%WED%"
                    5 -> "%THU%"
                    6 -> "%FRI%"
                    7 -> "%SAT%"
                    else -> ""
                }

                LocationRepository(application).updateToTodayDateDays(futureDate, dayOfToday)

                iteratingDay--
                if (iteratingDay == -1)
                    iteratingDay = 7
                calendar.add(Calendar.DATE, -1)
                futureDate = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(calendar.time)
            }

            LocationRepository(application).updateToTodayDateDays(currentDate, dayOfToday)
            LocationRepository(application).updateAllNotDoneDays()
            LocationRepository(application).updateCurrentDate()
            Log.d(
                "checkDate",
                "Date has changed : " + dateData.mcurrent_date + ", " + currentDate
            )
        } else {
            Log.d(
                "checkDate",
                "Date not changed : " + dateData.mcurrent_date + ", " + currentDate
            )
        }

    }

    // Track current location
    private fun checkCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = LOCATION_TRACKER_INTERVAL
        mLocationRequest.fastestInterval = LOCATION_TRACKER_FASTEST_INTERVAL

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient!!.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()
        )
    }

    // Callback method that gets lastLocation
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location = locationResult.lastLocation
            val destLong = location.longitude
            val destLat = location.latitude
            val dist = getDistanceFromLatLonInKm(srcLat, srcLong, destLat, destLong)

            // If the user moved more than 100m, update current location
            if (dist > 0.1) {
                thread(start = true) {
                    val currentLocation = CurrentLocationData(destLat, destLong)
                    updateCurrentLocation(currentLocation)
                }
            }
        }
    }

    // Update current location
    private fun updateCurrentLocation(location: CurrentLocationData) {
        LocationRepository(application).insertCurrentLocation(location)
        Log.d(TAG, "Current Location Updated")
    }

    // Update estimatedTime if current location changes
    private fun updateEstimatedTime(long: Double, lat: Double) {
        if (destination != null) {
            //calculate estimated time
            estimateTravelTime(
                lat.toString(),
                long.toString(),
                destination!!.x,
                destination!!.y
            )
        } else Log.d(TAG, "No schedule!")
    }

    // Call ODsay to estimate Travel time
    private fun estimateTravelTime(sx: String, sy: String, ex: String, ey: String) {
        Log.d(TAG, "$sx $sy $ex $ey")
        odsayService.requestSearchPubTransPath(
            sx,
            sy,
            ex,
            ey,
            "0",
            "0",
            "0",
            onEstimateTimeResultCallbackListener
        )
    }

    // Callback method to get json data from ODsay
    private val onEstimateTimeResultCallbackListener = object : OnResultCallbackListener {
        override fun onSuccess(odsayData: ODsayData?, api: API?) {
            Log.d(TAG, "Connection to ODsay successful")
            try {
                if (api == API.SEARCH_PUB_TRANS_PATH) {
                    // Update estimated time to the database
                    val response = odsayData!!.json
                    // Handle error
                    if (response.has("error")) {
                        val msg = response.getJSONObject("error").getString("msg")
                        Log.d(TAG, msg)
                        thread(start = true) {
                            LocationRepository(application).insertTravelTime(TravelTimeData("10"))
                        }
                    }
                    // Update estimated time
                    else {
                        val inquiryResult = (response.getJSONObject("result")
                            .getJSONArray("path").get(0) as JSONObject).getJSONObject("info")
                            .getInt("totalTime").toString()

                        thread(start = true) {
                            LocationRepository(application).insertTravelTime(
                                TravelTimeData(
                                    inquiryResult
                                )
                            )
                        }
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        override fun onError(i: Int, s: String?, api: API?) {}
    }

    // Get distance between two coordinates in KM
    private fun getDistanceFromLatLonInKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val R = 6371 // Radius of the earth in km
        val dLat = deg2rad(lat2 - lat1) // deg2rad below
        val dLon = deg2rad(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(deg2rad(lat1)) * cos(deg2rad(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a)) // Distance in km
    }

    // Convert degree to radian
    private fun deg2rad(deg: Double): Double {
        return deg * (PI / 180)
    }

    // Convert date to seconds
    private fun timeToSeconds(time: String): Long {
        val sf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
        val date: Date? = sf.parse(time)
        return date!!.time
    }

    // Convert min to seconds
    private fun minToSeconds(time: String): Long {
        val t: Long = time.toLong()
        return t * 60 * 1000
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun soundAlarm() {
        // Alarm Sound
        if (!ringtoneInfo.isPlaying) {                      // Only play ringtone when ringtone is not playing
            try {
                // For morning alarm sound
                val alarmSound: Uri =
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ringtone = RingtoneManager.getRingtone(this, alarmSound)
                ringtone!!.audioAttributes = AudioAttributes.Builder()
                    .setUsage(USAGE_ALARM)
                    .setContentType(CONTENT_TYPE_MUSIC).build()
                ringtone!!.play()
                ringtoneInfo.isPlaying = true
            } catch (e: Exception) {
                Log.d("ALARM SOUND GENERATION failed : ", e.toString())
            }
        }
    }
}

class AlarmNotificationDeletedReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        Log.d("ALARM NOTIFICATION DELETED", "Deleted")
        if (ringtone != null) {
            ringtone!!.stop()
            ringtoneInfo.isPlaying = false
        }
    }
}