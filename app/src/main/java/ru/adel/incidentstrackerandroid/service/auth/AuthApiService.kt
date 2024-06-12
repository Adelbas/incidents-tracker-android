package ru.adel.incidentstrackerandroid.service.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import ru.adel.incidentstrackerandroid.models.AuthenticationRequest
import ru.adel.incidentstrackerandroid.models.LoginResponse
import ru.adel.incidentstrackerandroid.models.RegistrationRequest

interface AuthApiService {

    @POST("auth/register")
    suspend fun register(
        @Body register: RegistrationRequest,
    ): Response<LoginResponse>

    @POST("auth/login")
    suspend fun login(
        @Body auth: AuthenticationRequest,
    ): Response<LoginResponse>

    @GET("auth/refresh")
    suspend fun refreshToken(
        @Header("Authorization") token: String,
    ): Response<LoginResponse>
}