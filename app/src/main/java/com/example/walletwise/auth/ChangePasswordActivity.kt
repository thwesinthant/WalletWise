package com.example.walletwise.auth

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.databinding.ActivityChangePasswordBinding
import com.example.walletwise.network.ChangePasswordRequest
import com.example.walletwise.network.RetrofitClient
import com.example.walletwise.util.PasswordUtils
import com.example.walletwise.util.SessionManager
import kotlinx.coroutines.launch

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var database: AppDatabase

    private var isCurrentPasswordVisible = false
    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        setupListeners()
    }

    private fun setupListeners() {

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Update password button
        binding.btnSend.setOnClickListener {
            changePassword()
        }

        // Current password visibility
        binding.ivToggleCurrentPassword.setOnClickListener {

            isCurrentPasswordVisible = !isCurrentPasswordVisible

            togglePasswordVisibility(
                binding.etCurrentPassword,
                isCurrentPasswordVisible
            )
        }

        // New password visibility
        binding.ivToggleNewPassword.setOnClickListener {

            isNewPasswordVisible = !isNewPasswordVisible

            togglePasswordVisibility(
                binding.etNewPassword,
                isNewPasswordVisible
            )
        }

        // Confirm password visibility
        binding.ivToggleConfirmNewPassword.setOnClickListener {

            isConfirmPasswordVisible = !isConfirmPasswordVisible

            togglePasswordVisibility(
                binding.etConfirmNewPassword,
                isConfirmPasswordVisible
            )
        }
    }

    private fun togglePasswordVisibility(
        editText: EditText,
        isVisible: Boolean
    ) {

        val cursorPosition = editText.selectionStart

        if (isVisible) {

            editText.inputType =
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        } else {

            editText.inputType =
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        editText.setSelection(
            cursorPosition.coerceAtMost(editText.text.length)
        )
    }

    private fun changePassword() {

        val currentPassword =
            binding.etCurrentPassword.text.toString().trim()

        val newPassword =
            binding.etNewPassword.text.toString().trim()

        val confirmPassword =
            binding.etConfirmNewPassword.text.toString().trim()


        // ---------------------------------------------------------
        // 1. Validate current password
        // ---------------------------------------------------------

        if (currentPassword.isEmpty()) {

            binding.etCurrentPassword.error =
                "Enter your current password"

            binding.etCurrentPassword.requestFocus()

            return
        }


        // ---------------------------------------------------------
        // 2. Validate new password
        // ---------------------------------------------------------

        if (newPassword.isEmpty()) {

            binding.etNewPassword.error =
                "Enter your new password"

            binding.etNewPassword.requestFocus()

            return
        }


        // ---------------------------------------------------------
        // 3. Minimum password length
        // ---------------------------------------------------------

        if (newPassword.length < 8) {

            binding.etNewPassword.error =
                "Password must be at least 8 characters"

            binding.etNewPassword.requestFocus()

            return
        }


        // ---------------------------------------------------------
        // 4. Password must contain letter + number
        // ---------------------------------------------------------

        val hasLetter =
            newPassword.any { it.isLetter() }

        val hasDigit =
            newPassword.any { it.isDigit() }

        if (!hasLetter || !hasDigit) {

            binding.etNewPassword.error =
                "Password must contain letters and numbers"

            binding.etNewPassword.requestFocus()

            return
        }


        // ---------------------------------------------------------
        // 5. Confirm password
        // ---------------------------------------------------------

        if (confirmPassword.isEmpty()) {

            binding.etConfirmNewPassword.error =
                "Confirm your new password"

            binding.etConfirmNewPassword.requestFocus()

            return
        }


        // ---------------------------------------------------------
        // 6. Password match
        // ---------------------------------------------------------

        if (newPassword != confirmPassword) {

            binding.etConfirmNewPassword.error =
                "Passwords do not match"

            binding.etConfirmNewPassword.requestFocus()

            return
        }


        // ---------------------------------------------------------
        // 7. New password must be different
        // ---------------------------------------------------------

        if (currentPassword == newPassword) {

            binding.etNewPassword.error =
                "New password must be different from current password"

            binding.etNewPassword.requestFocus()

            return
        }


        // ---------------------------------------------------------
        // 8. Get logged-in user ID
        // ---------------------------------------------------------

        val userId =
            SessionManager.getUserId(this)

        if (userId <= 0) {

            Toast.makeText(
                this,
                "Session expired. Please log in again.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        // ---------------------------------------------------------
        // Disable button while request is running
        // ---------------------------------------------------------

        binding.btnSend.isEnabled = false


        lifecycleScope.launch {

            try {

                // -------------------------------------------------
                // Get current user from Room
                // -------------------------------------------------

                val user =
                    database.userDao()
                        .getUserByIdOnce(userId)

                if (user == null) {

                    Toast.makeText(
                        this@ChangePasswordActivity,
                        "User account not found.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }


                // -------------------------------------------------
                // Send request to PHP/MySQL
                // -------------------------------------------------

                val request =
                    ChangePasswordRequest(
                        email = user.email,
                        current_password = currentPassword,
                        new_password = newPassword
                    )

                val response =
                    RetrofitClient.apiService
                        .changePassword(request)


                // -------------------------------------------------
                // Check HTTP response
                // -------------------------------------------------

                if (!response.isSuccessful) {

                    Toast.makeText(
                        this@ChangePasswordActivity,
                        "Server error: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }


                // -------------------------------------------------
                // Get API response
                // -------------------------------------------------

                val body =
                    response.body()

                if (body == null) {

                    Toast.makeText(
                        this@ChangePasswordActivity,
                        "Invalid server response.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }


                // -------------------------------------------------
                // Password successfully changed
                // -------------------------------------------------

                if (body.success) {

                    database.userDao().updatePassword(
                        email = user.email,
                        newPasswordHash =
                            PasswordUtils.hash(newPassword)
                    )


                    Toast.makeText(
                        this@ChangePasswordActivity,
                        body.message ?: "Password changed successfully.",
                        Toast.LENGTH_LONG
                    ).show()


                    // Return to Profile
                    finish()

                } else {

                    Toast.makeText(
                        this@ChangePasswordActivity,
                        body.message
                            ?: "Password change failed.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    this@ChangePasswordActivity,
                    "Network error. Please check your connection.",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                // Re-enable button
                binding.btnSend.isEnabled = true
            }
        }
    }
}