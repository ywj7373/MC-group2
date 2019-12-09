package com.example.goldenpegasus.pedometer

interface StepListener {
    fun step(timeNs: Long)
}