package com.example.walletwise.account

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.walletwise.R
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.entity.AccountBalance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountActivity : AppCompatActivity() {

    // =========================================================
    // USER
    // =========================================================

    private var currentUserId: Int = -1

    private var userCurrency: String = "MMK"


    // =========================================================
    // DATABASE
    // =========================================================

    private lateinit var database: AppDatabase


    // =========================================================
    // ADAPTER
    // =========================================================

    private lateinit var accountAdapter: AccountAdapter


    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var rvAccounts: RecyclerView

    private lateinit var tvAccountSummary: TextView

    private lateinit var emptyAccountState: LinearLayout


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_account
        )


        // =====================================================
        // USER ID
        // =====================================================

        currentUserId =
            intent.getIntExtra(
                "USER_ID",
                -1
            )


        if (currentUserId == -1) {

            Toast.makeText(
                this,
                "User ID not found",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }


        // =====================================================
        // DATABASE
        // =====================================================

        database =
            AppDatabase.getDatabase(
                applicationContext
            )


        // =====================================================
        // FIND VIEWS
        // =====================================================

        rvAccounts =
            findViewById(
                R.id.rvAccounts
            )

        tvAccountSummary =
            findViewById(
                R.id.tvAccountSummary
            )

        emptyAccountState =
            findViewById(
                R.id.emptyAccountState
            )


        // =====================================================
        // RECYCLER VIEW
        // =====================================================

        rvAccounts.layoutManager =
            LinearLayoutManager(
                this
            )


        // =====================================================
        // ADAPTER
        // =====================================================

        accountAdapter =
            AccountAdapter(
                accounts = emptyList(),
                currency = userCurrency,

                onMenuClick = { account ->

                    showAccountMenu(
                        account
                    )
                }
            )


        rvAccounts.adapter =
            accountAdapter


        // =====================================================
        // BACK
        // =====================================================

        findViewById<View>(
            R.id.btnBack
        ).setOnClickListener {

            finish()
        }


        // =====================================================
        // ADD ACCOUNT
        // =====================================================

        findViewById<TextView>(
            R.id.btnAddAccount
        ).setOnClickListener {

            openAddAccount()
        }



        loadUserCurrency()
        observeAccounts()
    }

    // =========================================================
// LOAD USER CURRENCY
// =========================================================

    private fun loadUserCurrency() {

        lifecycleScope.launch {

            database
                .userDao()
                .getUserById(
                    currentUserId
                )
                .collect { user ->

                    user ?: return@collect


                    userCurrency =
                        user.currency.ifBlank {
                            "MMK"
                        }


                    accountAdapter.updateCurrency(
                        userCurrency
                    )
                }
        }
    }

    // =========================================================
    // ADD ACCOUNT
    // =========================================================

    private fun openAddAccount() {

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


    // =========================================================
    // EDIT ACCOUNT
    // =========================================================

    private fun openEditAccount(
        accountId: Int
    ) {

        val intent =
            Intent(
                this,
                AddEditAccountActivity::class.java
            )


        intent.putExtra(
            "USER_ID",
            currentUserId
        )


        intent.putExtra(
            "ACCOUNT_ID",
            accountId
        )


        startActivity(
            intent
        )
    }


    // =========================================================
    // OBSERVE ACCOUNTS
    // =========================================================

    private fun observeAccounts() {

        lifecycleScope.launch {

            database
                .accountDao()
                .getAccountBalances(
                    currentUserId
                )
                .collect { accounts ->

                    updateAccountUI(
                        accounts
                    )
                }
        }
    }


    // =========================================================
    // UPDATE UI
    // =========================================================

    private fun updateAccountUI(
        accounts: List<AccountBalance>
    ) {

        accountAdapter.updateCurrency(
            userCurrency
        )


        accountAdapter.updateList(
            accounts
        )


        val totalBalance =
            accounts.sumOf {
                it.currentBalance
            }


        tvAccountSummary.text =
            "${accounts.size} accounts · " +
                    "$userCurrency " +
                    String.format(
                        "%,.2f",
                        totalBalance
                    )


        if (accounts.isEmpty()) {

            rvAccounts.visibility =
                View.GONE

            emptyAccountState.visibility =
                View.VISIBLE

        } else {

            rvAccounts.visibility =
                View.VISIBLE

            emptyAccountState.visibility =
                View.GONE
        }
    }


    // =========================================================
    // ACCOUNT MENU
    // =========================================================

    private fun showAccountMenu(
        account: AccountBalance
    ) {

        AlertDialog.Builder(
            this
        )
            .setTitle(
                account.name
            )
            .setItems(
                arrayOf(
                    "Edit",
                    "Delete"
                )
            ) { _, which ->

                when (which) {

                    // EDIT

                    0 -> {

                        openEditAccount(
                            account.accountId
                        )
                    }


                    // DELETE

                    1 -> {

                        showDeleteConfirmation(
                            account
                        )
                    }
                }
            }
            .show()
    }


    // =========================================================
    // DELETE CONFIRMATION
    // =========================================================

    private fun showDeleteConfirmation(
        account: AccountBalance
    ) {

        val balance =
            account.currentBalance


        // =========================================================
        // CHECK ACCOUNT BALANCE
        // =========================================================

        if (kotlin.math.abs(balance) > 0.000001) {

            AlertDialog.Builder(
                this
            )
                .setTitle(
                    "Cannot Delete Account"
                )
                .setMessage(
                    "${account.name} still has a balance of " +
                            "$userCurrency ${
                                String.format(
                                    "%,.2f",
                                    balance
                                )
                            }.\n\n" +
                            "Please transfer or withdraw the remaining " +
                            "balance before deleting this account."
                )
                .setPositiveButton(
                    "OK",
                    null
                )
                .show()

            return
        }


        // =========================================================
        // BALANCE IS ZERO
        // ALLOW DELETE
        // =========================================================

        AlertDialog.Builder(
            this
        )
            .setTitle(
                "Delete Account"
            )
            .setMessage(
                "Are you sure you want to delete ${account.name}?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                deleteAccount(
                    account
                )
            }
            .show()
    }


    // =========================================================
    // DELETE ACCOUNT
    // =========================================================

    private fun deleteAccount(
        account: AccountBalance
    ) {

        lifecycleScope.launch(
            Dispatchers.IO
        ) {

            try {

                // =================================================
                // GET LATEST ACCOUNT BALANCE
                // =================================================

                val latestAccountBalance =
                    database
                        .accountDao()
                        .getAccountBalanceById(
                            accountId =
                                account.accountId,

                            userId =
                                currentUserId
                        )


                if (latestAccountBalance == null) {

                    withContext(
                        Dispatchers.Main
                    ) {

                        Toast.makeText(
                            this@AccountActivity,
                            "Account not found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    return@launch
                }


                // =================================================
                // SAFETY CHECK
                // =================================================

                if (
                    kotlin.math.abs(
                        latestAccountBalance.currentBalance
                    ) > 0.000001
                ) {

                    withContext(
                        Dispatchers.Main
                    ) {

                        AlertDialog.Builder(
                            this@AccountActivity
                        )
                            .setTitle(
                                "Cannot Delete Account"
                            )
                            .setMessage(
                                "${latestAccountBalance.name} still has a balance of " +
                                        "$userCurrency ${
                                            String.format(
                                                "%,.2f",
                                                latestAccountBalance.currentBalance
                                            )
                                        }.\n\n" +
                                        "Please transfer or withdraw the remaining " +
                                        "balance before deleting this account."
                            )
                            .setPositiveButton(
                                "OK",
                                null
                            )
                            .show()
                    }

                    return@launch
                }


                // =================================================
                // GET ACCOUNT ENTITY
                // =================================================

                val accountEntity =
                    database
                        .accountDao()
                        .getAccountById(
                            account.accountId,
                            currentUserId
                        )


                // =================================================
                // DELETE
                // =================================================

                if (accountEntity != null) {

                    database
                        .accountDao()
                        .delete(
                            accountEntity
                        )


                    withContext(
                        Dispatchers.Main
                    ) {

                        Toast.makeText(
                            this@AccountActivity,
                            "Account deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {

                e.printStackTrace()


                withContext(
                    Dispatchers.Main
                ) {

                    Toast.makeText(
                        this@AccountActivity,
                        "Error deleting account",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}