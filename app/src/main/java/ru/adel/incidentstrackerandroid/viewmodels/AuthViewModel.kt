package ru.adel.incidentstrackerandroid.viewmodels

import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.adel.incidentstrackerandroid.models.AuthenticationRequest
import ru.adel.incidentstrackerandroid.models.LoginResponse
import ru.adel.incidentstrackerandroid.models.RegistrationRequest
import ru.adel.incidentstrackerandroid.repository.AuthRepository
import ru.adel.incidentstrackerandroid.utils.ApiResponse
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
): BaseViewModel() {

    private val _loginResponse = MutableLiveData<ApiResponse<LoginResponse>>()
    val loginResponse = _loginResponse

    fun login(authenticationRequest: AuthenticationRequest, coroutinesErrorHandler: CoroutinesErrorHandler) = baseRequest(
        _loginResponse,
        coroutinesErrorHandler
    ) {
        authRepository.login(authenticationRequest)
    }

    fun register(registrationRequest: RegistrationRequest, coroutinesErrorHandler: CoroutinesErrorHandler) = baseRequest(
        _loginResponse,
        coroutinesErrorHandler
    ) {
        authRepository.register(registrationRequest)
    }
}