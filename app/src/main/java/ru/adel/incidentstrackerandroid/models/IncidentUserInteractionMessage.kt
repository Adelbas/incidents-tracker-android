package ru.adel.incidentstrackerandroid.models

import java.time.LocalDateTime

data class IncidentUserInteractionMessage(
    val incidentId: Long,
    val incidentDate: String,
    val status: InteractionStatus,
    val timestamp: LocalDateTime
)