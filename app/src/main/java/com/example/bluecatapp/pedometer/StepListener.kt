package com.example.bluecatapp.pedometer

import android.content.Context

interface StepListener {
    fun step(timeNs: Long)
}