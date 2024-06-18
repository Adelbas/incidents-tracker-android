package ru.adel.incidentstrackerandroid.utils

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavDeepLinkBuilder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ru.adel.incidentstrackerandroid.MainActivity
import ru.adel.incidentstrackerandroid.R
import ru.adel.incidentstrackerandroid.models.IncidentUserInteractionMessage
import ru.adel.incidentstrackerandroid.models.InteractionStatus
import ru.adel.incidentstrackerandroid.models.LocationMessage
import ru.adel.incidentstrackerandroid.models.NotificationMessage
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader
import ua.naiksoftware.stomp.dto.StompMessage
import ua.naiksoftware.stomp.provider.OkHttpConnectionProvider
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class WebSocketService: Service() {

    @Inject
    lateinit var tokenManager: TokenManager

    private var mStompClient: StompClient? = null
    private var compositeDisposable: CompositeDisposable? = null
    private lateinit var tokenHeader: StompHeader

    private val gson: Gson = GsonBuilder().registerTypeAdapter(
        LocalDateTime::class.java,
        GsonLocalDateTimeAdapter()
    ).create()

    private var isConnected: Boolean = false

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val latitude = intent?.getDoubleExtra("latitude", 0.0)
            val longitude = intent?.getDoubleExtra("longitude", 0.0)
            handleLocation(latitude!!, longitude!!)
        }
    }

    private val interactionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra("id", 0L)
            val incidentDate = intent?.getStringExtra("incidentDate")
            val interactionStatus = intent?.getStringExtra("status")
            handleUserInteraction(id!!, incidentDate!!, interactionStatus!!)
        }
    }

    override fun onCreate() {
        super.onCreate()
        LocalBroadcastManager.getInstance(this).registerReceiver(locationReceiver, IntentFilter(LOCATION_UPDATE))
        LocalBroadcastManager.getInstance(this).registerReceiver(interactionReceiver, IntentFilter(INCIDENT_INTERACTION))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    private fun start() {
        if (isConnected) return
        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Поиск происшествий...")
            .setSmallIcon(R.drawable.ic_launcher_background)
        startForeground(1, notification.build())

        val accessToken = runBlocking {
            tokenManager.getAccessToken().first().toString()
        }
        tokenHeader = StompHeader("Authorization", "Bearer $accessToken")
        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, SOCKET_URL)
            .withServerHeartbeat(30000)
        resetSubscriptions()
        initChat()
    }

    private fun initChat() {
        resetSubscriptions()

        if (mStompClient != null) {
            //настраиваем подписку на топик
            val topicSubscribe = mStompClient!!.topic(USER_PRIVATE_TOPIC)
                .subscribeOn(Schedulers.io(), false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ topicMessage: StompMessage ->
                    Log.d(OkHttpConnectionProvider.TAG, topicMessage.payload)

                    val message: NotificationMessage =
                        gson.fromJson(topicMessage.payload, NotificationMessage::class.java)

                    Log.i("MESSAGE", message.toString())
                    handleNotificationMessage(message)
                },
                    {
                        Log.e(OkHttpConnectionProvider.TAG, "Error!", it) //обработка ошибок
                    }
                )

            //подписываемся на состояние WebSocket'a
            val lifecycleSubscribe = mStompClient!!.lifecycle()
                .subscribeOn(Schedulers.io(), false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { lifecycleEvent: LifecycleEvent ->
                    when (lifecycleEvent.type!!) {
                        LifecycleEvent.Type.OPENED -> {
                            Log.d(OkHttpConnectionProvider.TAG, "Stomp connection opened")
                            isConnected = true
                        }
                        LifecycleEvent.Type.ERROR -> Log.e(OkHttpConnectionProvider.TAG, "Error", lifecycleEvent.exception)
                        LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT,
                        LifecycleEvent.Type.CLOSED -> {
                            Log.d(OkHttpConnectionProvider.TAG, "Stomp connection closed")
                            isConnected = false
                        }
                    }
                }

            compositeDisposable!!.add(lifecycleSubscribe)
            compositeDisposable!!.add(topicSubscribe)

            //открываем соединение
            if (!mStompClient!!.isConnected) {
                mStompClient!!.connect(listOf(tokenHeader))
            }


        } else {
            Log.e(OkHttpConnectionProvider.TAG, "mStompClient is null!")
        }
    }

    private fun sendLocationMessage(locationMessage: LocationMessage) {
        sendCompletable(mStompClient!!.send(LOCATION_TOPIC, gson.toJson(locationMessage)))
    }

    private fun sendInteractionMessage(interactionMessage: IncidentUserInteractionMessage) {
        sendCompletable(mStompClient!!.send(INTERACTION_TOPIC, gson.toJson(interactionMessage)))
    }

    private fun sendCompletable(request: Completable) {
        compositeDisposable?.add(
            request.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Log.d(OkHttpConnectionProvider.TAG, "Stomp sended")
                    },
                    {
                        Log.e(OkHttpConnectionProvider.TAG, "Stomp error", it)
                    }
                )
        )
    }

    private fun handleNotificationMessage(notificationMessage: NotificationMessage) {
        if (isMainFragmentVisible()) {
            val intent = Intent(INCIDENT_RECEIVED)
            intent.putExtra("notificationMessage", notificationMessage)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        }
        val bundle = Bundle()
        bundle.putLong("incidentId", notificationMessage.incidentId)
        bundle.putString("timestamp", notificationMessage.timestamp.toString())
        val pendingIntent = NavDeepLinkBuilder(this)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.login_nav_graph)
            .setDestination(R.id.incidentFragment)
            .setArguments(bundle)
            .createPendingIntent()

        val notification = NotificationCompat.Builder(this, "incident")
            .setContentTitle("Новое происшествие")
            .setContentText(notificationMessage.title)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2,notification)
    }

    private fun resetSubscriptions() {
        Log.i("RESET", "RESTING SUB")
        if (compositeDisposable != null) {
            compositeDisposable!!.dispose()
        }

        compositeDisposable = CompositeDisposable()
    }

    private fun stop() {
        Log.i("WS", "Stop WS")
        isConnected = false
        mStompClient?.disconnect()
        stopForeground(true)
        stopSelf()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(interactionReceiver)
    }

    override fun onDestroy() {
        isConnected = false
        Log.i("WS", "Destroy WS")
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(interactionReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun handleLocation(latitude: Double, longitude: Double) {
        Log.i("WS", "Handle new location: ($latitude, $longitude)")
        val locationMessage = LocationMessage(longitude,latitude, LocalDateTime.now())
        sendLocationMessage(locationMessage)
    }


    private fun handleUserInteraction(id: Long, incidentDate: String, status: String) {
        Log.i("WS","Handle new interaction: ($id, $incidentDate)")
        val incidentUserInteractionMessage = IncidentUserInteractionMessage(
            id, incidentDate, InteractionStatus.valueOf(status), LocalDateTime.now())
        sendInteractionMessage(incidentUserInteractionMessage)
    }


    private fun isMainFragmentVisible(): Boolean {
        val isVisible = runBlocking {
            tokenManager.isMainFragmentVisible().first()
        }
        return isVisible == true
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val LOCATION_UPDATE = "LOCATION_UPDATE"
        const val INCIDENT_INTERACTION = "INCIDENT_INTERACTION"
        const val INCIDENT_RECEIVED = "INCIDENT_RECEIVED"
        const val SOCKET_URL = "http://10.0.2.2:8080/ws"
        const val LOCATION_TOPIC = "/app/location"
        const val INTERACTION_TOPIC = "/app/interaction"
        const val USER_PRIVATE_TOPIC = "/user/queue/private"
    }
}
