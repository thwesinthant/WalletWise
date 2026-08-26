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
import com.example.walletwise.network.RetrofitClient
import com.example.walletwise.network.VerifyOtpRequest
import kotlinx.coroutines.launch

class OtpVerificationActivity : AppCompatActivity() {

    private lateinit var etOtp1: EditText
    private lateinit var etOtp2: EditText
    private lateinit var etOtp3: EditText
    private lateinit var etOtp4: EditText

    private lateinit var btnVerifyCode: Button
    private lateinit var tvResendCode: TextView
    private lateinit var btnBack: ImageButton

    private var userId: Int = 0
    private var email: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_otp_verification
        )

        userId = intent.getIntExtra(
            "user_id",
            0
        )

        email = intent.getStringExtra(
            "email"
        ) ?: ""

        etOtp1 = findViewById(R.id.etOtp1)
        etOtp2 = findViewById(R.id.etOtp2)
        etOtp3 = findViewById(R.id.etOtp3)
        etOtp4 = findViewById(R.id.etOtp4)

        btnVerifyCode =
            findViewById(R.id.btnVerifyCode)

        tvResendCode =
            findViewById(R.id.tvResendCode)

        btnBack =
            findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        btnVerifyCode.setOnClickListener {
            verifyCode()
        }

        tvResendCode.setOnClickListener {
            resendCode()
        }
    }

    private fun verifyCode() {

        val otp =
            etOtp1.text.toString() +
                    etOtp2.text.toString() +
                    etOtp3.text.toString() +
                    etOtp4.text.toString()

        if (otp.length != 4) {

            Toast.makeText(
                this,
                "Enter the complete 4-digit code",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        btnVerifyCode.isEnabled = false

        lifecycleScope.launch {

            try {

                val response =
                    RetrofitClient.apiService.verifyOtp(
                        VerifyOtpRequest(
                            user_id = userId,
                            otp = otp
                        )
                    )

                if (response.isSuccessful) {

                    val body = response.body()

                    if (body?.success == true) {

                        val resetToken =
                            body.data?.reset_token

                        if (resetToken != null) {

                            val intent = Intent(
                                this@OtpVerificationActivity,
                                ResetPasswordActivity::class.java
                            )

                            intent.putExtra(
                                "reset_token",
                                resetToken
                            )
                            intent.putExtra(
                                "email",
                                email
                            )
                            startActivity(intent)

                            finish()

                        }

                    } else {

                        Toast.makeText(
                            this@OtpVerificationActivity,
                            body?.message
                                ?: "Invalid OTP",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {

                    Toast.makeText(
                        this@OtpVerificationActivity,
                        "Server error",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@OtpVerificationActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                btnVerifyCode.isEnabled = true
            }
        }
    }

    private fun resendCode() {

        if (email.isEmpty()) return

        lifecycleScope.launch {

            try {

                val response =
                    RetrofitClient.apiService.forgotPassword(
                        com.example.walletwise.network
                            .ForgotPasswordRequest(email)
                    )

                if (response.isSuccessful &&
                    response.body()?.success == true
                ) {

                    Toast.makeText(
                        this@OtpVerificationActivity,
                        "New code sent to your email",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this@OtpVerificationActivity,
                        response.body()?.message
                            ?: "Unable to resend code",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@OtpVerificationActivity,
                    "Network error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}