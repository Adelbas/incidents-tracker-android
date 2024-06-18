package ru.adel.incidentstrackerandroid.ui.main

import android.content.Intent
import ru.adel.incidentstrackerandroid.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.NumberPicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.adel.incidentstrackerandroid.utils.ApiResponse
import ru.adel.incidentstrackerandroid.utils.WebSocketService
import ru.adel.incidentstrackerandroid.utils.coroutinesErrorHandler
import ru.adel.incidentstrackerandroid.viewmodels.MainViewModel
import ru.adel.incidentstrackerandroid.viewmodels.TokenViewModel


@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val mainViewModel: MainViewModel by viewModels()

    private val tokenViewModel: TokenViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val numberPicker = view.findViewById<NumberPicker>(R.id.radiusNumberPicker)
        val radiusCheckbox = view.findViewById<CheckBox>(R.id.checkboxIsRadiusVisible)
        val buttonSave = view.findViewById<Button>(R.id.buttonSave)
        val buttonExit = view.findViewById<Button>(R.id.buttonExit)

        numberPicker.setMaxValue(NOTIFICATION_MAX_DISTANCE)
        numberPicker.setMinValue(NOTIFICATION_MIN_DISTANCE)

        val currentNotificationDistance = mainViewModel.getNotificationDistance()
        numberPicker.value = currentNotificationDistance
        numberPicker.wrapSelectorWheel = false

        radiusCheckbox.isChecked = mainViewModel.getRadiusVisible()

        buttonSave.setOnClickListener {
            mainViewModel.saveRadiusVisible(radiusCheckbox.isChecked)
            if (numberPicker.value != currentNotificationDistance) {
                mainViewModel.saveNotificationDistance(
                    numberPicker.value,
                    coroutinesErrorHandler,
                )
            }
        }

        buttonExit.setOnClickListener {
            mainViewModel.logout()
        }

        mainViewModel.logoutResponse.observe(viewLifecycleOwner) {
            when(it) {
                is ApiResponse.Failure -> {
                    Log.e("Error", "Error! ${it.errorMessage}")
                    Toast.makeText(requireContext(), "Ошибка", Toast.LENGTH_SHORT).show()
                }
                ApiResponse.Loading -> {
                    Log.i("LOGOUT", "Loading")
                }
                is ApiResponse.Success -> {
                    Log.i("LOGOUT", "Success")
                    tokenViewModel.deleteToken()
                    Intent(requireContext(), WebSocketService::class.java).apply {
                        action = WebSocketService.ACTION_STOP
                        requireActivity().startService(this)
                    }
                    findNavController().navigate(R.id.action_settings_to_loginFragment)
                }
            }
        }
    }

    companion object {
        const val NOTIFICATION_MIN_DISTANCE = 500
        const val NOTIFICATION_MAX_DISTANCE = 30000
    }
}