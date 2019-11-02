package com.example.bluecatapp.ui.location

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import java.util.*
import android.os.Build
import android.app.NotificationManager
import android.app.NotificationChannel
import android.R
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.core.app.ActivityCompat


class RoutineService : Service {
    constructor() : super() {
    }

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

    override fun onDestroy() {
        super.onDestroy()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true)
        }
    }

    private lateinit var lm : LocationManager

    override fun onBind(p0: Intent?): IBinder? {
        null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RoutineDebug", "onStartCommand called")
        //Toast.makeText(this, "ROUTINE JOB CALLED", Toast.LENGTH_SHORT).show()

        // Using LocationManager
        /*
        lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val mLocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                Log.d("RoutineDebug", "onLocationChanged called")
                val longitude = location?.longitude
                val latitude = location?.latitude
                val altitude = location?.altitude

                Toast.makeText(this@RoutineService, "Longitude = " + longitude + ", Latitude = " + latitude + ", Altitude = " + altitude, Toast.LENGTH_SHORT).show()
                Log.d("RoutineDebug", "Acquired Location :: "+"Longitude = " + longitude + ", Latitude = " + latitude + ", Altitude = " + altitude)
                lm.removeUpdates(this)
                Log.d("RoutineDebug", "onLocationChanged finished")
            }

            override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
            }

            override fun onProviderDisabled(p0: String?) {
            }

            override fun onProviderEnabled(p0: String?) {
            }
        }

        try {
            //lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1f, mLocationListener)
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f, mLocationListener)
        }
        catch (e:SecurityException) {

        }
        Log.d("RoutineDebug", "onStartCommand finished")
         */


        // Using FusedLocation
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
             && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
              return START_NOT_STICKY
        }
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if(location == null) {
                    Log.e("RoutineDebug", "location get fail")
                } else {
                    Log.d("RoutineDebug", "${location.latitude} , ${location.longitude}")
                    Toast.makeText(this@RoutineService, "Longitude = " + location.longitude + ", Latitude = " + location.latitude + ", Altitude = " + location.altitude, Toast.LENGTH_SHORT).show()
                }
            }
                .addOnFailureListener {
                    Log.e("RoutineDebug", "location error : ${it.message}")
                    it.printStackTrace()
                }



        return START_NOT_STICKY
    }
}
