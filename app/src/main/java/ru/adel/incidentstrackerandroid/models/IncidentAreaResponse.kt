package ru.adel.incidentstrackerandroid.models

data class IncidentAreaResponse (
    val id: Long,
    val title: String,
    val longitude: Double,
    val latitude: Double,
    val createdAt: String
)