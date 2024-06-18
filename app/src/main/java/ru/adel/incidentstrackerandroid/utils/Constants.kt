package ru.adel.incidentstrackerandroid.utils

import android.content.Context
import android.location.Location
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.FragmentActivity
import com.google.gson.Gson
import com.yandex.mapkit.geometry.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Response
import ru.adel.incidentstrackerandroid.models.ErrorResponse
import ru.adel.incidentstrackerandroid.viewmodels.CoroutinesErrorHandler
import kotlin.math.PI
import kotlin.math.cos

fun<T> apiRequestFlow(call: suspend () -> Response<T>): Flow<ApiResponse<T>> = flow {
    emit(ApiResponse.Loading)

    withTimeoutOrNull(30000L) {
        val response = call()

        try {
            if (response.isSuccessful) {
                if (response.body() != null) {
                    emit(ApiResponse.Success(response.body()!!))
                } else {
                    emit(ApiResponse.Success(null as T))
                }
            } else {
                response.errorBody()?.let { error ->
                    error.close()
                    val parsedError: ErrorResponse = Gson().fromJson(error.charStream(), ErrorResponse::class.java)
                    emit(ApiResponse.Failure(parsedError.message, parsedError.code))
                }
            }
        } catch (e: Exception) {
            emit(ApiResponse.Failure(e.message ?: e.toString(), 400))
        }
    } ?: emit(ApiResponse.Failure("Timeout! Please try again.", 408))
}.flowOn(Dispatchers.IO)

fun hideKeyboard(activity: FragmentActivity, inputView: View) {
    val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    val view: View? = activity.currentFocus
    if (view != null) {
        imm!!.hideSoftInputFromWindow(inputView.windowToken, 0)
    }
}

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0].toDouble()
}

fun offsetPointBottom(point: Point, distanceMeters: Double): Point {
    val distanceKm = distanceMeters / 1000.0

    val newLatitude = point.latitude - (distanceKm / 111.0)

    val newLongitude = point.longitude - (distanceKm / (111.0 * cos(point.latitude * PI / 180.0)))

    return Point(newLatitude, newLongitude)
}

fun offsetPointTop(point: Point, distanceMeters: Double): Point {
    val distanceKm = distanceMeters / 1000.0

    val newLatitude = point.latitude + (distanceKm / 111.0)

    val newLongitude = point.longitude + (distanceKm / (111.0 * cos(point.latitude * PI / 180.0)))

    return Point(newLatitude, newLongitude)
}

val coroutinesErrorHandler = object : CoroutinesErrorHandler {
    override fun onError(message: String) {
        Log.e("Error", "Error! $message")
    }
}