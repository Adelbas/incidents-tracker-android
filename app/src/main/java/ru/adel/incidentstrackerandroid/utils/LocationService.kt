package ru.adel.incidentstrackerandroid.utils

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.internal.notify
import ru.adel.incidentstrackerandroid.R
import ru.adel.incidentstrackerandroid.utils.location.DefaultLocationClient
import ru.adel.incidentstrackerandroid.utils.location.LocationClient
import javax.inject.Inject

@AndroidEntryPoint
class LocationService: Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient

    @Inject
    lateinit var webSocketService: WebSocketService

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    private fun start() {
        webSocketService.start()
        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Поиск происшествий...")
            .setSmallIcon(R.drawable.ic_launcher_background)

//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        locationClient
            .getLocationUpdates(10000L)
            .catch { e ->
                Log.e("LOCATION-ERROR", e.message.toString())
                e.printStackTrace()
            }
            .onEach { location ->
                handleLocationUpdate(location)
            }
            .launchIn(serviceScope)

        startForeground(1, notification.build())
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun handleLocationUpdate(location: Location) {
        val lat = location.latitude.toString()
        val long = location.longitude.toString()
        Log.i("LOCATION","($lat, $long)" )
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}
