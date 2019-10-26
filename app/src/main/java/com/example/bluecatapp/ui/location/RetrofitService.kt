package com.example.bluecatapp.ui.location

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface RetrofitService {

    @Headers(
        "X-NCP-APIGW-API-KEY-ID: vdzq4ajyej",
        "X-NCP-APIGW-API-KEY: A6CoEzf661hYYJ7qBkvILYG7spFDQOR18SBgIh1N"
    )
    @GET("/map-reversegeocode/v2/gc")
    fun requestReverseGeocode(
        @Query("coords") coords: String,
        @Query("orders") orders: String = "roadaddr",
        @Query("output") output: String = "json"
    ): Call<CoordToAddrData>

    @Headers(
        "X-NCP-APIGW-API-KEY-ID: vdzq4ajyej",
        "X-NCP-APIGW-API-KEY: A6CoEzf661hYYJ7qBkvILYG7spFDQOR18SBgIh1N"
    )
    @GET("/map-place/v1/search")
    fun requestSearchPlace(
        @Query("query") query: String,
        @Query("coordinate") coordinate: String
    ): Call<SearchPlaceData>
}