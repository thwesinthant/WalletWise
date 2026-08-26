package com.example.walletwise.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walletwise.R
import com.example.walletwise.network.ResetPasswordRequest
import com.example.walletwise.network.RetrofitClient
import kotlinx.coroutines.launch
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.util.PasswordUtils
import android.text.InputType

class ResetPasswordActivity : AppCompatActivity() {

    private var resetToken: String = ""
    private var email: String = ""

    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnResetPassword: Button
    private lateinit var btnBack: ImageButton
    private lateinit var btnToggleNewPassword: ImageButton
    private lateinit var btnToggleConfirmPassword: ImageButton

    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_reset_password
        )

        resetToken = intent.getStringExtra(
            "reset_token"
        ) ?: ""
        email = intent.getStringExtra("email") ?: ""

        etNewPassword =
            findViewById(R.id.etNewPassword)

        etConfirmPassword =
            findViewById(R.id.etConfirmPassword)

        btnResetPassword =
            findViewById(R.id.btnResetPassword)

        btnBack =
            findViewById(R.id.btnBack)

        btnToggleNewPassword =
            findViewById(R.id.btnToggleNewPassword)

        btnToggleConfirmPassword =
            findViewById(R.id.btnToggleConfirmPassword)

        btnBack.setOnClickListener {
            finish()
        }

        btnResetPassword.setOnClickListener {
            resetPassword()
        }

        btnToggleNewPassword.setOnClickListener {
            isNewPasswordVisible = !isNewPasswordVisible
            togglePasswordVisibility(etNewPassword, btnToggleNewPassword, isNewPasswordVisible)
        }

        btnToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePasswordVisibility(etConfirmPassword, btnToggleConfirmPassword, isConfirmPasswordVisible)
        }

    }

    private fun togglePasswordVisibility(
        editText: EditText,
        toggleButton: ImageButton,
        isVisible: Boolean
    ) {
        if (isVisible) {
            editText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleButton.setImageResource(android.R.drawable.ic_menu_view)
        } else {
            editText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleButton.setImageResource(android.R.drawable.ic_secure)
        }

        // Keep cursor at the end after switching input type
        editText.setSelection(editText.text?.length ?: 0)
    }

    private fun resetPassword() {

        val password =
            etNewPassword.text.toString()

        val confirmPassword =
            etConfirmPassword.text.toString()

        if (password.length < 8) {

            etNewPassword.error =
                "Minimum 8 characters"

            return
        }

        if (password != confirmPassword) {

            etConfirmPassword.error =
                "Passwords do not match"

            return
        }

        if (resetToken.isEmpty()) {

            Toast.makeText(
                this,
                "Invalid reset session",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        btnResetPassword.isEnabled = false

        lifecycleScope.launch {

            try {

                val response =
                    RetrofitClient.apiService.resetPassword(
                        ResetPasswordRequest(
                            reset_token = resetToken,
                            new_password = password
                        )
                    )

                if (response.isSuccessful) {

                    val body = response.body()

                    if (body?.success == true) {
                        val syncedEmail = body.data?.email ?: email

                        // Mirror the new password into Room so local login stays in sync
                        val database = AppDatabase.getDatabase(this@ResetPasswordActivity)
                        database.userDao().updatePassword(
                            email = syncedEmail,
                            newPasswordHash = PasswordUtils.hash(password)
                        )

                        Toast.makeText(
                            this@ResetPasswordActivity,
                            "Password reset successfully",
                            Toast.LENGTH_LONG
                        ).show()

                        val intent = Intent(
                            this@ResetPasswordActivity,
                            LoginActivity::class.java
                        )

                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP

                        startActivity(intent)

                        finish()

                    } else {

                        Toast.makeText(
                            this@ResetPasswordActivity,
                            body?.message
                                ?: "Password reset failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {

                    Toast.makeText(
                        this@ResetPasswordActivity,
                        "Server error",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@ResetPasswordActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                btnResetPassword.isEnabled = true
            }
        }
    }
}