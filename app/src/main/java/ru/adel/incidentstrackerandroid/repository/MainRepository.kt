package ru.adel.incidentstrackerandroid.repository

import android.util.Log
import ru.adel.incidentstrackerandroid.models.IncidentPostRequest
import ru.adel.incidentstrackerandroid.service.main.MainApiService
import ru.adel.incidentstrackerandroid.utils.apiRequestFlow
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val mainApiService: MainApiService,
) {
    fun createIncident(request: IncidentPostRequest) = apiRequestFlow {
        Log.d(Log.INFO.toString(), "Sending create incident request $request")
        mainApiService.createIncident(request)
    }
}