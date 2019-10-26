package com.example.bluecatapp.ui.location

data class CoordToAddrData(
    val status: HttpStatus,
    val results: List<LocationResults>
)

data class HttpStatus (
    val code: Int,
    val name: String,
    val message: String
)

data class LocationResults (
    val name: String,
    val code: LocationCode,
    val region: LocationRegion,
    val land: LocationLand
)

data class LocationCode (
    val id: String,
    val type: String,
    val mappingId: String
)

data class LocationRegion (
    val area0: LocationArea,
    val area1: LocationArea,
    val area2: LocationArea,
    val area3: LocationArea,
    val area4: LocationArea
)

data class LocationArea (
    val name: String,
    val coords: LocationCoords,
    val alias: String
)

data class LocationCoords (
    val center: LocationCenter
)

data class LocationCenter (
    val crs: String,
    val x: Double,
    val y: Double
)

data class LocationLand (
    val type: String,
    val number1: String,
    val number2: String,
    val addition0: LocationAddition,
    val addition1: LocationAddition,
    val addition2: LocationAddition,
    val addition3: LocationAddition,
    val addition4: LocationAddition,
    val name: String,
    val coords: LocationCoords
)

data class LocationAddition (
    val type: String,
    val value: String
)