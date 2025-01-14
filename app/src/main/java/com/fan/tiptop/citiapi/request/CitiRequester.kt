package com.fan.tiptop.citiapi

import com.fan.tiptop.citiapi.data.CitiStationStatus
import com.fan.tiptop.citiapi.data.CitibikeStationInformationModel
import com.fan.tiptop.citiapi.data.CitibikeStationInformationModelDecorated
import com.fan.tiptop.citiapi.data.CitibikeStationModel
import com.fan.tiptop.citiapi.location.LocationUtils
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class Stations(val stations: List<CitibikeStationModel>)

@Serializable
data class StationStatusModel(val data: Stations, val last_updated: Int, val ttl: Int)

@Serializable
data class StationInformations(val stations: List<CitibikeStationInformationModel>)

@Serializable
data class StationInformationModel(
    val data: StationInformations,
    val last_updated: Int,
    val ttl: Int
)

class CitiRequester {
    private var _stationIdToStation: MutableMap<Int, CitibikeStationModel> = mutableMapOf()
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun getAvailabilitiesWithLocation(
        response: String,
        stationList: List<CitibikeStationInformationModelDecorated>,
        userLocation:Location?
    ): List<CitiStationStatus> {
        var result = mutableListOf<CitiStationStatus>()
        if (stationList.isEmpty()) {
            return result
        }
        if (this._stationIdToStation.isEmpty()) {
            val model = getStationStatusModel(response)
            for (stationInfo in model.data.stations) {
                this._stationIdToStation.put(stationInfo.station_id.toInt(), stationInfo)
            }
        }
        for (stationInfoModelDecorated in stationList) {
            val stationInfoModel=  stationInfoModelDecorated.model
            val stationId = stationInfoModel.station_id.toInt()
            val stationStatus = this._stationIdToStation[stationId]
                ?: throw Exception("Unable to find station for $stationId")
            var distanceDescription = ""
            if (userLocation != null ) {
                distanceDescription = LocationUtils.computeAndFormatDistance(
                    userLocation.latitude,
                    userLocation.longitude,
                    stationInfoModel.lat,
                    stationInfoModel.lon
                )
            }
            result.add(
                CitiStationStatus(
                    stationStatus.num_bikes_available.toString(),
                    stationStatus.num_ebikes_available.toString(),
                    stationStatus.num_docks_available.toString(),
                    stationInfoModel.name,
                    stationInfoModel.station_id.toInt(),
                    distanceDescription,
                    isClosestStation = stationInfoModelDecorated.isClosest,
                    isFavorite = stationInfoModelDecorated.isFavorite
                )
            )
        }
        return result
    }

    private fun getStationStatusModel(string: String): StationStatusModel {
        return json.decodeFromString(string)
    }

    fun getStationInformationModel(stationInformationContent: String): StationInformationModel {
        return json.decodeFromString(stationInformationContent)
    }
}
