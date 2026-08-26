package com.example.walletwise.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walletwise.R
import com.example.walletwise.network.ForgotPasswordRequest
import com.example.walletwise.network.RetrofitClient
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var btnSendCode: Button
    private lateinit var tvGoToLogIn: TextView
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_forgot_password)

        etEmail = findViewById(R.id.etEmail)
        btnSendCode = findViewById(R.id.btnSendCode)
        tvGoToLogIn = findViewById(R.id.tvGoToLogIn)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        tvGoToLogIn.setOnClickListener {
            finish()
        }

        btnSendCode.setOnClickListener {
            sendCode()
        }
    }

    private fun sendCode() {

        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {

            etEmail.error = "Enter your email"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()
        ) {

            etEmail.error = "Enter a valid email"
            return
        }

        btnSendCode.isEnabled = false

        lifecycleScope.launch {

            try {

                val response =
                    RetrofitClient.apiService.forgotPassword(
                        ForgotPasswordRequest(email)
                    )

                if (response.isSuccessful) {

                    val body = response.body()

                    if (body?.success == true) {

                        val userId =
                            body.data?.user_id ?: return@launch

                        val intent = Intent(
                            this@ForgotPasswordActivity,
                            OtpVerificationActivity::class.java
                        )

                        intent.putExtra(
                            "user_id",
                            userId
                        )

                        intent.putExtra(
                            "email",
                            email
                        )

                        startActivity(intent)

                    } else {

                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            body?.message ?: "Something went wrong",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {

                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Server error: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@ForgotPasswordActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                btnSendCode.isEnabled = true
            }
        }
    }
}