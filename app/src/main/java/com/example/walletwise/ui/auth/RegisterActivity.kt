package com.example.walletwise.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.walletwise.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        binding.btnGetStarted.setOnClickListener {
            handleRegister()
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.tvGoToSignUp.setOnClickListener {
            finish()
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible

        if (isPasswordVisible) {
            binding.etPassword.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.btnTogglePassword.setImageResource(android.R.drawable.ic_menu_view)
        } else {
            binding.etPassword.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.btnTogglePassword.setImageResource(android.R.drawable.ic_secure)
        }

        // Keep cursor at the end after switching input type
        binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
    }

    private fun handleRegister() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (!validateInput(fullName, email, password)) {
            return
        }

        // TODO: Replace with real persistence once Room entities/DAO exist:
        // val user = User(fullName = fullName, email = email, passwordHash = hashPassword(password))
        // lifecycleScope.launch { userDao.insert(user) }

        Toast.makeText(this, "Account created for $fullName", Toast.LENGTH_SHORT).show()

        // Navigate to next screen in flow (e.g. OTP verification or Login)
        // startActivity(Intent(this, OtpVerificationActivity::class.java))
        finish()
    }

    private fun validateInput(fullName: String, email: String, password: String): Boolean {
        if (fullName.isEmpty()) {
            binding.etFullName.error = "Full name is required"
            binding.etFullName.requestFocus()
            return false
        }

        if (fullName.length < 2) {
            binding.etFullName.error = "Name is too short"
            binding.etFullName.requestFocus()
            return false
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            binding.etEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Enter a valid email address"
            binding.etEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            binding.etPassword.requestFocus()
            return false
        }

        if (password.length < 8) {
            binding.etPassword.error = "Password must be at least 8 characters"
            binding.etPassword.requestFocus()
            return false
        }

        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        if (!hasLetter || !hasDigit) {
            binding.etPassword.error = "Password must contain both letters and numbers"
            binding.etPassword.requestFocus()
            return false
        }

        return true
    }
}