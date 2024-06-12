package ru.adel.incidentstrackerandroid.ui.auth

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import dagger.hilt.android.AndroidEntryPoint
import ru.adel.incidentstrackerandroid.R
import ru.adel.incidentstrackerandroid.models.AuthenticationRequest
import ru.adel.incidentstrackerandroid.utils.ApiResponse
import ru.adel.incidentstrackerandroid.viewmodels.AuthViewModel
import ru.adel.incidentstrackerandroid.viewmodels.CoroutinesErrorHandler
import ru.adel.incidentstrackerandroid.viewmodels.TokenViewModel

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels()

    private val tokenViewModel: TokenViewModel by activityViewModels()

    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val foreground = view.findViewById<LinearLayout>(R.id.lLayout)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)

        tokenViewModel.accessToken.observe(viewLifecycleOwner) { token ->
            if (token != null)
                navController.navigate(R.id.action_loginFragment_to_main_nav_graph)
            else foreground.visibility = View.VISIBLE
        }

        btnRegister.setOnClickListener {
//            navController.navigate(R.id.action_loginFragment_to_registerFragment)
            navController.navigate(R.id.action_loginFragment_to_main_nav_graph)
        }

        viewModel.loginResponse.observe(viewLifecycleOwner) {
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
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Успешный вход", Toast.LENGTH_SHORT).show()
                    tokenViewModel.saveToken(it.data.accessToken, it.data.refreshToken)
                }
            }
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(
                    AuthenticationRequest(email, password),
                    object: CoroutinesErrorHandler {
                        override fun onError(message: String) {
                            Log.e("Error", "Error! $message")
                            Toast.makeText(requireContext(), "Ошибка", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }
    }
}