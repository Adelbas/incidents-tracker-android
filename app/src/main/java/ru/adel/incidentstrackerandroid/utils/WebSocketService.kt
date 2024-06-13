package ru.adel.incidentstrackerandroid.utils

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ru.adel.incidentstrackerandroid.R
import ru.adel.incidentstrackerandroid.models.LocationMessage
import ru.adel.incidentstrackerandroid.models.NotificationMessage
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader
import ua.naiksoftware.stomp.dto.StompMessage
import ua.naiksoftware.stomp.provider.OkHttpConnectionProvider
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

class WebSocketService @Inject constructor(
    private var tokenManager: TokenManager
) : Service(){

    private var mStompClient: StompClient? = null
    private var compositeDisposable: CompositeDisposable? = null
    private lateinit var tokenHeader: StompHeader

    private val gson: Gson = GsonBuilder().registerTypeAdapter(LocalDateTime::class.java,
        GsonLocalDateTimeAdapter()
    ).create()

    private val _notificationMessage = MutableLiveData<NotificationMessage?>()
    val liveNotificationMessage: LiveData<NotificationMessage?> = _notificationMessage


    val isConnected = false

    fun start() {
        val accessToken = runBlocking {
            tokenManager.getAccessToken().first().toString()
        }
        tokenHeader = StompHeader("Authorization", "Bearer $accessToken")
//        val headerMap: Map<String, String> = Collections.singletonMap("Authorization", "Bearer $accessToken")
        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, SOCKET_URL)
            .withServerHeartbeat(30000)
        resetSubscriptions()
        initChat()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null;
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
                    //десериализуем сообщение
                    val message: NotificationMessage =
                        gson.fromJson(topicMessage.payload, NotificationMessage::class.java)

                    Log.i("MESSAGE", message.toString())
                    handleMessage(message) //пишем сообщение в БД и в LiveData
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
                        LifecycleEvent.Type.OPENED -> Log.d(OkHttpConnectionProvider.TAG, "Stomp connection opened")
                        LifecycleEvent.Type.ERROR -> Log.e(OkHttpConnectionProvider.TAG, "Error", lifecycleEvent.exception)
                        LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT,
                        LifecycleEvent.Type.CLOSED -> {
                            Log.d(OkHttpConnectionProvider.TAG, "Stomp connection closed")
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
    /*
    отправляем сообщение в общий чат
    */
    fun sendMessage() {
        val locationMessage = LocationMessage(54.3,43.2, LocalDateTime.now())
        sendCompletable(mStompClient!!.send(LOCATION_TOPIC, gson.toJson(locationMessage)))
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

    private fun handleMessage(message: NotificationMessage) {
        val context = this // Ensure the context is not null
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        _notificationMessage.value = message
        val notification = NotificationCompat.Builder(context, "incident")
            .setContentTitle("Новое происшествие")
            .setContentText(message.title)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .build()


        notificationManager.notify(2, notification)
    }

    private fun resetSubscriptions() {
        Log.i("RESET", "RESTING SUB")
        if (compositeDisposable != null) {
            compositeDisposable!!.dispose()
        }

        compositeDisposable = CompositeDisposable()
    }

    companion object{
        const val SOCKET_URL = "http://10.0.2.2:8080/ws"
        const val LOCATION_TOPIC = "/app/location"
        const val USER_PRIVATE_TOPIC = "/user/queue/private"
    }
}