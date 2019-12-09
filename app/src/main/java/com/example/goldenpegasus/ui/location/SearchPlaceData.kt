package com.example.goldenpegasus.ui.location

//Parse JSON to Java Objects
data class SearchPlaceData (
    val status: String,
    val meta: SearchPlaceMeta,
    val places: List<SearchPlacePlaces>,
    val errorMessage: String
)

data class SearchPlaceMeta (
    val totalCount: Int,
    val count: Int
)

data class SearchPlacePlaces (
    val name: String,
    val road_address: String,
    val jibun_address: String,
    val phone_number: String,
    val x: String,
    val y: String,
    val distance: Double,
    val sessionId: String
)
