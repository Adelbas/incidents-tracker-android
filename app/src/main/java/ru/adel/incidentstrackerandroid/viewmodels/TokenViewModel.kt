package ru.adel.incidentstrackerandroid.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ru.adel.incidentstrackerandroid.utils.TokenManager
import javax.inject.Inject

@HiltViewModel
class TokenViewModel @Inject constructor(
    private val tokenManager: TokenManager,
): ViewModel() {

    val accessToken = MutableLiveData<String?>()
    val refreshToken = MutableLiveData<String?>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            tokenManager.getAccessToken().collect {
                withContext(Dispatchers.Main) {
                    accessToken.value = it
                }
            }
            tokenManager.getRefreshToken().collect {
                withContext(Dispatchers.Main) {
                    refreshToken.value = it
                }
            }
        }
    }

    fun saveToken(accessToken: String, refreshToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            tokenManager.saveAccessToken(accessToken)
            tokenManager.saveRefreshToken(refreshToken)
        }
    }

    fun deleteToken() {
        runBlocking {
            tokenManager.deleteAccessToken()
            tokenManager.deleteRefreshToken()
        }
    }
}