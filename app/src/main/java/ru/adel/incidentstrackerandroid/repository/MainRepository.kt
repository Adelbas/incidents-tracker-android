package ru.adel.incidentstrackerandroid.repository

import android.util.Log
import ru.adel.incidentstrackerandroid.models.IncidentAreaRequest
import ru.adel.incidentstrackerandroid.models.IncidentPostRequest
import ru.adel.incidentstrackerandroid.service.main.MainApiService
import ru.adel.incidentstrackerandroid.utils.apiRequestFlow
import java.time.LocalDate
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val mainApiService: MainApiService,
) {
    fun createIncident(request: IncidentPostRequest) = apiRequestFlow {
        Log.d(Log.INFO.toString(), "Sending create incident request $request")
        mainApiService.createIncident(request)
    }

    fun getIncident(id: Long, date: LocalDate) = apiRequestFlow {
        Log.d(Log.INFO.toString(), "Sending get incident request <$id, $date>")
        mainApiService.getIncidentByIdAndDate(id, date)
    }

    fun getIncidentsInArea(request: IncidentAreaRequest) = apiRequestFlow {
        Log.d(Log.INFO.toString(), "Sending get incidents in area request $request")
        mainApiService.getIncidentsInArea(request)
    }

    fun updateNotificationDistance(distance: Int) = apiRequestFlow {
        Log.d(Log.INFO.toString(), "Sending update notification distance to <$distance> request")
        mainApiService.updateNotificationDistance(distance)
    }

    fun logout() = apiRequestFlow {
        Log.d(Log.INFO.toString(), "Sending logout request")
        mainApiService.logout()
    }
}