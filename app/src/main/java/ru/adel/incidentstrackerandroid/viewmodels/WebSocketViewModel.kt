//package ru.adel.incidentstrackerandroid.viewmodels
//
//import android.util.Log
//import android.view.View
//import androidx.fragment.app.activityViewModels
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.google.gson.Gson
//import com.google.gson.GsonBuilder
//import dagger.hilt.android.lifecycle.HiltViewModel
//import io.reactivex.Completable
//import io.reactivex.android.schedulers.AndroidSchedulers
//import io.reactivex.disposables.CompositeDisposable
//import io.reactivex.schedulers.Schedulers
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.runBlocking
//import kotlinx.coroutines.withContext
//import ru.adel.incidentstrackerandroid.R
//import ru.adel.incidentstrackerandroid.models.LocationMessage
//import ru.adel.incidentstrackerandroid.models.NotificationMessage
//import ru.adel.incidentstrackerandroid.utils.TokenManager
//import ua.naiksoftware.stomp.Stomp
//import ua.naiksoftware.stomp.StompClient
//import ua.naiksoftware.stomp.dto.LifecycleEvent
//import ua.naiksoftware.stomp.dto.StompMessage
//import ua.naiksoftware.stomp.provider.OkHttpConnectionProvider.TAG
//import java.time.LocalDateTime
//import java.util.Collections
//import javax.inject.Inject
//
//@HiltViewModel
//class WebSocketViewModel @Inject constructor(
//    private val tokenManager: TokenManager
//): ViewModel() {
//    companion object{
//        const val SOCKET_URL = "http://10.0.2.2:8080/ws"
//        const val LOCATION_TOPIC = "/app/location"
//        const val USER_PRIVATE_TOPIC = "/user/queue/private"
//    }
//
//    private val gson: Gson = Gson()
//    private var mStompClient: StompClient? = null
//    private var compositeDisposable: CompositeDisposable = CompositeDisposable()
//
//    private val _notificationMessage = MutableLiveData<NotificationMessage?>()
//    val liveNotificationMessage: LiveData<NotificationMessage?> = _notificationMessage
//    val isConnected = false
//
//    init {
//        val accessToken = runBlocking {
//            tokenManager.getAccessToken().first()
//        }
//        val headerMap: Map<String, String> = Collections.singletonMap("Authorization", "Bearer $accessToken")
//        Log.i("JWT",headerMap.toString())
//        //инициализация WebSocket клиента
//        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, SOCKET_URL, headerMap)
//            .withServerHeartbeat(30000)
//        resetSubscriptions()
//        initChat() //инициализация подписок
//    }
//
//    private fun initChat() {
//        resetSubscriptions()
//
//        if (mStompClient != null) {
//            //настраиваем подписку на топик
//            val topicSubscribe = mStompClient!!.topic(USER_PRIVATE_TOPIC)
//                .subscribeOn(Schedulers.io(), false)
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe({ topicMessage: StompMessage ->
//                    Log.d(TAG, topicMessage.payload)
//                    //десериализуем сообщение
//                    val message: NotificationMessage =
//                        gson.fromJson(topicMessage.payload, NotificationMessage::class.java)
//
//                    addMessage(message) //пишем сообщение в БД и в LiveData
//                },
//                    {
//                        Log.e(TAG, "Error!", it) //обработка ошибок
//                    }
//                )
//
//            //подписываемся на состояние WebSocket'a
//            val lifecycleSubscribe = mStompClient!!.lifecycle()
//                .subscribeOn(Schedulers.io(), false)
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe { lifecycleEvent: LifecycleEvent ->
//                    when (lifecycleEvent.type!!) {
//                        LifecycleEvent.Type.OPENED -> Log.d(TAG, "Stomp connection opened")
//                        LifecycleEvent.Type.ERROR -> Log.e(TAG, "Error", lifecycleEvent.exception)
//                        LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT,
//                        LifecycleEvent.Type.CLOSED -> {
//                            Log.d(TAG, "Stomp connection closed")
//                        }
//                    }
//                }
//
//            compositeDisposable!!.add(lifecycleSubscribe)
//            compositeDisposable!!.add(topicSubscribe)
//
//            //открываем соединение
//            if (!mStompClient!!.isConnected) {
//                mStompClient!!.connect()
//            }
//
//
//        } else {
//            Log.e(TAG, "mStompClient is null!")
//        }
//    }
//    /*
//    отправляем сообщение в общий чат
//    */
//    fun sendMessage() {
//        val locationMessage = LocationMessage(54.3,43.2, LocalDateTime.now())
//        sendCompletable(mStompClient!!.send(LOCATION_TOPIC, gson.toJson(locationMessage)))
//    }
//
//    private fun sendCompletable(request: Completable) {
//        compositeDisposable?.add(
//            request.subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(
//                    {
//                        Log.d(TAG, "Stomp sended")
//                    },
//                    {
//                        Log.e(TAG, "Stomp error", it)
//                    }
//                )
//        )
//    }
//
//    private fun addMessage(message: NotificationMessage) {
//        _notificationMessage.value = message
//    }
//
//    private fun resetSubscriptions() {
//        if (compositeDisposable != null) {
//            compositeDisposable!!.dispose()
//        }
//
//        compositeDisposable = CompositeDisposable()
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//
//        mStompClient?.disconnect()
//        compositeDisposable?.dispose()
//    }
//}