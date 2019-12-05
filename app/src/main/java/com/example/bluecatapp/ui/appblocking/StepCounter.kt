/*
 * Copyright (C) 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.bluecatapp.ui.appblocking

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.bluecatapp.AppBlockForegroundService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.OnDataPointListener
import java.util.*
import kotlin.properties.Delegates


/**
 * This sample demonstrates combining the Recording API and History API of the Google Fit platform
 * to record steps, and display the daily current step count. It also demonstrates how to
 * authenticate a user with Google Play Services.
 */
class StepCounter(val mActivity: Activity) {

    private val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 150
    private val TAG = "GOOGLE_FIT"
    private lateinit var fitnessOptions: FitnessOptions
    private var stepCount = 0

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        Toast.makeText(
//            this,
//            "Step Counter Created!",
//            Toast.LENGTH_SHORT
//        ).show()
//    }

    fun accessGoogleFit(){
        fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .build()

        if (checkPermissions()) {
            GoogleSignIn.requestPermissions(
                mActivity,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                GoogleSignIn.getLastSignedInAccount(mActivity),
                fitnessOptions)
        } else {
            subscribe()
            readData()
        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (resultCode == RESULT_OK) {
//            if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
//                subscribe()
//                readData()
//            }
//        }


//    override fun onDestroy() {
//        super.onDestroy()
//        // Unsubscribe from Recording Client
//        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this)!!)
//            .unsubscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
//    }

    /** Records step data by requesting a subscription to background step data. */
    private fun subscribe() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        Fitness.getRecordingClient(mActivity, GoogleSignIn.getLastSignedInAccount(mActivity)!!)
            .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .addOnCompleteListener { task ->
                if (task.isSuccessful()) {
                    Log.i(TAG, "Successfully subscribed!");
                }
            }
            .addOnFailureListener { Log.i(TAG, "There was a problem subscribing.") }
    }

    /**
     * Reads the current daily step total, computed from midnight of the current day on the device's
     * current timezone.
     */
    private fun readData() {
        Fitness.getHistoryClient(mActivity, GoogleSignIn.getLastSignedInAccount(mActivity)!!)
            .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener { dataSet ->
                if (!dataSet.isEmpty) {
                    stepCount = dataSet.getDataPoints()[0].getValue(Field.FIELD_STEPS).asInt()
                    Log.i(TAG, "Total steps: $stepCount")
                    Toast.makeText(
                        mActivity,
                        "Google fit: $stepCount steps",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e -> Log.w(TAG, "There was a problem getting the step count.", e); }
    }

    // Check if fitness options permissions granted
    private fun checkPermissions() : Boolean{
        return !GoogleSignIn.hasPermissions(
            GoogleSignIn.getLastSignedInAccount(mActivity),
            fitnessOptions
        )
    }

    fun getStepCount(): Int{
        return stepCount
    }


//    private fun registerFitnessDataListener(dataSource: DataSource, dataType: DataType) {
//        mListener = OnDataPointListener { dataPoint ->
//            for (field in dataPoint.dataType.fields) {
//                val `val`: Value = dataPoint.getValue(field)
//                Log.i(TAG, "Detected DataPoint field: " + field.name)
//                Log.i(TAG, "Detected DataPoint value: $`val`")
//            }
//        }
//
//        Fitness.getSensorsClient(
//            this,
//            GoogleSignIn.getLastSignedInAccount(this)!!
//        )
//            .add(
//                SensorRequest.Builder()
//                    .setDataSource(dataSource) // Optional but recommended for custom data sets.
//                    .setDataType(dataType) // Can't be omitted.
//                    .setSamplingRate(10, TimeUnit.SECONDS)
//                    .build(),
//                mListener
//            )
//            .addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    Log.i(TAG, "Listener registered!")
//                } else {
//                    Log.e(TAG, "Listener not registered.", task.exception)
//                }
//            }
//    }
}

class Step(step: Int) {

    var step: Int by Delegates.observable(step) { _, old, new ->
        Log.i("GOOGLE_FIT", "New Steps: $old -> $new")
    }
}

