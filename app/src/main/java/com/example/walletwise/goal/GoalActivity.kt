package com.example.walletwise.goal

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.walletwise.R
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.entity.Goal
import com.example.walletwise.entity.Notification
import com.example.walletwise.notification.NotificationPopupManager
import com.example.walletwise.util.BottomNavHelper
import com.example.walletwise.util.NavTab
import com.google.android.material.textfield.TextInputLayout

import kotlinx.coroutines.launch

import java.text.NumberFormat
import java.util.Locale


class GoalActivity : AppCompatActivity() {

    companion object {

        const val EXTRA_USER_ID = "USER_ID"
    }


    // ============================================================
    // DATABASE
    // ============================================================

    private lateinit var database: AppDatabase

    private lateinit var notificationPopupManager: NotificationPopupManager


    // ============================================================
    // NOTIFICATION OBSERVER
    // ============================================================

    private var notificationObserverInitialized = false

    private var latestNotificationId = 0


    // ============================================================
    // ADAPTER
    // ============================================================

    private lateinit var adapter: GoalAdapter


    // ============================================================
    // USER
    // ============================================================

    private var userId: Int = -1

    private var userCurrency: String = "MMK"


    // ============================================================
    // VIEWS
    // ============================================================

    private lateinit var rvGoals: RecyclerView

    private lateinit var btnAddGoal: View

    private lateinit var btnBack: View

    private lateinit var emptyGoalState: View

    private lateinit var tvActiveGoals: TextView

    private lateinit var tvTotalGoalSaved: TextView


    // ============================================================
    // ON CREATE
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_goals
        )


        // ========================================================
        // GET USER ID
        // ========================================================

        userId = intent.getIntExtra(
            EXTRA_USER_ID,
            -1
        )


        if (userId == -1) {

            Toast.makeText(
                this,
                "User not found",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }


        // ========================================================
        // DATABASE
        // ========================================================

        database =
            AppDatabase.getDatabase(
                applicationContext
            )


        // ========================================================
        // NOTIFICATION POPUP MANAGER
        // ========================================================

        notificationPopupManager =
            NotificationPopupManager(
                this
            )


        // ========================================================
        // INITIALIZE VIEWS
        // ========================================================

        initializeViews()


        // ========================================================
        // BOTTOM NAVIGATION
        // ========================================================

        setupBottomNavigation()


        // ========================================================
        // OBSERVE NOTIFICATIONS
        //
        // IMPORTANT:
        // Start this immediately so that the observer is already
        // listening when a goal-completed notification is inserted.
        // ========================================================

        observeNotificationPopups()


        // ========================================================
        // LOAD USER CURRENCY
        // ========================================================

        loadUserCurrency()
    }


    // ============================================================
    // INITIALIZE VIEWS
    // ============================================================

    private fun initializeViews() {

        rvGoals =
            findViewById(
                R.id.rvGoals
            )


        btnAddGoal =
            findViewById(
                R.id.btnAddGoal
            )


        btnBack =
            findViewById(
                R.id.btnBack
            )


        emptyGoalState =
            findViewById(
                R.id.emptyGoalState
            )


        tvActiveGoals =
            findViewById(
                R.id.tvActiveGoals
            )


        tvTotalGoalSaved =
            findViewById(
                R.id.tvTotalGoalSaved
            )


        // ========================================================
        // BACK BUTTON
        // ========================================================

        btnBack.setOnClickListener {

            finish()
        }
    }


    // ============================================================
    // LOAD USER CURRENCY
    // ============================================================

    private fun loadUserCurrency() {

        lifecycleScope.launch {

            try {

                // =================================================
                // GET CURRENT USER
                // =================================================

                val user =
                    database
                        .userDao()
                        .getUserByIdOnce(
                            userId
                        )


                // =================================================
                // GET CURRENCY
                // =================================================

                userCurrency =
                    user
                        ?.currency
                        ?.trim()
                        ?.takeIf {
                            it.isNotEmpty()
                        }
                        ?: "MMK"


                // =================================================
                // SETUP
                // =================================================

                setupRecyclerView()

                setupAddGoalButton()

                observeGoals()


            } catch (e: Exception) {

                // =================================================
                // FALLBACK CURRENCY
                // =================================================

                userCurrency = "MMK"


                setupRecyclerView()

                setupAddGoalButton()

                observeGoals()
            }
        }
    }


    // ============================================================
    // RECYCLER VIEW
    // ============================================================

    private fun setupRecyclerView() {

        adapter =
            GoalAdapter(

                currency =
                    userCurrency,

                onPiggyBankClick = { goal ->

                    showAddMoneyDialog(
                        goal
                    )
                },

                onDeleteClick = { goal ->

                    showDeleteConfirmation(
                        goal
                    )
                }
            )


        rvGoals.layoutManager =
            LinearLayoutManager(this)


        rvGoals.adapter =
            adapter
    }


    // ============================================================
    // ADD GOAL BUTTON
    // ============================================================

    private fun setupAddGoalButton() {

        btnAddGoal.setOnClickListener {

            showAddGoalDialog()
        }
    }


    // ============================================================
    // BOTTOM NAVIGATION
    // ============================================================

    private fun setupBottomNavigation() {

        BottomNavHelper.setup(

            activity = this,

            root =
                findViewById(
                    android.R.id.content
                ),

            current =
                NavTab.GOALS,

            userId =
                userId
        )
    }


    // ============================================================
    // OBSERVE NOTIFICATION POPUPS
    // ============================================================

    private fun observeNotificationPopups() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                database
                    .notificationDao()
                    .getNotificationsByUser(
                        userId
                    )
                    .collect { notifications ->


                        // =================================================
                        // FIRST EMISSION
                        //
                        // This is the important fix.
                        //
                        // If there are no notifications when the page
                        // opens, initialize latestNotificationId to 0.
                        //
                        // This allows the FIRST notification created
                        // while the page is open to trigger the popup.
                        // =================================================

                        if (
                            !notificationObserverInitialized
                        ) {

                            latestNotificationId =
                                notifications
                                    .firstOrNull()
                                    ?.notificationId
                                    ?: 0


                            notificationObserverInitialized =
                                true


                            return@collect
                        }


                        // =================================================
                        // NO NOTIFICATIONS
                        // =================================================

                        if (
                            notifications.isEmpty()
                        ) {

                            return@collect
                        }


                        // =================================================
                        // GET NEWEST NOTIFICATION
                        //
                        // NotificationDao already uses:
                        //
                        // ORDER BY CAST(created_at AS INTEGER) DESC
                        //
                        // Therefore first() is the newest notification.
                        // =================================================

                        val newestNotification =
                            notifications.first()


                        // =================================================
                        // CHECK IF NEW NOTIFICATION
                        // =================================================

                        if (
                            newestNotification.notificationId >
                            latestNotificationId
                        ) {

                            latestNotificationId =
                                newestNotification.notificationId


                            // =================================================
                            // SHOW POPUP
                            // =================================================

                            notificationPopupManager.show(
                                newestNotification
                            )
                        }
                    }
            }
        }
    }


    // ============================================================
    // OBSERVE GOALS
    // ============================================================

    private fun observeGoals() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                database
                    .goalDao()
                    .getGoalsByUser(
                        userId
                    )
                    .collect { goals ->


                        // =========================================
                        // UPDATE RECYCLER VIEW
                        // =========================================

                        adapter.submitList(
                            goals
                        )


                        // =========================================
                        // EMPTY STATE
                        // =========================================

                        if (
                            goals.isEmpty()
                        ) {

                            emptyGoalState.visibility =
                                View.VISIBLE

                            rvGoals.visibility =
                                View.GONE

                        } else {

                            emptyGoalState.visibility =
                                View.GONE

                            rvGoals.visibility =
                                View.VISIBLE
                        }


                        // =========================================
                        // ACTIVE GOALS
                        // =========================================

                        val activeGoals =
                            goals.count { goal ->

                                goal.currentAmount <
                                        goal.targetAmount
                            }


                        tvActiveGoals.text =
                            activeGoals.toString()


                        // =========================================
                        // TOTAL SAVED
                        // =========================================

                        val totalSaved =
                            goals.sumOf { goal ->

                                goal.currentAmount
                            }


                        tvTotalGoalSaved.text =
                            formatMoney(
                                totalSaved
                            )
                    }
            }
        }
    }


    // ============================================================
    // ADD GOAL DIALOG
    // ============================================================

    private fun showAddGoalDialog() {

        val dialog =
            Dialog(this)


        dialog.setContentView(
            R.layout.dialog_add_goal
        )


        dialog.window?.setBackgroundDrawable(
            ColorDrawable(
                Color.TRANSPARENT
            )
        )


        // ========================================================
        // VIEWS
        // ========================================================

        val etGoalTitle =
            dialog.findViewById<EditText>(
                R.id.etGoalTitle
            )


        val etGoalTarget =
            dialog.findViewById<EditText>(
                R.id.etGoalTarget
            )


        val tilGoalTarget =
            dialog.findViewById<TextInputLayout>(
                R.id.tilGoalTarget
            )


        val btnAdd =
            dialog.findViewById<TextView>(
                R.id.btnAdd
            )


        val btnCancel =
            dialog.findViewById<TextView>(
                R.id.btnCancel
            )


        // ========================================================
        // DYNAMIC CURRENCY
        // ========================================================

        tilGoalTarget.suffixText =
            userCurrency


        // ========================================================
        // ADD GOAL
        // ========================================================

        btnAdd.setOnClickListener {

            val title =
                etGoalTitle
                    .text
                    .toString()
                    .trim()


            val target =
                etGoalTarget
                    .text
                    .toString()
                    .trim()
                    .toDoubleOrNull()


            // ====================================================
            // VALIDATE TITLE
            // ====================================================

            if (
                title.isEmpty()
            ) {

                etGoalTitle.error =
                    "Enter a goal name"

                return@setOnClickListener
            }


            // ====================================================
            // VALIDATE TARGET
            // ====================================================

            if (
                target == null ||
                target <= 0.0
            ) {

                etGoalTarget.error =
                    "Enter a valid target amount"

                return@setOnClickListener
            }


            // ====================================================
            // INSERT GOAL
            // ====================================================

            lifecycleScope.launch {

                try {

                    val goal =
                        Goal(

                            userId =
                                userId,

                            title =
                                title,

                            targetAmount =
                                target,

                            currentAmount =
                                0.0
                        )


                    database
                        .goalDao()
                        .insertGoal(
                            goal
                        )


                    // =================================================
                    // SUCCESS
                    // =================================================

                    Toast.makeText(
                        this@GoalActivity,
                        "Goal created successfully",
                        Toast.LENGTH_SHORT
                    ).show()


                    dialog.dismiss()


                } catch (e: Exception) {

                    Toast.makeText(
                        this@GoalActivity,
                        "Failed to create goal: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }


        // ========================================================
        // CANCEL
        // ========================================================

        btnCancel.setOnClickListener {

            dialog.dismiss()
        }


        // ========================================================
        // SHOW DIALOG
        // ========================================================

        dialog.show()


        setDialogWidth(
            dialog
        )
    }


    // ============================================================
    // ADD MONEY DIALOG
    // ============================================================

    private fun showAddMoneyDialog(
        goal: Goal
    ) {

        val dialog =
            Dialog(this)


        dialog.setContentView(
            R.layout.dialog_add_goal_money
        )


        dialog.window?.setBackgroundDrawable(
            ColorDrawable(
                Color.TRANSPARENT
            )
        )


        // ========================================================
        // VIEWS
        // ========================================================

        val tvAddMoneyTitle =
            dialog.findViewById<TextView>(
                R.id.tvAddMoneyTitle
            )


        val etAddAmount =
            dialog.findViewById<EditText>(
                R.id.etAddAmount
            )


        val tilAddAmount =
            dialog.findViewById<TextInputLayout>(
                R.id.tilAddAmount
            )


        val btnConfirm =
            dialog.findViewById<TextView>(
                R.id.btnAddMoneyConfirm
            )


        val btnCancel =
            dialog.findViewById<TextView>(
                R.id.btnAddMoneyCancel
            )


        // ========================================================
        // DYNAMIC CURRENCY
        // ========================================================

        tilAddAmount.suffixText =
            userCurrency


        // ========================================================
        // TITLE
        // ========================================================

        tvAddMoneyTitle.text =
            "Add Money to ${goal.title}"


        // ========================================================
        // CONFIRM
        // ========================================================

        btnConfirm.setOnClickListener {

            val amountToAdd =
                etAddAmount
                    .text
                    .toString()
                    .trim()
                    .toDoubleOrNull()


            // ====================================================
            // VALIDATION
            // ====================================================

            if (
                amountToAdd == null ||
                amountToAdd <= 0.0
            ) {

                etAddAmount.error =
                    "Enter a valid amount"

                return@setOnClickListener
            }


            // ====================================================
            // CALCULATE NEW AMOUNT
            // ====================================================

            val newAmount =
                (
                        goal.currentAmount +
                                amountToAdd
                        ).coerceAtMost(
                        goal.targetAmount
                    )


            // ====================================================
            // CHECK WHETHER GOAL WILL BE COMPLETED
            // ====================================================

            val goalCompleted =
                newAmount >= goal.targetAmount &&
                        goal.currentAmount <
                        goal.targetAmount


            // ====================================================
            // DATABASE UPDATE
            // ====================================================

            lifecycleScope.launch {

                try {

                    // =============================================
                    // UPDATE GOAL AMOUNT
                    // =============================================

                    database
                        .goalDao()
                        .updateCurrentAmount(

                            goalId =
                                goal.goalId,

                            userId =
                                userId,

                            newAmount =
                                newAmount
                        )


                    // =============================================
                    // GOAL COMPLETED
                    // =============================================

                    if (
                        goalCompleted
                    ) {

                        // =========================================
                        // CHECK DUPLICATE NOTIFICATION
                        // =========================================

                        val existingNotification =
                            database
                                .notificationDao()
                                .countGoalCompletedNotification(

                                    userId =
                                        userId,

                                    goalId =
                                        goal.goalId
                                )


                        // =========================================
                        // CREATE NOTIFICATION ONLY ONCE
                        // =========================================

                        if (
                            existingNotification == 0
                        ) {

                            val notification =
                                Notification(

                                    userId =
                                        userId,

                                    title =
                                        "Goal Completed 🎉",

                                    message =
                                        "\"${goal.title}\" goal has been completed! " +
                                                "You reached " +
                                                "${formatMoney(goal.targetAmount)}.",

                                    type =
                                        "GOAL_COMPLETED",

                                    referenceType =
                                        "GOAL",

                                    referenceId =
                                        goal.goalId,

                                    isRead =
                                        false,

                                    timeAgo =
                                        "Just now",

                                    createdAt =
                                        System.currentTimeMillis()
                                            .toString()
                                )


                            database
                                .notificationDao()
                                .insertNotification(
                                    notification
                                )
                        }


                        // =========================================
                        // SUCCESS MESSAGE
                        // =========================================

                        Toast.makeText(
                            this@GoalActivity,
                            "${goal.title} goal reached! 🎉",
                            Toast.LENGTH_SHORT
                        ).show()


                    } else {

                        // =========================================
                        // NORMAL ADD MONEY
                        // =========================================

                        Toast.makeText(
                            this@GoalActivity,
                            "Money added successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }


                    // =============================================
                    // CLOSE DIALOG
                    // =============================================

                    dialog.dismiss()


                } catch (e: Exception) {

                    Toast.makeText(
                        this@GoalActivity,
                        "Failed to add money: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }


        // ========================================================
        // CANCEL
        // ========================================================

        btnCancel.setOnClickListener {

            dialog.dismiss()
        }


        // ========================================================
        // SHOW
        // ========================================================

        dialog.show()


        setDialogWidth(
            dialog
        )
    }


    // ============================================================
    // DELETE CONFIRMATION
    // ============================================================

    private fun showDeleteConfirmation(
        goal: Goal
    ) {

        AlertDialog.Builder(this)

            .setTitle(
                "Delete Goal?"
            )

            .setMessage(
                "Are you sure you want to delete \"${goal.title}\"?\n\n" +
                        "This action cannot be undone."
            )

            .setNegativeButton(
                "Cancel",
                null
            )

            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                deleteGoal(
                    goal
                )
            }

            .show()
    }


    // ============================================================
    // DELETE GOAL
    // ============================================================

    private fun deleteGoal(
        goal: Goal
    ) {

        lifecycleScope.launch {

            try {

                // =================================================
                // VERIFY OWNERSHIP
                // =================================================

                val existingGoal =
                    database
                        .goalDao()
                        .getGoalByIdForUser(

                            goalId =
                                goal.goalId,

                            userId =
                                userId
                        )


                // =================================================
                // GOAL NOT FOUND
                // =================================================

                if (
                    existingGoal == null
                ) {

                    Toast.makeText(
                        this@GoalActivity,
                        "Goal not found",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }


                // =================================================
                // DELETE
                // =================================================

                database
                    .goalDao()
                    .deleteGoal(
                        existingGoal
                    )


                Toast.makeText(
                    this@GoalActivity,
                    "Goal deleted",
                    Toast.LENGTH_SHORT
                ).show()


            } catch (e: Exception) {

                Toast.makeText(
                    this@GoalActivity,
                    "Failed to delete goal: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // ============================================================
    // FORMAT MONEY
    // ============================================================

    private fun formatMoney(
        amount: Double
    ): String {

        val formatter =
            NumberFormat.getNumberInstance(
                Locale.US
            )


        formatter.maximumFractionDigits =
            0


        formatter.minimumFractionDigits =
            0


        return "${formatter.format(amount)} $userCurrency"
    }


    // ============================================================
    // DIALOG WIDTH
    // ============================================================

    private fun setDialogWidth(
        dialog: Dialog,
        marginDp: Int = 20
    ) {

        val density =
            resources
                .displayMetrics
                .density


        val marginPx =
            (
                    marginDp *
                            density
                    ).toInt()


        val screenWidth =
            resources
                .displayMetrics
                .widthPixels


        dialog.window?.setLayout(

            screenWidth -
                    (
                            marginPx * 2
                            ),

            ViewGroup
                .LayoutParams
                .WRAP_CONTENT
        )
    }


    // ============================================================
    // ON DESTROY
    // ============================================================

    override fun onDestroy() {

        notificationPopupManager.dismiss()

        super.onDestroy()
    }
}