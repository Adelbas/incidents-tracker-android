package ru.adel.incidentstrackerandroid.ui.main

import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.location.*
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import dagger.hilt.android.AndroidEntryPoint
import ru.adel.incidentstrackerandroid.R
import ru.adel.incidentstrackerandroid.utils.TokenManager
//import ru.adel.incidentstrackerandroid.viewmodels.WebSocketViewModel
import javax.inject.Inject


@AndroidEntryPoint
class MainFragment: Fragment(), UserLocationObjectListener, CameraListener, LocationListener {

//    private val webSocketViewModel: WebSocketViewModel by viewModels()

    private lateinit var mapView: MapView

    private lateinit var mapKit: MapKit

    private lateinit var userLocationLayer: UserLocationLayer

//    private lateinit var locationManager : LocationManager

    private lateinit var btnLocation : FloatingActionButton

    private var routeStartLocation = Point(0.0, 0.0)

    private var followUserLocation = false

    private var zoomValue = 16f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.initialize(requireContext())
        mapKit = MapKitFactory.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapview)
        btnLocation = view.findViewById(R.id.user_location_fab)

        userLocationLayer = mapKit.createUserLocationLayer(mapView.mapWindow)
        userLocationLayer.isVisible = true
        userLocationLayer.isAutoZoomEnabled = true
        userLocationLayer.setObjectListener(this)
        mapView.map.addCameraListener(this)

        cameraUserPosition()
        userInterface()

//        webSocketViewModel.sendMessage()
//        locationManager = mapKit.createLocationManager()
//        locationManager.subscribeForLocationUpdates(0.0,30000,0.0, true, FilteringMode.ON, this)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onDestroy() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onDestroy()
    }

    override fun onObjectAdded(userLocationView: UserLocationView) {
        setAnchor()

        userLocationView.pin.setIcon(ImageProvider.fromResource(requireContext(), R.drawable.ic_location_icon))
        userLocationView.arrow.setIcon(ImageProvider.fromResource(requireContext(), R.drawable.ic_location_icon))
        userLocationView.accuracyCircle.fillColor= Color.TRANSPARENT
    }

    override fun onObjectRemoved(userLocationView: UserLocationView) {}

    override fun onObjectUpdated(userLocationView: UserLocationView, event: ObjectEvent) {}

    override fun onCameraPositionChanged(p0: Map, cPos: CameraPosition, cUpd: CameraUpdateReason, finish: Boolean) {
        if (finish) {
            if (followUserLocation) {
                setAnchor()
            }
        } else {
            if (!followUserLocation) {
                noAnchor()
            }
        }
    }

    override fun onLocationUpdated(location: Location) {
        Log.i("LOCATION-UPDATE", "long: ${location.position.longitude} lat: ${location.position.latitude}")
    }

    override fun onLocationStatusUpdated(locationStatus: LocationStatus) {
        Log.i("LOCATION-STATUS-UPDATE", "status ${locationStatus.toString()}")
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
            moveCameraToPoint(routeStartLocation)
        } else {
            moveCameraToPoint(Point(0.0,0.0))
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

        followUserLocation = false
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
        }

        btnOk.setOnClickListener {
            pin.isGone = true
            btnLayout.isGone = true
            navigationView.isVisible = true
            val longitude = mapView.mapWindow.map.cameraPosition.target.longitude
            val latitude = mapView.mapWindow.map.cameraPosition.target.latitude
            val bundle = Bundle()
            bundle.putDouble("longitude", longitude)
            bundle.putDouble("latitude", latitude)
            listener.onAddButtonResult(true, bundle)
        }
    }
}
