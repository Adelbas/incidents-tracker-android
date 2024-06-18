package ru.adel.incidentstrackerandroid.utils.location

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.adel.incidentstrackerandroid.R
import ru.adel.incidentstrackerandroid.utils.WebSocketService

@AndroidEntryPoint
class LocationService: Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var locationClient: LocationClient

    private var DISTANCE_TO_UPDATE_LOCATION: Int = 100

    private var lastLocation: Location? = null

    private var isEnabled: Boolean = false

    override fun onBind(intent: Intent?): IBinder? {
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
        if (isEnabled) return
        locationClient
            .getLocationUpdates(10000L)
            .catch { e ->
                Log.e("LOCATION-ERROR", e.message.toString())
                e.printStackTrace()
            }
            .onEach{
                location -> processLocation(location)
            }
            .launchIn(serviceScope)

        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Поиск происшествий...")
            .setSmallIcon(R.drawable.ic_launcher_background)
        startForeground(1,notification.build())
        isEnabled = true
    }

    private fun processLocation(location: Location) {
        val lat = location.latitude
        val long = location.longitude
        Log.i("LOCATION", "($lat, $long)")

        if (isNeedToUpdate(location)) {
            lastLocation = location
            broadcastLocation(lat, long)
        }
    }

    private fun isNeedToUpdate(location: Location): Boolean {
        lastLocation?.let {
            val distance = location.distanceTo(it)
            return distance > DISTANCE_TO_UPDATE_LOCATION
        }
        return true
    }

    private fun broadcastLocation(lat: Double, long: Double) {
        val intent = Intent(WebSocketService.LOCATION_UPDATE)
        intent.putExtra("latitude", lat)
        intent.putExtra("longitude", long)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    private fun stop() {
        isEnabled = false
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        isEnabled = false
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}