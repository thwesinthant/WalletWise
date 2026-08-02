package com.example.walletwise.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.walletwise.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
//
//        binding.tvGoToSignUp.setOnClickListener {
//            startActivity(Intent(this, RegisterActivity::class.java))
//        }

//        binding.tvForgotPassword.setOnClickListener {
//            // TODO: launch ForgotPasswordActivity once built (Phase 4)
//        }
//
//        binding.tvContinueAsGuest.setOnClickListener {
//            // TODO: skip auth entirely and go straight to MainActivity (Dashboard)
//            // since MoneyMate is single-user/local-only, "guest" can just mean "no PIN lock"
//        }
//
//        binding.btnSignIn.setOnClickListener {
//            val email = binding.etEmail.text?.toString().orEmpty()
//            val password = binding.etPassword.text?.toString().orEmpty()
//            // TODO: replace with LoginViewModel call once User entity/DAO exist (Phase 3/4)
//        }
    }
}