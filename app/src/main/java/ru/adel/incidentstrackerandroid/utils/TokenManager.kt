package ru.adel.incidentstrackerandroid.utils
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.adel.incidentstrackerandroid.dataStore

class TokenManager(private val context: Context) {
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val MAIN_FRAGMENT_VISIBLE_KEY = booleanPreferencesKey("main_fragment")
        private val NOTIFICATION_DISTANCE = intPreferencesKey("notification_distance")
        private val IS_RADIUS_VISIBLE = booleanPreferencesKey("radius_visible")
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

    fun isMainFragmentVisible(): Flow<Boolean?> {
        return context.dataStore.data.map { preferences ->
            preferences[MAIN_FRAGMENT_VISIBLE_KEY]
        }
    }

    suspend fun saveMainFragmentVisible(isVisible: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MAIN_FRAGMENT_VISIBLE_KEY] = isVisible
        }
    }

    fun getNotificationDistance(): Flow<Int?> {
        return context.dataStore.data.map { preferences ->
            preferences[NOTIFICATION_DISTANCE]
        }
    }

    suspend fun saveNotificationDistance(distance: Int) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_DISTANCE] = distance
        }
    }

    fun isRadiusVisible(): Flow<Boolean?> {
        return context.dataStore.data.map { preferences ->
            preferences[IS_RADIUS_VISIBLE]
        }
    }

    suspend fun saveRadiusVisible(isVisible: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_RADIUS_VISIBLE] = isVisible
        }
    }
}