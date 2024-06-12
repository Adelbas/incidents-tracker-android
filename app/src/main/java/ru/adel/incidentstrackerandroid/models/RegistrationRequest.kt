package ru.adel.incidentstrackerandroid.models

data class RegistrationRequest(
    val firstname: String,
    val lastname: String,
    val email: String,
    val password: String
)