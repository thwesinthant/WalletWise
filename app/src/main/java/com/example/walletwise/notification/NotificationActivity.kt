package com.example.walletwise.notification

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.walletwise.R
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.profile.ProfileActivity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class NotificationActivity :
    AppCompatActivity() {


    private lateinit var adapter:
            NotificationAdapter

    private lateinit var database:
            AppDatabase

    private var userId:
            Int = -1


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        setContentView(
            R.layout.activity_notifications
        )


        // ========================================================
        // GET USER ID
        // ========================================================

        userId =
            intent.getIntExtra(
                "USER_ID",
                -1
            )


        if (userId == -1) {

            finish()

            return
        }


        // ========================================================
        // VIEWS
        // ========================================================

        val btnBack =
            findViewById<ImageView>(
                R.id.btnBack
            )


        val rvNotifications =
            findViewById<RecyclerView>(
                R.id.rvNotifications
            )


        val layoutEmptyState =
            findViewById<View>(
                R.id.layoutEmptyState
            )


        val navSettings =
            findViewById<ImageView>(
                R.id.btnSettings
            )


        // ========================================================
        // BACK
        // ========================================================

        btnBack.setOnClickListener {

            finish()
        }


        // ========================================================
        // SETTINGS
        // ========================================================

        navSettings.setOnClickListener {

            val intent =
                Intent(
                    this,
                    ProfileActivity::class.java
                )


            intent.putExtra(
                "USER_ID",
                userId
            )


            startActivity(intent)
        }


        // ========================================================
// DATABASE
// ========================================================

        database =
            AppDatabase.getDatabase(
                this
            )


        // ========================================================
        // RECYCLER VIEW
        // ========================================================

        rvNotifications.layoutManager =
            LinearLayoutManager(this)


        // ========================================================
        // ADAPTER
        // ========================================================

        adapter =
            NotificationAdapter(

                emptyList(),

                // ------------------------------------------------
                // MARK AS READ
                // ------------------------------------------------

                onMarkAsReadClick = { notification ->

                    lifecycleScope.launch(
                        Dispatchers.IO
                    ) {

                        database
                            .notificationDao()
                            .markAsRead(
                                notification.notificationId
                            )
                    }
                },


                // ------------------------------------------------
                // DELETE
                // ------------------------------------------------

                onDeleteClick = { notification ->

                    lifecycleScope.launch(
                        Dispatchers.IO
                    ) {

                        database
                            .notificationDao()
                            .deleteNotification(
                                notification.notificationId
                            )
                    }
                }
            )


        rvNotifications.adapter =
            adapter


        // ========================================================
        // OBSERVE NOTIFICATIONS
        // ========================================================

        lifecycleScope.launch {

            database
                .notificationDao()
                .getNotificationsByUser(
                    userId
                )
                .collect { list ->


                    if (list.isEmpty()) {


                        rvNotifications.visibility =
                            View.GONE


                        layoutEmptyState.visibility =
                            View.VISIBLE


                    } else {


                        rvNotifications.visibility =
                            View.VISIBLE


                        layoutEmptyState.visibility =
                            View.GONE


                        adapter.updateList(
                            list
                        )
                    }
                }
        }
    }
}