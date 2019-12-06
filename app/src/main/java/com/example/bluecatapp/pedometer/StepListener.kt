package com.example.bluecatapp.pedometer


interface StepListener {
    fun step(timeNs: Long)
}