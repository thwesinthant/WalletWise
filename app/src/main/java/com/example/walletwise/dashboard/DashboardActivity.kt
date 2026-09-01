package com.example.walletwise.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.walletwise.R
import com.example.walletwise.account.AccountActivity
import com.example.walletwise.account.AccountAdapter
import com.example.walletwise.account.AddEditAccountActivity
import com.example.walletwise.account.DashboardAccountAdapter
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.entity.Transaction
import com.example.walletwise.notification.NotificationActivity
import com.example.walletwise.profile.ProfileActivity
import com.example.walletwise.transactions.AddTransactionActivity
import com.example.walletwise.transactions.TransactionActivity
import com.example.walletwise.transactions.TransactionAdapter
import com.example.walletwise.util.BottomNavHelper
import com.example.walletwise.util.NavTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    // ADAPTERS
    // =============================================================

    private lateinit var txnAdapter: TransactionAdapter

    private lateinit var accountAdapter: DashboardAccountAdapter


    // =============================================================
    // VIEWS
    // =============================================================

    private lateinit var greetName: TextView

    private lateinit var tvAvatarInitial: TextView

    private lateinit var ivDashboardAvatar: ImageView

    private lateinit var balanceAmount: TextView

    private lateinit var balanceUpdatedDate: TextView

    private lateinit var expenseAmount: TextView

    private lateinit var incomeAmount: TextView

    private lateinit var rvDashboardTransactions: RecyclerView

    private lateinit var rvDashboardAccounts: RecyclerView

    private lateinit var tvAccountSummary: TextView

    private lateinit var emptyDashboardAccounts: View

    private lateinit var emptyBalanceState: View


    // =============================================================
    // ON CREATE
    // =============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_dashboard
        )


        // =========================================================
        // USER ID
        // =========================================================

        currentUserId =
            intent.getIntExtra(
                "USER_ID",
                -1
            )


        if (currentUserId == -1) {

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
        // FIND VIEWS
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

        balanceUpdatedDate =
            findViewById(
                R.id.balanceUpdatedDate
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

        rvDashboardAccounts =
            findViewById(
                R.id.rvDashboardAccounts
            )

        tvAccountSummary =
            findViewById(
                R.id.tvAccountSummary
            )

        emptyDashboardAccounts =
            findViewById(
                R.id.emptyDashboardAccounts
            )

        emptyBalanceState =
            findViewById(
                R.id.emptyBalanceState
            )

        emptyDashboardAccounts.setOnClickListener {

            openAccounts()
        }


        emptyBalanceState.setOnClickListener {

            openAccounts()
        }

        // =========================================================
        // BOTTOM NAV
        // =========================================================

        BottomNavHelper.setup(
            activity = this,
            root = findViewById(
                android.R.id.content
            ),
            current = NavTab.HOME,
            userId = currentUserId
        )


        // =========================================================
        // TRANSACTIONS RECYCLER VIEW
        // =========================================================

        rvDashboardTransactions.layoutManager =
            LinearLayoutManager(this)


        // =========================================================
        // ACCOUNTS RECYCLER VIEW
        // =========================================================

        rvDashboardAccounts.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )


        // =========================================================
        // ACCOUNT ADAPTER
        // =========================================================

        accountAdapter =
            DashboardAccountAdapter(
                accounts = emptyList(),
                currency = userCurrency,

                onAddAccountClick = {

                    openAccounts()
                }
            )


        rvDashboardAccounts.adapter =
            accountAdapter


        // =========================================================
        // TRANSACTION ADAPTER
        // =========================================================

        txnAdapter =
            TransactionAdapter(
                rawList = emptyList(),
                currency = userCurrency,

                onEditClick = { transaction ->

                    val intent =
                        Intent(
                            this,
                            AddTransactionActivity::class.java
                        )

                    intent.putExtra(
                        "USER_ID",
                        currentUserId
                    )

                    intent.putExtra(
                        "TRANSACTION_ID",
                        transaction.transactionId
                    )

                    startActivity(intent)
                },

                onDeleteClick = { transaction ->

                    showDeleteConfirmation(
                        transaction
                    )
                }
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

        observeAccounts()

        observeRecentTransactions()

        observeBalanceUpdatedDate()
    }

    // =============================================================
// OBSERVE BALANCE UPDATED DATE
// =============================================================

    private fun observeBalanceUpdatedDate() {

        lifecycleScope.launch {

            database
                .transactionDao()
                .observeLatestTransactionDate(
                    currentUserId
                )
                .collect { latestDate ->

                    updateBalanceUpdatedDate(
                        latestDate
                    )
                }
        }
    }

    // =============================================================
    // OBSERVE ACCOUNTS
    // =============================================================

    private fun observeAccounts() {

        lifecycleScope.launch {

            database
                .accountDao()
                .getAccountBalances(
                    currentUserId
                )
                .collect { accounts ->


                    // =============================================
                    // UPDATE ACCOUNT LIST
                    // =============================================

                    accountAdapter.updateList(
                        accounts
                    )

                    accountAdapter.updateCurrency(
                        userCurrency
                    )


                    rvDashboardAccounts.visibility =
                        View.VISIBLE

                    emptyDashboardAccounts.visibility =
                        View.GONE


                    // =============================================
                    // NO ACCOUNTS
                    // =============================================

                    if (accounts.isEmpty()) {

                        latestBalance =
                            0.0


                        balanceAmount.visibility =
                            View.GONE

                        emptyBalanceState.visibility =
                            View.VISIBLE


                        tvAccountSummary.text =
                            "No accounts yet"

                        return@collect
                    }


                    // =============================================
                    // CALCULATE TOTAL WALLET BALANCE
                    // =============================================

                    val totalWalletBalance =
                        accounts.sumOf {
                            it.currentBalance
                        }


                    // =============================================
                    // SAVE TOTAL BALANCE
                    // =============================================

                    latestBalance =
                        totalWalletBalance


                    // =============================================
                    // SHOW BALANCE
                    // =============================================

                    balanceAmount.visibility =
                        View.VISIBLE

                    emptyBalanceState.visibility =
                        View.GONE


                    updateWalletBalanceUI()


                    // =============================================
                    // ACCOUNT SUMMARY
                    // =============================================

                    tvAccountSummary.text =
                        "${accounts.size} accounts · " +
                                formatCurrency(
                                    totalWalletBalance
                                )
                }
        }
    }

    private fun updateBalanceUpdatedDate(
        lastUpdated: Long?
    ) {

        if (lastUpdated == null) {

            balanceUpdatedDate.text =
                "No updates yet"

            return
        }

        val dateFormat =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

        balanceUpdatedDate.text =
            "Updated ${
                dateFormat.format(
                    Date(lastUpdated)
                )
            }"
    }

    // =============================================================
    // DELETE TRANSACTION
    // =============================================================

    private fun deleteTransaction(
        transaction: Transaction
    ) {

        lifecycleScope.launch(
            Dispatchers.IO
        ) {

            try {

                database
                    .transactionDao()
                    .deleteTransaction(
                        transactionId =
                            transaction.transactionId,

                        userId =
                            currentUserId
                    )

            } catch (e: Exception) {

                e.printStackTrace()

                withContext(
                    Dispatchers.Main
                ) {

                    android.widget.Toast.makeText(
                        this@DashboardActivity,
                        "Error deleting transaction: ${e.localizedMessage}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    // =============================================================
    // DELETE CONFIRMATION
    // =============================================================

    private fun showDeleteConfirmation(
        transaction: Transaction
    ) {

        AlertDialog.Builder(this)

            .setTitle(
                "Delete Transaction"
            )

            .setMessage(
                "Are you sure you want to delete this transaction?"
            )

            .setNegativeButton(
                "Cancel",
                null
            )

            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                deleteTransaction(
                    transaction
                )
            }

            .show()
    }


    private fun openAccounts() {

        val intent =
            Intent(
                this,
                AddEditAccountActivity::class.java
            )

        intent.putExtra(
            "USER_ID",
            currentUserId
        )

        startActivity(
            intent
        )
    }

    // =============================================================
    // NAVIGATION
    // =============================================================


    private fun setupNavigation() {


        // ---------------------------------------------------------
        // ACCOUNTS - SEE ALL
        // ---------------------------------------------------------

        findViewById<TextView>(
            R.id.btnViewAllAccounts
        ).setOnClickListener {

            val intent =
                Intent(
                    this@DashboardActivity,
                    AccountActivity::class.java
                )

            intent.putExtra(
                "USER_ID",
                currentUserId
            )

            startActivity(intent)
        }


        // ---------------------------------------------------------
        // NOTIFICATION
        // ---------------------------------------------------------

        findViewById<ImageView>(
            R.id.ivNoti
        ).setOnClickListener {

            val intent =
                Intent(
                    this@DashboardActivity,
                    NotificationActivity::class.java
                )

            intent.putExtra(
                "USER_ID",
                currentUserId
            )

            startActivity(intent)
        }


        // ---------------------------------------------------------
        // ADD TRANSACTION
        // ---------------------------------------------------------

        findViewById<TextView>(
            R.id.fabAdd
        ).setOnClickListener {

            val intent =
                Intent(
                    this@DashboardActivity,
                    AddTransactionActivity::class.java
                )

            intent.putExtra(
                "USER_ID",
                currentUserId
            )

            startActivity(intent)
        }


        // ---------------------------------------------------------
        // VIEW ALL TRANSACTIONS
        // ---------------------------------------------------------

        findViewById<TextView>(
            R.id.btnViewAllExpenses
        ).setOnClickListener {

            val intent =
                Intent(
                    this@DashboardActivity,
                    TransactionActivity::class.java
                )

            intent.putExtra(
                "USER_ID",
                currentUserId
            )

            startActivity(intent)
        }


        // ---------------------------------------------------------
        // PROFILE
        // ---------------------------------------------------------

        findViewById<View>(
            R.id.avatarContainer
        ).setOnClickListener {

            val intent =
                Intent(
                    this@DashboardActivity,
                    ProfileActivity::class.java
                )

            intent.putExtra(
                "USER_ID",
                currentUserId
            )

            startActivity(intent)
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


                    // -------------------------------------------------
                    // GREETING
                    // -------------------------------------------------

                    greetName.text =
                        "Hello, ${user.fullName}"


                    // -------------------------------------------------
                    // AVATAR INITIAL
                    // -------------------------------------------------

                    if (
                        user.fullName.isNotEmpty()
                    ) {

                        tvAvatarInitial.text =
                            user.fullName
                                .first()
                                .uppercase()
                    }


                    // -------------------------------------------------
                    // PROFILE IMAGE
                    // -------------------------------------------------

                    val imagePath =
                        user.profileImage


                    if (
                        !imagePath.isNullOrEmpty()
                    ) {

                        val imageFile =
                            File(imagePath)


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


                    // -------------------------------------------------
                    // CURRENCY
                    // -------------------------------------------------

                    userCurrency =
                        user.currency.ifBlank {
                            "MMK"
                        }


                    // -------------------------------------------------
                    // UPDATE ADAPTER CURRENCY
                    // -------------------------------------------------

                    txnAdapter.updateCurrency(
                        userCurrency
                    )

                    accountAdapter.updateCurrency(
                        userCurrency
                    )


                    // -------------------------------------------------
                    // UPDATE FINANCIAL UI
                    // -------------------------------------------------

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
    }

    private fun updateWalletBalanceUI() {

        balanceAmount.text =
            formatCurrency(
                latestBalance
            )
    }


    // =============================================================
    // OBSERVE FINANCIAL DATA
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
                ) { expense, income ->

                    val exp =
                        expense ?: 0.0

                    val inc =
                        income ?: 0.0


                    Pair(
                        exp,
                        inc
                    )
                }
                .collect { (exp, inc) ->


                    // =============================================
                    // SAVE TOTAL EXPENSE
                    // =============================================

                    latestExpense =
                        exp


                    // =============================================
                    // SAVE TOTAL INCOME
                    // =============================================

                    latestIncome =
                        inc


                    // =============================================
                    // DO NOT CALCULATE BALANCE HERE
                    //
                    // Wallet balance comes from Account balances.
                    // =============================================

                    updateFinancialUI()
                }
        }
    }


    // =============================================================
    // OBSERVE RECENT TRANSACTIONS
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
                ) { transactions, categories ->

                    Pair(
                        transactions,
                        categories
                    )

                }
                .collect { (transactions, categories) ->

                    txnAdapter.updateCategories(
                        categories
                    )

                    txnAdapter.updateList(
                        transactions
                    )
                }
        }
    }
}