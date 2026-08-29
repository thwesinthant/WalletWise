package com.example.walletwise.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.walletwise.R
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.notification.NotificationActivity
import com.example.walletwise.profile.ProfileActivity
import com.example.walletwise.transactions.AddTransactionActivity
import com.example.walletwise.transactions.TransactionActivity
import com.example.walletwise.transactions.TransactionAdapter
import com.example.walletwise.util.BottomNavHelper
import com.example.walletwise.util.NavTab
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File


class DashboardActivity : AppCompatActivity() {

    // =============================================================
    // USER
    // =============================================================

    private var currentUserId: Int = -1

    private var userCurrency: String = "MMK"


    // =============================================================
    // FINANCIAL DATA
    // =============================================================

    private var latestExpense: Double = 0.0

    private var latestIncome: Double = 0.0

    private var latestBalance: Double = 0.0


    // =============================================================
    // DATABASE
    // =============================================================

    private lateinit var database: AppDatabase


    // =============================================================
    // ADAPTER
    // =============================================================

    private lateinit var txnAdapter: TransactionAdapter


    // =============================================================
    // VIEWS
    // =============================================================

    private lateinit var greetName: TextView

    private lateinit var tvAvatarInitial: TextView

    private lateinit var ivDashboardAvatar: ImageView

    private lateinit var balanceAmount: TextView

    private lateinit var expenseAmount: TextView

    private lateinit var incomeAmount: TextView

    private lateinit var rvDashboardTransactions:
            RecyclerView


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_dashboard
        )


        // =========================================================
        // USER
        // =========================================================

        currentUserId =
            intent.getIntExtra(
                "USER_ID",
                -1
            )


        if (
            currentUserId == -1
        ) {

            finish()

            return
        }


        // =========================================================
        // DATABASE
        // =========================================================

        database =
            AppDatabase.getDatabase(
                applicationContext
            )


        // =========================================================
        // VIEWS
        // =========================================================

        greetName =
            findViewById(
                R.id.greetName
            )


        tvAvatarInitial =
            findViewById(
                R.id.tvAvatarInitial
            )


        ivDashboardAvatar =
            findViewById(
                R.id.ivDashboardAvatar
            )


        balanceAmount =
            findViewById(
                R.id.balanceAmount
            )


        expenseAmount =
            findViewById(
                R.id.expenseAmount
            )


        incomeAmount =
            findViewById(
                R.id.incomeAmount
            )


        rvDashboardTransactions =
            findViewById(
                R.id.rvDashboardTransactions
            )


        // =========================================================
        // BOTTOM NAV
        // =========================================================

        BottomNavHelper.setup(
            activity = this,
            root =
                findViewById(
                    android.R.id.content
                ),
            current = NavTab.HOME,
            userId = currentUserId
        )


        // =========================================================
        // RECYCLER VIEW
        // =========================================================

        rvDashboardTransactions.layoutManager =
            LinearLayoutManager(
                this
            )


        txnAdapter =
            TransactionAdapter(
                emptyList()
            )


        rvDashboardTransactions.adapter =
            txnAdapter


        // =========================================================
        // NAVIGATION
        // =========================================================

        setupNavigation()


        // =========================================================
        // ROOM OBSERVERS
        // =========================================================

        observeUser()

        observeFinancialData()

        observeRecentTransactions()
    }


    // =============================================================
    // NAVIGATION
    // =============================================================

    private fun setupNavigation() {


        // ---------------------------------------------------------
        // NOTIFICATION
        // ---------------------------------------------------------

        findViewById<ImageView>(
            R.id.ivNoti
        ).setOnClickListener {

            val intent =
                Intent(
                    this,
                    NotificationActivity::class.java
                )

            intent.putExtra(
                "USER_ID",
                currentUserId
            )

            startActivity(
                intent
            )
        }


        // ---------------------------------------------------------
        // ADD TRANSACTION
        // ---------------------------------------------------------

        findViewById<TextView>(
            R.id.fabAdd
        ).setOnClickListener {

            val intent =
                Intent(
                    this,
                    AddTransactionActivity::class.java
                )

            intent.putExtra(
                "USER_ID",
                currentUserId
            )

            startActivity(
                intent
            )
        }


        // ---------------------------------------------------------
        // VIEW ALL TRANSACTIONS
        // ---------------------------------------------------------

        findViewById<TextView>(
            R.id.btnViewAllExpenses
        ).setOnClickListener {

            val intent =
                Intent(
                    this,
                    TransactionActivity::class.java
                )

            intent.putExtra(
                "USER_ID",
                currentUserId
            )

            startActivity(
                intent
            )
        }


        // ---------------------------------------------------------
        // PROFILE
        // ---------------------------------------------------------

        findViewById<View>(
            R.id.avatarContainer
        ).setOnClickListener {

            val intent =
                Intent(
                    this,
                    ProfileActivity::class.java
                )

            intent.putExtra(
                "USER_ID",
                currentUserId
            )

            startActivity(
                intent
            )
        }
    }


    // =============================================================
    // OBSERVE USER
    // =============================================================

    private fun observeUser() {

        lifecycleScope.launch {

            database
                .userDao()
                .getUserById(
                    currentUserId
                )
                .collect { user ->

                    user ?: return@collect


                    // =============================================
                    // GREETING
                    // =============================================

                    greetName.text =
                        "Hello, ${user.fullName}"


                    // =============================================
                    // AVATAR INITIAL
                    // =============================================

                    if (
                        user.fullName.isNotEmpty()
                    ) {

                        tvAvatarInitial.text =
                            user.fullName
                                .first()
                                .uppercase()
                    }


                    // =============================================
                    // PROFILE IMAGE
                    // =============================================

                    val imagePath =
                        user.profileImage


                    if (
                        !imagePath.isNullOrEmpty()
                    ) {

                        val imageFile =
                            File(
                                imagePath
                            )


                        if (
                            imageFile.exists()
                        ) {

                            ivDashboardAvatar.visibility =
                                View.VISIBLE


                            tvAvatarInitial.visibility =
                                View.GONE


                            ivDashboardAvatar.imageTintList =
                                null


                            ivDashboardAvatar.scaleType =
                                ImageView.ScaleType.CENTER_CROP


                            ivDashboardAvatar.setImageURI(
                                Uri.fromFile(
                                    imageFile
                                )
                            )

                        } else {

                            ivDashboardAvatar.visibility =
                                View.GONE


                            tvAvatarInitial.visibility =
                                View.VISIBLE
                        }

                    } else {

                        ivDashboardAvatar.visibility =
                            View.GONE


                        tvAvatarInitial.visibility =
                            View.VISIBLE
                    }


                    // =============================================
                    // CURRENCY
                    // =============================================

                    userCurrency =
                        user.currency
                            .ifBlank {
                                "MMK"
                            }


                    // =============================================
                    // PASS CURRENCY TO ADAPTER
                    // =============================================

                    txnAdapter.updateCurrency(
                        userCurrency
                    )


                    // =============================================
                    // UPDATE FINANCIAL UI
                    // =============================================

                    updateFinancialUI()
                }
        }
    }


    // =============================================================
    // FORMAT CURRENCY
    // =============================================================

    private fun formatCurrency(
        amount: Double
    ): String {

        return "$userCurrency ${
            String.format(
                "%,.2f",
                amount
            )
        }"
    }


    // =============================================================
    // UPDATE FINANCIAL UI
    // =============================================================

    private fun updateFinancialUI() {

        expenseAmount.text =
            formatCurrency(
                latestExpense
            )


        incomeAmount.text =
            formatCurrency(
                latestIncome
            )


        balanceAmount.text =
            formatCurrency(
                latestBalance
            )
    }


    // =============================================================
    // FINANCIAL DATA
    // =============================================================

    private fun observeFinancialData() {

        lifecycleScope.launch {

            val expenseFlow =
                database
                    .transactionDao()
                    .getTotalExpense(
                        currentUserId
                    )


            val incomeFlow =
                database
                    .transactionDao()
                    .getTotalIncome(
                        currentUserId
                    )


            expenseFlow
                .combine(
                    incomeFlow
                ) {
                        expense,
                        income ->


                    val exp =
                        expense ?: 0.0


                    val inc =
                        income ?: 0.0


                    val balance =
                        inc - exp


                    Triple(
                        exp,
                        inc,
                        balance
                    )
                }
                .collect {
                        (exp, inc, balance) ->


                    latestExpense =
                        exp


                    latestIncome =
                        inc


                    latestBalance =
                        balance


                    updateFinancialUI()
                }
        }
    }


    // =============================================================
    // RECENT TRANSACTIONS
    // =============================================================

    private fun observeRecentTransactions() {

        lifecycleScope.launch {

            val transactionFlow =
                database
                    .transactionDao()
                    .getRecent10Transactions(
                        currentUserId
                    )


            val categoryFlow =
                database
                    .categoryDao()
                    .observeAll(
                        currentUserId
                    )


            transactionFlow
                .combine(
                    categoryFlow
                ) {
                        transactions,
                        categories ->

                    Pair(
                        transactions,
                        categories
                    )
                }
                .collect {
                        (transactions, categories) ->


                    txnAdapter
                        .updateCategories(
                            categories
                        )


                    txnAdapter
                        .updateList(
                            transactions
                        )
                }
        }
    }
}