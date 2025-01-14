package com.fan.tiptop.citiapi.data

// this dataclass contains everything necessary to draw a tile
data class CitiStationStatus(
    // retrieved from citi status api request
    val numBikeAvailable: String,
    val numEbikeAvailable: String,
    val numDockAvailable: String,
    // retrieved from citi information api request
    val address: String,
    val stationId: Int,
    //retrieved from gps data
    var distance:String?=null,
    var isClosestStation: Boolean=false,
    // retrieved from database
    var isFavorite: Boolean=false,
    //changed by user
    var selected: Boolean=false
)
