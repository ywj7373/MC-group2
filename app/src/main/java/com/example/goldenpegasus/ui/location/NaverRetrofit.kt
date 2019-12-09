package com.example.goldenpegasus.ui.location

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NaverRetrofit {
    fun getService(): RetrofitService = retrofit.create(RetrofitService::class.java)

    private val retrofit =
        Retrofit.Builder()
            .baseUrl("https://naveropenapi.apigw.ntruss.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}