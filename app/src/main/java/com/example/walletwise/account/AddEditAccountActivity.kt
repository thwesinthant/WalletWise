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

        super.onCreate(
            savedInstanceState
        )

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
            findViewById<TextView>(
                R.id.tvPageTitle
            )

        etAccountName =
            findViewById<EditText>(
                R.id.etAccountName
            )

        etOpeningBalance =
            findViewById<EditText>(
                R.id.etOpeningBalance
            )

        btnSaveAccount =
            findViewById<TextView>(
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
        // EDIT MODE
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
        // SAVE BUTTON
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

                val account =
                    database
                        .accountDao()
                        .getAccountById(
                            accountId,
                            currentUserId
                        )


                withContext(
                    Dispatchers.Main
                ) {

                    if (account == null) {

                        Toast.makeText(
                            this@AddEditAccountActivity,
                            "Account not found",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()

                        return@withContext
                    }


                    // =================================================
                    // SAVE EXISTING ACCOUNT
                    // =================================================

                    existingAccount =
                        account


                    // =================================================
                    // SHOW DATA
                    // =================================================

                    etAccountName.setText(
                        account.name
                    )

                    etOpeningBalance.setText(
                        account.openingBalance.toString()
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
        // GET INPUT
        // =====================================================

        val name =
            etAccountName
                .text
                .toString()
                .trim()


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

        val openingBalance =
            balanceText.toDoubleOrNull()


        if (openingBalance == null) {

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
        // DATABASE OPERATION
        // =====================================================

        lifecycleScope.launch(
            Dispatchers.IO
        ) {

            try {

                if (!isEditMode) {

                    // =========================================
                    // ADD ACCOUNT
                    // =========================================

                    val newAccount =
                        Account(
                            userId =
                                currentUserId,

                            name =
                                name,

                            openingBalance =
                                openingBalance
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


                    val updatedAccount =
                        accountToUpdate.copy(

                            name =
                                name,

                            openingBalance =
                                openingBalance
                        )


                    database
                        .accountDao()
                        .update(
                            updatedAccount
                        )
                }


                // =============================================
                // SUCCESS
                // =============================================

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
}

