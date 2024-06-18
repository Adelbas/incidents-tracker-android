package ru.adel.incidentstrackerandroid.models

import java.util.UUID

data class IncidentGetResponse(
    val id: Long,
    val title: String,
    val longitude: Double,
    val latitude: Double,
    val postedUserId: UUID,
    val postedUserFirstName: String,
    val postedUserLastName: String,
    val image: String,
    val views: Int,
    val createdAt: String
)
