package ru.adel.incidentstrackerandroid.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.adel.incidentstrackerandroid.models.IncidentAreaRequest
import ru.adel.incidentstrackerandroid.models.IncidentAreaResponse
import ru.adel.incidentstrackerandroid.models.IncidentGetResponse
import ru.adel.incidentstrackerandroid.models.IncidentPostRequest
import ru.adel.incidentstrackerandroid.repository.MainRepository
import ru.adel.incidentstrackerandroid.utils.ApiResponse
import ru.adel.incidentstrackerandroid.utils.TokenManager
import ru.adel.incidentstrackerandroid.utils.coroutinesErrorHandler
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainRepository: MainRepository,
    private val tokenManager: TokenManager
): BaseViewModel() {

    private val _createIncidentResponse = MutableLiveData<ApiResponse<Void>>()
    val createIncidentResponse = _createIncidentResponse

    private val _getIncidentResponse = MutableLiveData<ApiResponse<IncidentGetResponse>>()
    val getIncidentResponse = _getIncidentResponse

    private val _getIncidentsInAreaResponse = MutableLiveData<ApiResponse<List<IncidentAreaResponse>>>()
    val getIncidentsInAreaResponse = _getIncidentsInAreaResponse

    private val _logoutResponse = MutableLiveData<ApiResponse<Void>>()
    val logoutResponse = _logoutResponse

    fun createIncident(request: IncidentPostRequest, coroutinesErrorHandler: CoroutinesErrorHandler) = baseRequest(
        _createIncidentResponse,
        coroutinesErrorHandler
    ) {
        mainRepository.createIncident(request)
    }

    fun getIncident(id: Long, date: LocalDate, coroutinesErrorHandler: CoroutinesErrorHandler) = baseRequest(
        _getIncidentResponse,
        coroutinesErrorHandler
    ) {
        mainRepository.getIncident(id, date)
    }

    fun getIncidentsInArea(request: IncidentAreaRequest, coroutinesErrorHandler: CoroutinesErrorHandler) = baseRequest(
        _getIncidentsInAreaResponse,
        coroutinesErrorHandler
    ) {
        mainRepository.getIncidentsInArea(request)
    }

    fun saveNotificationDistance(distance: Int, coroutinesErrorHandler: CoroutinesErrorHandler) = baseRequest(
        MutableLiveData<ApiResponse<Void>>(),
        coroutinesErrorHandler
    ) {
        runBlocking {
            tokenManager.saveNotificationDistance(distance)
        }
        mainRepository.updateNotificationDistance(distance)
    }

    fun saveMainFragmentVisible(isVisible: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            tokenManager.saveMainFragmentVisible(isVisible)
        }
    }

    fun getNotificationDistance(): Int {
        val distance = runBlocking {
            tokenManager.getNotificationDistance().first()
        }
        return distance?:500
    }

    fun saveRadiusVisible(isVisible: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            tokenManager.saveRadiusVisible(isVisible)
        }
    }

    fun getRadiusVisible(): Boolean {
        val isVisible = runBlocking {
            tokenManager.isRadiusVisible().first()
        }
        return isVisible == true
    }

    fun logout() = baseRequest(
        _logoutResponse,
        coroutinesErrorHandler
    ) {
        mainRepository.logout()
    }
}