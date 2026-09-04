package com.example.walletwise.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.walletwise.R
import com.example.walletwise.auth.LoginActivity
import com.example.walletwise.auth.RegisterActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewFlipper: ViewFlipper

    // Stores whether the user has already completed/skipped onboarding
    private val prefs by lazy {
        getSharedPreferences("walletwise_prefs", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        // Android system splash screen
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // If onboarding has already been completed,
        // go directly to Login
        if (prefs.getBoolean("onboarding_completed", false)) {
            openLogin()
            return
        }

        // First-time user → show onboarding
        setContentView(R.layout.activity_onboarding)

        viewFlipper = findViewById(R.id.viewFlipper)

        // Page 1 → Skip
        findViewById<View>(R.id.loginButton).setOnClickListener {
            openRegister()
        }

        // Page 1 → Page 2
        findViewById<View>(R.id.next1).setOnClickListener {
            viewFlipper.showNext()
        }

        // Page 2 → Skip
        findViewById<View>(R.id.back2).setOnClickListener {
            openRegister()
        }

        // Page 2 → Page 3
        findViewById<View>(R.id.next2).setOnClickListener {
            viewFlipper.showNext()
        }

        // Page 3 → Page 2
        findViewById<View>(R.id.back3).setOnClickListener {
            viewFlipper.showPrevious()
        }

        // Page 3 → Register
        findViewById<View>(R.id.getStartedButton).setOnClickListener {
            openRegister()
        }
    }

    /**
     * Mark onboarding as completed and open Register screen.
     */
    private fun openRegister() {

        prefs.edit()
            .putBoolean("onboarding_completed", true)
            .apply()

        startActivity(
            Intent(this, RegisterActivity::class.java)
        )

        finish()
    }

    /**
     * Open Login screen for returning users.
     */
    private fun openLogin() {

        startActivity(
            Intent(this, LoginActivity::class.java)
        )

        finish()
    }
}