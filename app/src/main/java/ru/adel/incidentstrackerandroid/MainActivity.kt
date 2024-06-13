package ru.adel.incidentstrackerandroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.adel.incidentstrackerandroid.ui.main.MainFragment
import ru.adel.incidentstrackerandroid.ui.main.OnAddButtonClickListener
import ru.adel.incidentstrackerandroid.utils.LocationService


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), OnAddButtonClickListener {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1002

    private lateinit var navController : NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        val haveApiKey = savedInstanceState?.getBoolean("haveApiKey") ?: false
//        if (!haveApiKey) {
//            MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
//        }

        requestLocationPermission()
        processNavigationMenu()
    }



//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        outState.putBoolean("haveApiKey", true)
//    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun requestBackgroundPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.size != 2 ||
                grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_SHORT).show()
                finish()
            } else requestBackgroundPermission()
        } else if (requestCode == BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.size != 1 ||
                grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_SHORT).show()
                finish()
            } //else startLocationService()
        }
    }

//    private fun startLocationService() {
//        Intent(this, LocationService::class.java).apply {
//            action = LocationService.ACTION_START
//            startForegroundService(this)
//        }
//    }

    private fun processNavigationMenu() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
        navController = navHostFragment!!.navController
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation_view)

        bottomNavigationView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { controller: NavController?, destination: NavDestination, arguments: Bundle? ->
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment -> {
                    bottomNavigationView.visibility = View.GONE
                }
                else -> bottomNavigationView.visibility = View.VISIBLE
            }
        }

        bottomNavigationView.setOnItemSelectedListener { item->
            when(item.itemId) {
                R.id.addFragment -> {
                    val currentFragment = navHostFragment.childFragmentManager.fragments[0]
                    if (currentFragment is MainFragment) {
                        currentFragment.onAddButtonClicked(this)
                    } else {
                        navHostFragment.navController.navigate(R.id.mainFragment)
                    }
                    false
                }
                R.id.mainFragment -> {
                    navController.navigate(R.id.mainFragment)
                    true
                }
                R.id.settingsFragment -> {
                    navController.navigate(R.id.settingsFragment)
                    true
                }
                else -> false
            }
        }
    }

    override fun onAddButtonResult(result: Boolean, bundle: Bundle) {
        if (result) {
            navController.navigate(R.id.addFragment, bundle)
        }
    }
}