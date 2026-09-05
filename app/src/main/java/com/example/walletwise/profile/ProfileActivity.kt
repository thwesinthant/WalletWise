package com.example.walletwise.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walletwise.R
import com.example.walletwise.auth.ChangePasswordActivity
import com.example.walletwise.auth.LoginActivity
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.util.BottomNavHelper
import com.example.walletwise.util.NavTab
import com.example.walletwise.util.SessionManager
import kotlinx.coroutines.launch
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase

    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_profile)

        // =========================================================
        // GET USER ID FIRST
        // =========================================================

        userId = intent.getIntExtra("USER_ID", -1)

        if (userId == -1) {

            Toast.makeText(
                this,
                "User not found",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        // =========================================================
        // DATABASE
        // =========================================================

        database = AppDatabase.getDatabase(this)

        // =========================================================
        // BOTTOM NAV
        // =========================================================

        BottomNavHelper.setup(
            activity = this,
            root = findViewById(android.R.id.content),
            current = NavTab.PROFILE,
            userId = userId
        )

        // =========================================================
        // VIEWS
        // =========================================================

        val ivAvatar =
            findViewById<ImageView>(R.id.ivAvatar)

        val btnEditPhoto =
            findViewById<TextView>(R.id.btnEditPhoto)

        val tvGreeting =
            findViewById<TextView>(R.id.tvGreeting)

        val tvNameValue =
            findViewById<TextView>(R.id.tvNameValue)

        val tvCurrencyValue =
            findViewById<TextView>(R.id.tvCurrencyValue)

        val tvEmailValue =
            findViewById<TextView>(R.id.tvEmailValue)

        val rowName =
            findViewById<LinearLayout>(R.id.rowName)

        val rowCurrency =
            findViewById<LinearLayout>(R.id.rowCurrency)

        val rowChangePassword =
            findViewById<LinearLayout>(R.id.rowChangePassword)

        val tvLogout =
            findViewById<TextView>(R.id.tvLogout)


        val btnBack =
            findViewById<ImageView>(R.id.btnBack)

        // =========================================================
        // BACK
        // =========================================================

        btnBack.setOnClickListener {
            finish()
        }

        // =========================================================
        // OBSERVE USER
        // =========================================================

        lifecycleScope.launch {

            database
                .userDao()
                .getUserById(userId)
                .collect { user ->

                    user ?: return@collect

                    tvGreeting.text =
                        "Hi, ${user.fullName}!"

                    tvNameValue.text =
                        user.fullName

                    tvCurrencyValue.text =
                        user.currency

                    tvEmailValue.text =
                        user.email

                    // Profile image
                    user.profileImage?.let { path ->

                        val imgFile = File(path)

                        if (imgFile.exists()) {

                            ivAvatar.imageTintList = null

                            ivAvatar.scaleType =
                                ImageView.ScaleType.CENTER_CROP

                            ivAvatar.setPadding(
                                0,
                                0,
                                0,
                                0
                            )

                            ivAvatar.setImageURI(
                                Uri.fromFile(imgFile)
                            )
                        }
                    }
                }
        }

        // =========================================================
        // EDIT PROFILE
        // =========================================================

        val goToEdit = {

            val intent =
                Intent(
                    this,
                    EditProfileActivity::class.java
                )

            intent.putExtra(
                "USER_ID",
                userId
            )

            startActivity(intent)
        }

        rowName.setOnClickListener {
            goToEdit()
        }

        rowCurrency.setOnClickListener {
            goToEdit()
        }

        btnEditPhoto.setOnClickListener {
            goToEdit()
        }

        // =========================================================
        // CHANGE PASSWORD
        // =========================================================

        rowChangePassword.setOnClickListener {

            val intent =
                Intent(
                    this,
                    ChangePasswordActivity::class.java
                )

            intent.putExtra(
                "USER_ID",
                userId
            )

            startActivity(intent)
        }

        // =========================================================
        // LOGOUT
        // =========================================================

        tvLogout.setOnClickListener {

            SessionManager.clearSession(this)

            val intent =
                Intent(
                    this,
                    LoginActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            finish()
        }


    }
}

