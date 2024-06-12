package ru.adel.incidentstrackerandroid.models

import java.time.LocalDateTime

data class LocationMessage(
    val longitude: Double,
    val latitude: Double,
    val timestamp: LocalDateTime
)
