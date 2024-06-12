package ru.adel.incidentstrackerandroid.models

import java.time.LocalDateTime

data class NotificationMessage(
    val incidentId: Long,
    val title: String,
    val longitude: Double,
    val latitude: Double,
    val timestamp: LocalDateTime
)
