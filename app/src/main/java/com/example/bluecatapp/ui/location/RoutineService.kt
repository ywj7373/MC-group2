package com.example.bluecatapp.ui.location

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import android.os.Build
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import androidx.core.app.ActivityCompat


class RoutineService : Service {
    private val TAG = "RoutineService"
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

            val notification = NotificationCompat.Builder(this, strId).build()
            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Using FusedLocation
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return START_NOT_STICKY
        }
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if(location == null) {
                    Log.e(TAG, "location get fail")
                } else {
                    Log.d(TAG, "${location.latitude} , ${location.longitude}")
                    Toast.makeText(this@RoutineService, "Longitude = " + location.longitude + ", Latitude = " + location.latitude + ", Altitude = " + location.altitude, Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Log.e("RoutineDebug", "location error : ${it.message}")
                it.printStackTrace()
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
}
