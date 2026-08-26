package com.example.walletwise.profile

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walletwise.R
import com.example.walletwise.auth.LoginActivity
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri
import android.widget.ImageView
import com.example.walletwise.util.BottomNavHelper
import com.example.walletwise.util.NavTab
import java.io.File


class ProfileActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private var userId: Int = -1

    companion object {
        private const val PREFS_NAME = "walletwise_session"
        private const val KEY_USER_ID = "USER_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        BottomNavHelper.setup(this, findViewById(android.R.id.content), NavTab.SETTINGS, userId)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        database = AppDatabase.getDatabase(this)

        val ivAvatar = findViewById<ImageView>(R.id.ivAvatar)
        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        val tvNameValue = findViewById<TextView>(R.id.tvNameValue)
        val tvCurrencyValue = findViewById<TextView>(R.id.tvCurrencyValue)
        val tvEmailValue = findViewById<TextView>(R.id.tvEmailValue)
        val rowName = findViewById<LinearLayout>(R.id.rowName)
        val rowCurrency = findViewById<LinearLayout>(R.id.rowCurrency)
        val tvLogout = findViewById<TextView>(R.id.tvLogout)
        val tvDeactivate = findViewById<TextView>(R.id.tvDeactivate)
        val btnBack = findViewById<android.widget.ImageView>(R.id.btnBack)

        btnBack?.setOnClickListener { finish() }

        // Live-updating profile data via Room Flow
        lifecycleScope.launch {
            database.userDao().getUserById(userId).collect { user ->
                user?.let {
                    withContext(Dispatchers.Main) {
                        tvGreeting.text = "Hi, ${it.fullName}!"
                        tvNameValue.text = it.fullName
                        tvCurrencyValue.text = it.currency
                        tvEmailValue.text = it.email

                        // Load profile photo if it exists
                        it.profileImage?.let { path ->
                            val imgFile = File(path)
                            if (imgFile.exists()) {
                                ivAvatar.imageTintList = null
                                ivAvatar.scaleType = ImageView.ScaleType.CENTER_CROP  // ← fill the circle properly
                                ivAvatar.setPadding(0, 0, 0, 0)
                                ivAvatar.setImageURI(Uri.fromFile(imgFile))
                            }
                        }
                    }
                }
            }
        }

        val goToEdit = {
            val intent = Intent(this, EditProfileActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }
        rowName.setOnClickListener { goToEdit() }
        rowCurrency.setOnClickListener { goToEdit() }

        tvLogout.setOnClickListener {
            SessionManager.clearSession(this)

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        tvDeactivate.setOnClickListener {
            // Placeholder — needs a confirm dialog + a "deactivate" flag in User, or a delete call
            Toast.makeText(this, "Deactivate flow not built yet", Toast.LENGTH_SHORT).show()
        }
    }
}