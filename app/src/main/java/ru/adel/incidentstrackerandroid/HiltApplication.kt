package ru.adel.incidentstrackerandroid

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HiltApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)

        initNotifications()
    }

    private fun initNotifications() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelLocationShare = NotificationChannel(
            "location",
            "Location",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val channelIncidentNotification = NotificationChannel(
            "incident",
            "Incident",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannels(listOf(channelLocationShare,channelIncidentNotification))
    }
}