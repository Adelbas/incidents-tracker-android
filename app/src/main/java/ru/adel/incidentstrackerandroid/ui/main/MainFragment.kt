package ru.adel.incidentstrackerandroid.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.*
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import dagger.hilt.android.AndroidEntryPoint
import ru.adel.incidentstrackerandroid.R
import ru.adel.incidentstrackerandroid.models.IncidentAreaRequest
import ru.adel.incidentstrackerandroid.models.IncidentAreaResponse
import ru.adel.incidentstrackerandroid.models.NotificationMessage
import ru.adel.incidentstrackerandroid.utils.*
import ru.adel.incidentstrackerandroid.viewmodels.MainViewModel
import java.time.LocalDate
import kotlin.math.max


@AndroidEntryPoint
class MainFragment: Fragment() {

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var mapView: MapView

    private lateinit var mapKit: MapKit

    private lateinit var userLocationLayer: UserLocationLayer

    private lateinit var mapObjectCollection: ClusterizedPlacemarkCollection

    private lateinit var btnLocation: FloatingActionButton

    private var circleMapObject: CircleMapObject? = null

    private var routeStartLocation: Point? = null

    private var lastBottomLeft: Point? = null

    private var lastTopRight: Point? = null

    private val defaultOffsetMeters: Double = 5000.0

    private var followUserLocation = false

    private var zoomValue = 16f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.initialize(requireContext())
        mapKit = MapKitFactory.getInstance()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(incidentReceiver, IntentFilter(WebSocketService.INCIDENT_RECEIVED))
        Log.i("MAIN","onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i("MAIN","onCreateView")
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.i("MAIN","onViewCreated")
        mainViewModel.saveMainFragmentVisible(true)
        mapView = view.findViewById(R.id.mapview)
        btnLocation = view.findViewById(R.id.user_location_fab)

        userLocationLayer = mapKit.createUserLocationLayer(mapView.mapWindow)
        userLocationLayer.isVisible = true
        userLocationLayer.isAutoZoomEnabled = true
        userLocationLayer.setObjectListener(userLocationObjectListener)
        mapView.map.addCameraListener(cameraListener)

        mapObjectCollection = mapView.map.mapObjects.addClusterizedPlacemarkCollection(clusterListener)
        mapObjectCollection.addTapListener(mapObjectTapListener)

        cameraUserPosition()
        userInterface()
        observeIncidents()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
        Log.i("MAIN","onStart")
    }

    override fun onStop() {
        if (this::mapView.isInitialized) {
            mapView.onStop()
        }
        Log.i("MAIN","onStop")
        MapKitFactory.getInstance().onStop()
        mainViewModel.saveMainFragmentVisible(false)
        super.onStop()
    }

    override fun onDestroy() {
        if (this::mapView.isInitialized) {
            mapView.onStop()
        }
        Log.i("MAIN","onDestroy")
        MapKitFactory.getInstance().onStop()
        mainViewModel.saveMainFragmentVisible(false)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(incidentReceiver)
        super.onDestroy()
    }

    private val incidentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getParcelableExtra<NotificationMessage>("notificationMessage")?.let { message ->
                val incident = IncidentAreaResponse(
                    message.incidentId,
                    message.title,
                    message.longitude,
                    message.latitude,
                    message.timestamp.toString()
                )
                Log.i("Incident from notification",incident.toString())
                addIncidentOnMap(incident)
            }
        }
    }

    private val userLocationObjectListener = object: UserLocationObjectListener {
        override fun onObjectAdded(userLocationView: UserLocationView) {
            setAnchor()

            userLocationView.pin.setIcon(ImageProvider.fromResource(requireContext(), R.drawable.ic_location_icon))
            userLocationView.arrow.setIcon(ImageProvider.fromResource(requireContext(), R.drawable.ic_location_icon))
            userLocationView.accuracyCircle.fillColor = Color.TRANSPARENT
        }
        override fun onObjectRemoved(userLocationView: UserLocationView) {}
        override fun onObjectUpdated(userLocationView: UserLocationView, event: ObjectEvent) {}
    }

    private val cameraListener = CameraListener { p0, cPos, cUpd, finish ->
        if (cUpd == CameraUpdateReason.GESTURES) {
            followUserLocation = false
            noAnchor()
        }
        if (finish) {
            if (followUserLocation) {
                setAnchor()
                updateNotificationDistanceCircle(cPos)
            }

            val visibleRegion = mapView.mapWindow.focusRegion
            val bottomLeft = visibleRegion.bottomLeft
            val topRight = visibleRegion.topRight
            if (isNeedRequest(bottomLeft, topRight)) {
                requestIncidentsInArea(bottomLeft, topRight)
            }

            if (mainViewModel.getRadiusVisible() && circleMapObject == null) {
                createNotificationDistanceCircle(cPos)
            }
        }
    }

    private fun createNotificationDistanceCircle(cameraPosition: CameraPosition) {
        val center = cameraPosition.target
        val circle = Circle(center, mainViewModel.getNotificationDistance().toFloat())
        circleMapObject = mapView.map.mapObjects.addCircle(circle).apply {
            strokeWidth = 2f
            strokeColor = Color.BLUE
            fillColor = Color.argb(66, 0, 0, 255)
        }
    }

    private fun updateNotificationDistanceCircle(cameraPosition: CameraPosition) {
        if (mainViewModel.getRadiusVisible() && circleMapObject != null) {
            val center = cameraPosition.target
            val circle = Circle(center, mainViewModel.getNotificationDistance().toFloat())
            circleMapObject!!.geometry = circle
        }
    }

    private fun disableNotificationDistanceCircle() {
        if (mainViewModel.getRadiusVisible() && circleMapObject != null && circleMapObject!!.isVisible) {
            circleMapObject!!.isVisible = false
        }
    }

    private fun isNeedRequest(bottomLeft: Point, topRight: Point): Boolean {
        if (lastBottomLeft == null && lastTopRight == null) {
            return true
        }
        return bottomLeft.latitude < lastBottomLeft!!.latitude ||
                bottomLeft.longitude < lastBottomLeft!!.longitude ||
                topRight.latitude > lastTopRight!!.latitude ||
                topRight.longitude > lastTopRight!!.longitude
    }

    private fun requestIncidentsInArea(bottomLeft: Point, topRight: Point) {
        val distance = calculateDistance(bottomLeft.latitude, bottomLeft.longitude, topRight.latitude, topRight.longitude)
        val offset = max(defaultOffsetMeters, distance / 2)

        val offsetBottomLeft = offsetPointBottom(bottomLeft, offset)
        val offsetTopRight = offsetPointTop(topRight, offset)

        lastBottomLeft = offsetBottomLeft
        lastTopRight = offsetTopRight

        mainViewModel.getIncidentsInArea(
            IncidentAreaRequest(
                offsetBottomLeft.longitude,
                offsetBottomLeft.latitude,
                offsetTopRight.longitude,
                offsetTopRight.latitude,
                LocalDate.now().minusDays(1).toString(),
                LocalDate.now().toString()
            ),
            coroutinesErrorHandler
        )
    }

    private fun observeIncidents() {
        mainViewModel.getIncidentsInAreaResponse.observe(viewLifecycleOwner) {
            when (it) {
                is ApiResponse.Failure -> {
                    Log.e("Error", "Error! ${it.errorMessage}")
                    Toast.makeText(requireContext(), "Ошибка поиска", Toast.LENGTH_SHORT).show()
                }

                ApiResponse.Loading -> {
                    Toast.makeText(requireContext(), "Поиск происшествий...", Toast.LENGTH_SHORT).show()
                }

                is ApiResponse.Success -> {
                    Toast.makeText(requireContext(), "Поиск завершен", Toast.LENGTH_SHORT).show()
                    handleIncidentsResponse(it.data)
                }
            }
        }
    }

    private fun handleIncidentsResponse(incidents: List<IncidentAreaResponse>) {
        mapObjectCollection.clear()
        incidents.forEach { incident ->
            addIncidentOnMap(incident)
        }
    }

    private fun addIncidentOnMap(incident: IncidentAreaResponse) {
        val point = Point(incident.latitude, incident.longitude)
        mapObjectCollection.addPlacemark(point).apply {
            geometry = point
            userData = incident
            setIcon(ImageProvider.fromResource(requireContext(), R.drawable.ic_incident))
        }
        mapObjectCollection.clusterPlacemarks(60.0, 13)
    }

    private val mapObjectTapListener = MapObjectTapListener { mapObject, point ->
        val placemark = mapObject as? PlacemarkMapObject
        val incident = placemark?.userData as? IncidentAreaResponse
        if (incident != null) {
            val bundle = Bundle()
            bundle.putLong("incidentId", incident.id)
            bundle.putString("timestamp", incident.createdAt)
            findNavController().navigate(
                R.id.action_main_to_incidentFragment,
                bundle,
                NavOptions.Builder()
                    .setRestoreState(true)
                    .setLaunchSingleTop(true)
                    .build()
            )
        }
        true
    }

    private val clusterListener = ClusterListener { cluster ->
        cluster.appearance.setIcon(ImageProvider.fromResource(requireContext(), R.drawable.ic_incident))
        cluster.addClusterTapListener {
            moveCameraToPoint(it.appearance.geometry)
            true
        }
    }

    private fun userInterface() {
        btnLocation.setOnClickListener {
            cameraUserPosition()
            followUserLocation = true
        }
    }

    private fun cameraUserPosition() {
        if (userLocationLayer.cameraPosition() != null) {
            routeStartLocation = userLocationLayer.cameraPosition()!!.target
            moveCameraToPoint(routeStartLocation!!)
        }
    }

    private fun setAnchor() {
        userLocationLayer.setAnchor(
            PointF(
                (mapView.width * 0.5).toFloat(), (mapView.height * 0.5).toFloat()
            ),
            PointF(
                (mapView.width * 0.5).toFloat(), (mapView.height * 0.83).toFloat()
            )
        )

        btnLocation.setImageResource(R.drawable.ic_my_location_black_24dp)
    }

    private fun noAnchor() {
        userLocationLayer.resetAnchor()

        btnLocation.setImageResource(R.drawable.ic_location_searching_black_24dp)
    }

    private fun moveCameraToPoint(point: Point) {
        mapView.map.move(
            CameraPosition(point, zoomValue, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 2f),
            null)
    }

    fun onAddButtonClicked(listener: OnAddButtonClickListener) {
        val pin = requireView().findViewById<ImageButton>(R.id.pin)
        val navigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val btnLayout = requireActivity().findViewById<LinearLayout>(R.id.btn_layout)

        pin.isGone = false
        btnLayout.isGone = false

        val fragment = requireActivity().findViewById<FragmentContainerView>(R.id.nav_host_fragment)
        val layoutParams = fragment.layoutParams as RelativeLayout.LayoutParams
        layoutParams.addRule(RelativeLayout.ABOVE, btnLayout.id)
        fragment.layoutParams = layoutParams

        navigationView.isVisible = false

        val btnCancel = requireActivity().findViewById<Button>(R.id.btn_cancel)
        val btnOk = requireActivity().findViewById<Button>(R.id.btn_ok)

        btnCancel.setOnClickListener {
            listener.onAddButtonResult(false, Bundle.EMPTY)
            pin.isGone = true
            btnLayout.isGone = true
            navigationView.isVisible = true
            layoutParams.addRule(RelativeLayout.ABOVE, navigationView.id)
            fragment.layoutParams = layoutParams
        }

        btnOk.setOnClickListener {
            pin.isGone = true
            btnLayout.isGone = true
            navigationView.isVisible = true
            layoutParams.addRule(RelativeLayout.ABOVE, navigationView.id)
            fragment.layoutParams = layoutParams
            val longitude = mapView.mapWindow.map.cameraPosition.target.longitude
            val latitude = mapView.mapWindow.map.cameraPosition.target.latitude
            val bundle = Bundle()
            bundle.putDouble("longitude", longitude)
            bundle.putDouble("latitude", latitude)
            listener.onAddButtonResult(true, bundle)
        }
    }
}
