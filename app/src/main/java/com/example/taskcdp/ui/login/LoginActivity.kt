package com.example.taskcdp.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.taskcdp.R
import com.example.taskcdp.data.model.LoginRequest
import com.example.taskcdp.databinding.ActivityLoginBinding
import com.example.taskcdp.ui.profile.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (authViewModel.userIsAuthenticated()) {
            navigateToProfilePage()
        }

        val savedLoginDetails = authViewModel.getLoginDetails()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.loginState.collect {
                        Timber.e("LoginData:::: ${it.data}")
                        when {
                            it.data.firstName.isNotBlank() && it.data.lastName.isNotBlank() -> {
                                binding.loading.visibility = View.GONE
                                authViewModel.saveUserIsAuthenticated(true)
                                authViewModel.saveUserDetails(it.data)
                                navigateToProfilePage()
                            }

                            it.loading -> {
                                binding.loading.visibility = View.VISIBLE
                                showToast(R.string.loading)
                            }

                            it.error.isNotEmpty() -> {
                                binding.loading.visibility = View.GONE
                                showToast(R.string.an_error_occured)
                            }
                        }
                    }
                }
            }
        }

        binding.rememberMeCheckBox?.setOnCheckedChangeListener { _, isChecked ->
            authViewModel.onRememberMeChecked(isChecked)
        }

        savedLoginDetails?.let { (savedUsername, savedPassword) ->
            binding.username.setText(savedUsername)
            binding.password.setText(savedPassword)
        }
        binding.login.isEnabled = true

        binding.login.setOnClickListener {
            val username = binding.username.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (authViewModel.isUserNameValid(username) && authViewModel.isPasswordValid(password)) {
                authViewModel.rememberMeChecked.observe(this) { isChecked ->
                    if (isChecked) {
                        authViewModel.saveLoginDetails(username, password)
                    } else {
                        authViewModel.clearLoginDetails()
                    }
                }

                authViewModel.login(LoginRequest(username, password))
            } else {
                showToast(R.string.invalid_username_or_password)
            }
        }
    }

    private fun navigateToProfilePage() {
        val nextActivityIntent =
            Intent(applicationContext, MainActivity::class.java)
        nextActivityIntent.action = Intent.ACTION_MAIN
        startActivity(nextActivityIntent)

        finish()
    }

    private fun showToast(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_LONG).show()
    }
}