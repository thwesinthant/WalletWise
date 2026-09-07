package com.example.walletwise.account

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walletwise.R
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.entity.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddEditAccountActivity : AppCompatActivity() {

    // =========================================================
    // USER
    // =========================================================

    private var currentUserId: Int = -1


    // =========================================================
    // ACCOUNT
    // =========================================================

    private var accountId: Int = -1

    private var existingAccount: Account? = null


    // =========================================================
    // DATABASE
    // =========================================================

    private lateinit var database: AppDatabase


    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var tvPageTitle: TextView

    private lateinit var etAccountName: EditText

    private lateinit var etOpeningBalance: EditText

    private lateinit var btnSaveAccount: TextView


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_add_edit_account
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
        // ACCOUNT ID
        // =====================================================

        accountId =
            intent.getIntExtra(
                "ACCOUNT_ID",
                -1
            )


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

        tvPageTitle =
            findViewById(
                R.id.tvPageTitle
            )

        etAccountName =
            findViewById(
                R.id.etAccountName
            )

        etOpeningBalance =
            findViewById(
                R.id.etOpeningBalance
            )

        btnSaveAccount =
            findViewById(
                R.id.btnSaveAccount
            )


        // =====================================================
        // BACK BUTTON
        // =====================================================

        findViewById<View>(
            R.id.btnBack
        ).setOnClickListener {

            finish()
        }


        // =====================================================
        // EDIT / ADD MODE
        // =====================================================

        if (accountId != -1) {

            tvPageTitle.text =
                "Edit Account"

            loadAccount()

        } else {

            tvPageTitle.text =
                "Add Account"
        }


        // =====================================================
        // SAVE
        // =====================================================

        btnSaveAccount.setOnClickListener {

            saveAccount()
        }
    }


    // =========================================================
    // LOAD ACCOUNT
    // =========================================================

    private fun loadAccount() {

        lifecycleScope.launch(
            Dispatchers.IO
        ) {

            try {

                // -------------------------------------------------
                // Get original account
                // -------------------------------------------------

                val account =
                    database
                        .accountDao()
                        .getAccountById(
                            accountId,
                            currentUserId
                        )


                if (account == null) {

                    withContext(
                        Dispatchers.Main
                    ) {

                        Toast.makeText(
                            this@AddEditAccountActivity,
                            "Account not found",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()
                    }

                    return@launch
                }


                // -------------------------------------------------
                // Get calculated current balance
                // -------------------------------------------------

                val accountBalance =
                    database
                        .accountDao()
                        .getAccountBalanceById(
                            accountId,
                            currentUserId
                        )


                if (accountBalance == null) {

                    withContext(
                        Dispatchers.Main
                    ) {

                        Toast.makeText(
                            this@AddEditAccountActivity,
                            "Unable to calculate account balance",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()
                    }

                    return@launch
                }


                // -------------------------------------------------
                // Save existing account
                // -------------------------------------------------

                existingAccount =
                    account


                // -------------------------------------------------
                // Update UI
                //
                // IMPORTANT:
                //
                // Show CURRENT BALANCE, not OPENING BALANCE.
                // -------------------------------------------------

                withContext(
                    Dispatchers.Main
                ) {

                    etAccountName.setText(
                        account.name
                    )

                    etOpeningBalance.setText(
                        formatAmount(
                            accountBalance.currentBalance
                        )
                    )
                }

            } catch (e: Exception) {

                e.printStackTrace()

                withContext(
                    Dispatchers.Main
                ) {

                    Toast.makeText(
                        this@AddEditAccountActivity,
                        "Error loading account",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    // =========================================================
    // SAVE ACCOUNT
    // =========================================================

    private fun saveAccount() {

        // =====================================================
        // GET NAME
        // =====================================================

        val name =
            etAccountName
                .text
                .toString()
                .trim()


        // =====================================================
        // GET BALANCE
        //
        // In ADD mode:
        //     this is the opening balance.
        //
        // In EDIT mode:
        //     this is the desired CURRENT balance.
        // =====================================================

        val balanceText =
            etOpeningBalance
                .text
                .toString()
                .trim()


        // =====================================================
        // VALIDATE NAME
        // =====================================================

        if (name.isEmpty()) {

            etAccountName.error =
                "Account name is required"

            etAccountName.requestFocus()

            return
        }


        // =====================================================
        // VALIDATE BALANCE
        // =====================================================

        val enteredBalance =
            balanceText.toDoubleOrNull()


        if (enteredBalance == null) {

            etOpeningBalance.error =
                "Enter a valid amount"

            etOpeningBalance.requestFocus()

            return
        }


        // =====================================================
        // CHECK MODE
        // =====================================================

        val isEditMode =
            accountId != -1


        // =====================================================
        // DATABASE
        // =====================================================

        lifecycleScope.launch(
            Dispatchers.IO
        ) {

            try {

                if (!isEditMode) {

                    // =========================================
                    // ADD ACCOUNT
                    //
                    // Entered amount is the opening balance.
                    // =========================================

                    val newAccount =
                        Account(

                            userId =
                                currentUserId,

                            name =
                                name,

                            openingBalance =
                                enteredBalance
                        )


                    database
                        .accountDao()
                        .insert(
                            newAccount
                        )

                } else {

                    // =========================================
                    // EDIT ACCOUNT
                    // =========================================

                    val accountToUpdate =
                        existingAccount


                    if (accountToUpdate == null) {

                        withContext(
                            Dispatchers.Main
                        ) {

                            Toast.makeText(
                                this@AddEditAccountActivity,
                                "Account is still loading",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        return@launch
                    }


                    // =========================================
                    // GET TRANSACTION EFFECT
                    //
                    // Example:
                    //
                    // Income       = +50,000
                    // Transfer in  = +10,000
                    // Expense      = -20,000
                    // Transfer out = -5,000
                    //
                    // Effect = 35,000
                    // =========================================

                    val transactionEffect =
                        database.accountDao().getTransactionEffect(accountId, currentUserId)


                    // =========================================
                    // CALCULATE NEW OPENING BALANCE
                    //
                    // Desired current balance
                    //     = new opening balance
                    //       + transaction effect
                    //
                    // Therefore:
                    //
                    // new opening balance
                    //     = desired current balance
                    //       - transaction effect
                    // =========================================

                    val newOpeningBalance =
                        enteredBalance -
                                transactionEffect


                    // =========================================
                    // UPDATE ACCOUNT
                    // =========================================

                    val updatedAccount =
                        accountToUpdate.copy(

                            name =
                                name,

                            openingBalance =
                                newOpeningBalance
                        )


                    database
                        .accountDao()
                        .update(
                            updatedAccount
                        )
                }


                // =================================================
                // SUCCESS
                // =================================================

                withContext(
                    Dispatchers.Main
                ) {

                    val message =
                        if (isEditMode) {
                            "Account updated"
                        } else {
                            "Account added"
                        }


                    Toast.makeText(
                        this@AddEditAccountActivity,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()


                    finish()
                }

            } catch (e: Exception) {

                e.printStackTrace()

                withContext(
                    Dispatchers.Main
                ) {

                    Toast.makeText(
                        this@AddEditAccountActivity,
                        "Error saving account",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    // =========================================================
    // FORMAT AMOUNT
    // =========================================================

    private fun formatAmount(
        amount: Double
    ): String {

        return String.format(
            java.util.Locale.US,
            "%.2f",
            amount
        )
    }
}