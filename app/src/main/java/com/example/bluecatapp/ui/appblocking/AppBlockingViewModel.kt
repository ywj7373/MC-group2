package com.example.bluecatapp.ui.appblocking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AppBlockingViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is AppBlocking Fragment"
    }
    val text: LiveData<String> = _text
}