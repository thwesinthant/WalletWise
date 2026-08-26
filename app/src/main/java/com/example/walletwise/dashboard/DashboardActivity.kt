package com.example.walletwise.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walletwise.R
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.databinding.ActivityDashboardBinding
import com.example.walletwise.profile.EditProfileActivity
import com.example.walletwise.profile.ProfileActivity
import kotlinx.coroutines.launch
import android.net.Uri
import android.view.View
import com.example.walletwise.notification.NotificationActivity
import com.example.walletwise.util.BottomNavHelper
import com.example.walletwise.util.NavTab
import java.io.File
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)

        if (userId == -1) {
            Toast.makeText(
                this,
                "Dashboard User ID: $userId",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        loadUserInfo()
        setupListeners()
    }

    private fun loadUserInfo() {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@DashboardActivity)
            database.userDao().getUserById(userId).collect { user ->
                if (user == null) {
                    Toast.makeText(
                        this@DashboardActivity,
                        "Unable to load user data",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@collect
                }

                binding.greetName.text = getString(
                    R.string.greet_name, user.fullName)

                // Show profile photo if it exists, otherwise keep the initial-letter badge
                user.profileImage?.let { path ->
                    val imgFile = File(path)
                    if (imgFile.exists()) {
                        binding.ivHomeAvatar.setImageURI(Uri.fromFile(imgFile))
                        binding.ivHomeAvatar.visibility = View.VISIBLE
                        binding.tvAvatarInitial.visibility = View.GONE
                    }
                }

                // TODO: wire balanceAmount / expenseAmount / incomeAmount
            }
        }
    }

    private fun setupListeners() {
        BottomNavHelper.setup(this, binding.root, NavTab.HOME, userId)

        binding.ivNoti.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        binding.btnViewAllExpenses.setOnClickListener { /* TODO */ }
        binding.btnSeeAllTransactions.setOnClickListener { /* TODO */ }
        binding.fabAdd.setOnClickListener { /* TODO */ }
    }
}