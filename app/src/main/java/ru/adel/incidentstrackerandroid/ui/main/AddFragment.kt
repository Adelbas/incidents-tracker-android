package ru.adel.incidentstrackerandroid.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.adel.incidentstrackerandroid.R
import ru.adel.incidentstrackerandroid.models.IncidentPostRequest
import ru.adel.incidentstrackerandroid.utils.ApiResponse
import ru.adel.incidentstrackerandroid.utils.coroutinesErrorHandler
import ru.adel.incidentstrackerandroid.utils.hideKeyboard
import ru.adel.incidentstrackerandroid.viewmodels.MainViewModel
import java.io.ByteArrayOutputStream


@AndroidEntryPoint
class AddFragment : Fragment() {

    private val mainViewModel : MainViewModel by viewModels()

    private lateinit var selectImageLauncher: ActivityResultLauncher<Intent>

    private var selectedImageUri: Uri? = null

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
        val btnSelectImage = view.findViewById<Button>(R.id.selectImageButton)
        val selectedImageView = view.findViewById<ImageView>(R.id.selectedImageView)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val foreground = view.findViewById<LinearLayout>(R.id.addLayout)

        btnSelectImage.setOnClickListener {
            openGallery()
        }

        selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                selectedImageUri = data?.data
                selectedImageUri?.let { uri ->
                    selectedImageView.setImageURI(uri)
                }
            }
        }

        submitButton.setOnClickListener {
            hideKeyboard(requireActivity(), requireView())
            val title = titleText.text.toString()
            val latitude = arguments?.getDouble("latitude")
            val longitude = arguments?.getDouble("longitude")

            if (title.isNotEmpty() && selectedImageUri != null && latitude != null && longitude != null) {
                val image = uriToByteArray(requireContext(), selectedImageUri!!)
                mainViewModel.createIncident(
                    IncidentPostRequest(
                        title,
                        longitude,
                        latitude,
                        image!!
                    ),
                    coroutinesErrorHandler
                )
            } else {
                Toast.makeText(requireContext(), "Введите название и выберите изображение", Toast.LENGTH_SHORT).show()
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
                    findNavController().navigate(R.id.action_add_to_MainFragment)
                }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        selectImageLauncher.launch(intent)
    }

    private fun uriToByteArray(context: Context, uri: Uri): ByteArray? {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }
}