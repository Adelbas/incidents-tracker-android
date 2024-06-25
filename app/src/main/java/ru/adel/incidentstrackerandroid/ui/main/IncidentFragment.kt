package ru.adel.incidentstrackerandroid.ui.main

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.*
import com.yandex.mapkit.search.Session.SearchListener
import dagger.hilt.android.AndroidEntryPoint
import ru.adel.incidentstrackerandroid.R
import ru.adel.incidentstrackerandroid.models.IncidentGetResponse
import ru.adel.incidentstrackerandroid.models.InteractionStatus
import ru.adel.incidentstrackerandroid.utils.ApiResponse
import ru.adel.incidentstrackerandroid.utils.WebSocketService
import ru.adel.incidentstrackerandroid.utils.coroutinesErrorHandler
import ru.adel.incidentstrackerandroid.viewmodels.MainViewModel
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class IncidentFragment : Fragment() {

    private val mainViewModel : MainViewModel by viewModels()

    private lateinit var incidentTitleTv: TextView
    private lateinit var createdByTv: TextView
    private lateinit var incidentDateTv: TextView
    private lateinit var viewsCountTv: TextView
    private lateinit var imageView: ImageView
    private lateinit var addressTv: TextView

    private var searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private var searchOptions = SearchOptions().apply {
        searchTypes = SearchType.GEO.value
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_incident, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val foreground = view.findViewById<LinearLayout>(R.id.incidentLayout)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val backButton = view.findViewById<ImageButton>(R.id.backButton)

        incidentTitleTv = view.findViewById(R.id.incidentTitle)
        createdByTv = view.findViewById(R.id.incidentCreatedBy)
        incidentDateTv = view.findViewById(R.id.incidentDate)
        viewsCountTv = view.findViewById(R.id.viewsCount)
        imageView = view.findViewById(R.id.incidentImage)
        addressTv = view.findViewById(R.id.incidentAddress)

        val incidentId = arguments?.getLong("incidentId")
        val timestamp = arguments?.getString("timestamp")

        val incidentDate = LocalDateTime.parse(timestamp).toLocalDate()

        mainViewModel.getIncident(
            incidentId!!,
            incidentDate!!,
            coroutinesErrorHandler
        )

        mainViewModel.getIncidentResponse.observe(viewLifecycleOwner) {
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
                    setIncidentValues(it.data)
                    processIncidentViewedInteraction(it.data)
                }
            }
        }

        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setIncidentValues(response: IncidentGetResponse) {
        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val dateTime = LocalDateTime.parse(response.createdAt).format(dtf)
        val imageBitmap = base64ToBitmap(response.image)

        incidentTitleTv.text = response.title
        createdByTv.text = "Опубликовал: ${response.postedUserLastName} ${response.postedUserFirstName}"
        incidentDateTv.text = "Дата: ${dateTime}"
        viewsCountTv.text = response.views.toString()
        imageView.setImageBitmap(imageBitmap)

        val latitude = response.latitude
        val longitude = response.longitude
        val searchSession = searchManager.submit(Point(latitude, longitude), 16, searchOptions, searchSessionListener)
    }

    private fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedString: ByteArray = Base64.decode(base64String, Base64.DEFAULT)
            val inputStream = ByteArrayInputStream(decodedString)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        }
    }

    private fun processIncidentViewedInteraction(incident: IncidentGetResponse) {
        val intent = Intent(WebSocketService.INCIDENT_INTERACTION)
        intent.putExtra("id", incident.id)
        intent.putExtra("incidentDate", LocalDateTime.parse(incident.createdAt).toLocalDate().toString())
        intent.putExtra("status", InteractionStatus.VIEWED.name)
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
    }

    private val searchSessionListener = object : SearchListener {
        override fun onSearchResponse(response: Response) {
            if (response.collection.children.isNotEmpty()) {
                val address = response.collection.children.first().obj?.metadataContainer?.getItem(
                    ToponymObjectMetadata::class.java
                )?.address?.formattedAddress
                addressTv.text = "Адрес: ${address}"
            }
        }

        override fun onSearchError(p0: com.yandex.runtime.Error) {
            Log.i("SEARCH","Search error")
        }
    }
}