package ru.adel.incidentstrackerandroid.models

data class IncidentPostRequest(
    val title: String,
    val description: String?,
    val longitude: Double,
    val latitude: Double,
    val image: ByteArray
)
