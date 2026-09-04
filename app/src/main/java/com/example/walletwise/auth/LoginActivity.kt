package com.example.walletwise.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.databinding.ActivityLoginBinding
import com.example.walletwise.dashboard.DashboardActivity
import com.example.walletwise.util.PasswordUtils
import com.example.walletwise.util.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auto-login: if a session exists, skip straight to Dashboard
        if (SessionManager.isLoggedIn(this)) {
            val userId = SessionManager.getUserId(this)
            val intent = Intent(this, DashboardActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
            finish()
            return
        }


        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        Toast.makeText(
            this,
            "Room database opened",
            Toast.LENGTH_SHORT
        ).show()

        setupListeners()
    }

    private fun setupListeners() {

        // Login button
        binding.btnLogIn.setOnClickListener {
            loginUser()
        }

        // Go to Register
        binding.tvGoToSignUp.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Forgot Password
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }


        // Password visibility
        binding.btnTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun loginUser() {

        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (email.isEmpty()) {
            binding.etEmail.error = "Enter your email"
            binding.etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Enter your password"
            binding.etPassword.requestFocus()
            return
        }

        lifecycleScope.launch {

            val user = database.userDao().login(
                email = email,
                password = PasswordUtils.hash(password)
            )

            if (user != null) {
                if (binding.cbRememberMe.isChecked) {
                    SessionManager.saveUserSession(this@LoginActivity, user.userId)
                } else {
                    // in case a previous session lingers from an earlier "remember me" login
                    SessionManager.clearSession(this@LoginActivity)
                }

                Toast.makeText(
                    this@LoginActivity,
                    "Login successful!",
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                intent.putExtra("USER_ID", user.userId)
                startActivity(intent)
                finish()

            }  else {
                Toast.makeText(
                    this@LoginActivity,
                    "Invalid email or password",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun togglePasswordVisibility() {

        val currentType = binding.etPassword.inputType

        if (currentType == 129) {
            binding.etPassword.inputType = 1
        } else {
            binding.etPassword.inputType = 129
        }

        binding.etPassword.setSelection(
            binding.etPassword.text.length
        )
    }
}