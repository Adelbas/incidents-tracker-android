package ru.adel.incidentstrackerandroid.utils
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.adel.incidentstrackerandroid.dataStore

class TokenManager(private val context: Context) {
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    }

    fun getAccessToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }
    }

    fun getRefreshToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[REFRESH_TOKEN_KEY]
        }
    }

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = token
        }
    }

    suspend fun saveRefreshToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[REFRESH_TOKEN_KEY] = token
        }
    }

    suspend fun deleteAccessToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
        }
    }

    suspend fun deleteRefreshToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(REFRESH_TOKEN_KEY)
        }
    }
}