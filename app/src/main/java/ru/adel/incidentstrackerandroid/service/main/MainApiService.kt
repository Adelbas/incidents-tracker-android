package ru.adel.incidentstrackerandroid.service.main

import retrofit2.Response
import retrofit2.http.*
import ru.adel.incidentstrackerandroid.models.IncidentAreaRequest
import ru.adel.incidentstrackerandroid.models.IncidentAreaResponse
import ru.adel.incidentstrackerandroid.models.IncidentGetResponse
import ru.adel.incidentstrackerandroid.models.IncidentPostRequest
import java.time.LocalDate

interface MainApiService {
    @POST("incident")
    suspend fun createIncident(
        @Body request: IncidentPostRequest
    ) : Response<Void>

    @GET("incident/{id}")
    suspend fun getIncidentByIdAndDate(
        @Path("id") id: Long,
        @Query("date") date: LocalDate
    ): Response<IncidentGetResponse>

    @POST("incident/area")
    suspend fun getIncidentsInArea(
        @Body request: IncidentAreaRequest
    ) : Response<List<IncidentAreaResponse>>

    @PUT("settings/notification")
    suspend fun updateNotificationDistance(
        @Query("distance") distance: Int
    ) : Response<Void>

    @POST("auth/logout")
    suspend fun logout(): Response<Void>
}