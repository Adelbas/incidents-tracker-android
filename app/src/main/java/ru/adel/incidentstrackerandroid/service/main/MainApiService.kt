package ru.adel.incidentstrackerandroid.service.main

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import ru.adel.incidentstrackerandroid.models.IncidentPostRequest

interface MainApiService {
    @POST("incident")
    suspend fun createIncident(
        @Body request: IncidentPostRequest
    ) : Response<Void>
}