package com.example.goldenpegasus.ui.location

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.goldenpegasus.R
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker

class MapActivity: AppCompatActivity(), OnMapReadyCallback {
    private var lat: Double = 0.0
    private var long: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.fragment_map)

        val latStr = intent.getStringExtra("Latitude")
        val longStr = intent.getStringExtra("Longitude")

        if (latStr != null) lat = latStr.toDouble()
        if (longStr != null) long = longStr.toDouble()

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as MapFragment?
            ?: run {
                val options = NaverMapOptions().camera(CameraPosition(
                    NaverMap.DEFAULT_CAMERA_POSITION.target, NaverMap.DEFAULT_CAMERA_POSITION.zoom, 30.0, 45.0)
                )
                MapFragment.newInstance(options).also {
                    supportFragmentManager.beginTransaction().add(R.id.map, it).commit()
                }
            }

        mapFragment.getMapAsync(this)
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }

    override fun onMapReady(naverMap: NaverMap) {
        val cameraUpdate = CameraUpdate.scrollTo(LatLng(lat, long))
        naverMap.moveCamera(cameraUpdate)

        Marker().apply {
            position = LatLng(lat, long)
            map = naverMap
        }
    }
}