package ru.adel.incidentstrackerandroid.ui.main

import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import ru.adel.incidentstrackerandroid.R
import ru.adel.incidentstrackerandroid.models.IncidentPostRequest
import ru.adel.incidentstrackerandroid.utils.ApiResponse
import ru.adel.incidentstrackerandroid.viewmodels.CoroutinesErrorHandler
import ru.adel.incidentstrackerandroid.viewmodels.MainViewModel


@AndroidEntryPoint
class AddFragment : Fragment() {

    private val mainViewModel : MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val submitButton = view.findViewById<Button>(R.id.submitBtn)
        val titleText = view.findViewById<EditText>(R.id.titleText)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val foreground = view.findViewById<LinearLayout>(R.id.addLayout)


//        if (arguments != null) {
//            latitude = requireArguments().getDouble("latitude")
//            longitude = requireArguments().getDouble("longitude")
//        }

        submitButton.setOnClickListener {
            val title = titleText.text.toString()
            val latitude = arguments?.getDouble("latitude")
            val longitude = arguments?.getDouble("longitude")

            if (title.isNotEmpty()) {
                mainViewModel.createIncident(
                    IncidentPostRequest(
                        title, longitude!!, latitude!!
                    ),
                    object: CoroutinesErrorHandler {
                        override fun onError(message: String) {
                            Log.e("Error", "Error! $message")
                            Toast.makeText(requireContext(), "Ошибка", Toast.LENGTH_SHORT).show()
                            foreground.visibility = View.VISIBLE
                            progressBar.visibility = View.GONE
                        }
                    }
                )
                hideKeyboard()
            } else {
                Toast.makeText(requireContext(), "Введите название", Toast.LENGTH_SHORT).show()
            }
        }

        mainViewModel.createIncidentResponse.observe(viewLifecycleOwner) {
            when(it) {
                is ApiResponse.Failure -> {
                    foreground.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    Log.e("Error", "Error! ${it.errorMessage}")
                    Toast.makeText(requireContext(), "Ошибка", Toast.LENGTH_SHORT).show()
                }
                ApiResponse.Loading -> {
                    foreground.visibility = View.GONE
                    progressBar.visibility = View.VISIBLE
                }
                is ApiResponse.Success -> {
                    foreground.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    titleText.text.clear()
                    Toast.makeText(requireContext(), "Происшествие успешно создано", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
        val view: View? = requireActivity().currentFocus
        if (view != null) {
            imm!!.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}