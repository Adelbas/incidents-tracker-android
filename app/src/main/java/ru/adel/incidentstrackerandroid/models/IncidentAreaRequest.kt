package ru.adel.incidentstrackerandroid.models

data class IncidentAreaRequest (
    val longitudeMin: Double,
    val latitudeMin: Double,
    val longitudeMax: Double,
    val latitudeMax: Double,
    val startDate: String,
    val endDate: String
)