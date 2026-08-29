package com.example.walletwise.auth
import com.example.walletwise.network.RetrofitClient
import com.example.walletwise.network.RegisterRequest
import android.R
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.walletwise.databinding.ActivityRegisterBinding
import androidx.lifecycle.lifecycleScope
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.database.CategorySeedLoader
import com.example.walletwise.entity.User
import com.example.walletwise.util.PasswordUtils
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false


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

        binding.btnToggleConfirmPassword.setOnClickListener {
            toggleConfirmPasswordVisibility()
        }

        binding.btnGetStarted.setOnClickListener {
            handleRegister()
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
            binding.btnTogglePassword.setImageResource(R.drawable.ic_menu_view)
        } else {
            binding.etPassword.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.btnTogglePassword.setImageResource(R.drawable.ic_secure)
        }

        // Keep cursor at the end after switching input type
        binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
    }

    private fun toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible

        if (isConfirmPasswordVisible) {
            binding.etConfirmPassword.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.btnToggleConfirmPassword.setImageResource(android.R.drawable.ic_menu_view)
        } else {
            binding.etConfirmPassword.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.btnToggleConfirmPassword.setImageResource(R.drawable.ic_secure)
        }

        binding.etConfirmPassword.setSelection(binding.etConfirmPassword.text?.length ?: 0)
    }

    private fun handleRegister() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (!validateInput(fullName, email, password)) {
            return
        }

        binding.btnGetStarted.isEnabled = false

        lifecycleScope.launch {
            try {

                val response = RetrofitClient.apiService.register(
                    RegisterRequest(
                        full_name = fullName,
                        email = email,
                        password = password
                    )
                )

                val body = response.body()

                if (response.isSuccessful && body?.success == true) {

                    // MySQL confirmed the account.
                    // Now mirror the account locally in Room.
                    val database = AppDatabase.getDatabase(this@RegisterActivity)

                    val user = User(
                        fullName = fullName,
                        email = email,
                        password = PasswordUtils.hash(password)
                    )

                    val insertedUserId = database.userDao().insertUser(user).toInt()

                    val categories = CategorySeedLoader.loadDefaultEntities(
                        this@RegisterActivity,
                        insertedUserId
                    )

                    database.categoryDao().insertAll(categories)
                    Toast.makeText(
                        this@RegisterActivity,
                        "Account created for $fullName",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()

                } else {

                    val errorMsg =
                        body?.message ?: "Registration failed. Please try again."

                    binding.etEmail.error = errorMsg
                    binding.etEmail.requestFocus()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@RegisterActivity,
                    "No internet connection. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()

            } finally {

                binding.btnGetStarted.isEnabled = true
            }
        }
    }

    private fun validateInput(fullName: String, email: String, password: String): Boolean {

        val confirmPassword = binding.etConfirmPassword.text.toString()

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
            binding.etPassword.error = "Password must contain letters and numbers"
            binding.etPassword.requestFocus()
            return false
        }

        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.error = "Please confirm your password"
            binding.etConfirmPassword.requestFocus()
            return false
        }

        if (confirmPassword != password) {
            binding.etConfirmPassword.error = "Passwords do not match"
            binding.etConfirmPassword.requestFocus()
            return false
        }

        return true
    }

}
