package com.example.bluecatapp.ui.appblocking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class AppBlockingViewModel() : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is AppBlocking Fragment"
    }
    val text: LiveData<String> = _text

//    private var appStepCounters: LiveData<MutableMap<String, Int>> ?= null
//    private var adapterData: LiveData<List<List<Any?>>> ?= null
//
//    fun getAppStepCounters(): LiveData<MutableMap<String, Int>>? {
//        return appStepCounters
//    }
//
//    fun getAdapterData(): LiveData<List<List<Any?>>>? {
//        return adapterData
//    }
//
//    fun setAppStepCounters(newData: MutableMap<String, Int>) {
//        appStepCounters = newData as LiveData<MutableMap<String, Int>>
//    }
//
//    fun setAdapterData(newData:List<List<Any?>>) {
//        adapterData = newData as LiveData<List<List<Any?>>>
//    }
}