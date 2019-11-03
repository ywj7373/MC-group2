package com.example.bluecatapp.ui.location

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import android.os.Build
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import androidx.core.app.ActivityCompat
import com.example.bluecatapp.R
import com.example.bluecatapp.data.CurrentLocationData
import com.example.bluecatapp.data.LocationRepository
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlin.concurrent.thread

const val TAG = "RoutineService"
class RoutineService : Service {
    constructor() : super()

    override fun onCreate() {
        super.onCreate()

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
        var mLocationRequest = LocationRequest()
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
            var location: Location = locationResult.lastLocation
            thread(start=true) {
                val currentLocation = CurrentLocationData(location.latitude, location.longitude)
                LocationRepository(application).insertCurrentLocation(currentLocation)
                Log.d(TAG, "Current Location Saved!")
            }
        }
    }
}
