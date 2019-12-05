package com.example.bluecatapp

import android.content.Context


interface StepListener {
    fun step(timeNs: Long)
}