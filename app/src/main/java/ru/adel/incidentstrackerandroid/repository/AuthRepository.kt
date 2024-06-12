package ru.adel.incidentstrackerandroid.repository

import ru.adel.incidentstrackerandroid.models.AuthenticationRequest
import ru.adel.incidentstrackerandroid.models.RegistrationRequest
import ru.adel.incidentstrackerandroid.service.auth.AuthApiService
import ru.adel.incidentstrackerandroid.utils.apiRequestFlow
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
) {
    fun login(authenticationRequest: AuthenticationRequest) = apiRequestFlow {
        authApiService.login(authenticationRequest)
    }

    fun register(registrationRequest: RegistrationRequest) = apiRequestFlow {
        authApiService.register(registrationRequest)
    }
}