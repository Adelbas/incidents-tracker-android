package ru.adel.incidentstrackerandroid.models

data class IncidentAreaResponse (
    val id: Long,
    val title: String,
    val categoryCode: String? = null,
    val categoryName: String? = null,
    val dangerLevel: String? = null,
    val longitude: Double,
    val latitude: Double,
    val createdAt: String
)
