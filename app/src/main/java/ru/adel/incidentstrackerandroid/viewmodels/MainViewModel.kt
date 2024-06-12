package ru.adel.incidentstrackerandroid.viewmodels

import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.adel.incidentstrackerandroid.models.IncidentPostRequest
import ru.adel.incidentstrackerandroid.repository.MainRepository
import ru.adel.incidentstrackerandroid.utils.ApiResponse
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainRepository: MainRepository,
): BaseViewModel() {

    private val _createIncidentResponse = MutableLiveData<ApiResponse<Void>>()
    val createIncidentResponse = _createIncidentResponse

//    private val _userInfoResponse = MutableLiveData<ApiResponse<UserInfoResponse>>()
//    val userInfoResponse = _userInfoResponse
//
//    fun getUserInfo(coroutinesErrorHandler: CoroutinesErrorHandler) = baseRequest(
//        _userInfoResponse,
//        coroutinesErrorHandler,
//    ) {
//        mainRepository.getUserInfo()
//    }
    fun createIncident(request: IncidentPostRequest, coroutinesErrorHandler: CoroutinesErrorHandler) = baseRequest(
        _createIncidentResponse,
        coroutinesErrorHandler
    ) {
        mainRepository.createIncident(request)
    }
}