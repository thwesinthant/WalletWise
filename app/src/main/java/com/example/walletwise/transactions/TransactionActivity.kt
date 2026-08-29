package com.example.walletwise.transactions

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.walletwise.R
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.entity.Transaction
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class TransactionActivity : AppCompatActivity() {

// =============================================================
// USER
// =============================================================

    private var currentUserId: Int = -1

    private var userCurrency: String = "MMK"


// =============================================================
// DATABASE
// =============================================================

    private lateinit var database: AppDatabase


// =============================================================
// ADAPTER
// =============================================================

    private lateinit var transactionAdapter: TransactionAdapter


// =============================================================
// DATA
// =============================================================

    private var allTransactions:
            List<Transaction> =
        emptyList()


// =============================================================
// FILTER
// =============================================================

    private enum class FilterType {

        ALL,

        INCOME,

        EXPENSE
    }


    private var currentFilter =
        FilterType.ALL


// =============================================================
// SORT
// =============================================================

    private var newestFirst =
        true


// =============================================================
// VIEWS
// =============================================================

    private lateinit var rvAllTransactions:
            RecyclerView

    private lateinit var emptyState:
            View

    private lateinit var filterAll:
            TextView

    private lateinit var filterIncome:
            TextView

    private lateinit var filterExpense:
            TextView


// =============================================================
// ACTIVITY CREATE
// =============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )



        setContentView(
            R.layout.activity_all_transactions
        )

// =========================================================
// SYSTEM STATUS BAR + NAVIGATION BAR SAFE SPACE
// =========================================================

        val rootView =
            findViewById<View>(
                R.id.transactionRoot
            )


        ViewCompat.setOnApplyWindowInsetsListener(
            rootView
        ) { view, insets ->


            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )


            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )


            insets
        }


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

        initializeViews()


        // =========================================================
        // RECYCLER VIEW
        // =========================================================

        setupRecyclerView()


        // =========================================================
        // CLICKS
        // =========================================================

        setupClicks()


        // =========================================================
        // OBSERVERS
        // =========================================================

        observeUser()

        observeTransactions()
    }


// =============================================================
// INITIALIZE VIEWS
// =============================================================

    private fun initializeViews() {

        rvAllTransactions =
            findViewById(
                R.id.rvAllTransactions
            )


        emptyState =
            findViewById(
                R.id.emptyState
            )


        filterAll =
            findViewById(
                R.id.filterAll
            )


        filterIncome =
            findViewById(
                R.id.filterIncome
            )


        filterExpense =
            findViewById(
                R.id.filterExpense
            )
    }


// =============================================================
// RECYCLER VIEW
// =============================================================

    private fun setupRecyclerView() {

        rvAllTransactions.layoutManager =
            LinearLayoutManager(
                this
            )


        transactionAdapter =
            TransactionAdapter(
                emptyList(),
                userCurrency
            )


        rvAllTransactions.adapter =
            transactionAdapter
    }


// =============================================================
// CLICKS
// =============================================================

    private fun setupClicks() {


        // ---------------------------------------------------------
        // BACK
        // ---------------------------------------------------------

        findViewById<View>(
            R.id.btnBack
        ).setOnClickListener {

            finish()
        }


        // ---------------------------------------------------------
        // ALL
        // ---------------------------------------------------------

        filterAll.setOnClickListener {

            currentFilter =
                FilterType.ALL


            updateFilterSelection()

            applyFilterAndSort()
        }


        // ---------------------------------------------------------
        // INCOME
        // ---------------------------------------------------------

        filterIncome.setOnClickListener {

            currentFilter =
                FilterType.INCOME


            updateFilterSelection()

            applyFilterAndSort()
        }


        // ---------------------------------------------------------
        // EXPENSE
        // ---------------------------------------------------------

        filterExpense.setOnClickListener {

            currentFilter =
                FilterType.EXPENSE


            updateFilterSelection()

            applyFilterAndSort()
        }


        // ---------------------------------------------------------
        // SORT
        // ---------------------------------------------------------

        findViewById<TextView>(
            R.id.btnFilterSort
        ).setOnClickListener {

            newestFirst =
                !newestFirst


            applyFilterAndSort()
        }


        // ---------------------------------------------------------
        // ADD TRANSACTION
        // ---------------------------------------------------------

        findViewById<TextView>(
            R.id.fabAddTransaction
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


                    userCurrency =
                        user.currency
                            .ifBlank {
                                "MMK"
                            }


                    transactionAdapter
                        .updateCurrency(
                            userCurrency
                        )
                }
        }
    }


// =============================================================
// OBSERVE TRANSACTIONS
// =============================================================

    private fun observeTransactions() {

        lifecycleScope.launch {

            val transactionFlow =
                database
                    .transactionDao()
                    .getAllTransactions(
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


                    allTransactions =
                        transactions


                    transactionAdapter
                        .updateCategories(
                            categories
                        )


                    applyFilterAndSort()
                }
        }
    }


// =============================================================
// FILTER AND SORT
// =============================================================

    private fun applyFilterAndSort() {


        // ---------------------------------------------------------
        // FILTER
        // ---------------------------------------------------------

        val filteredList =
            when (
                currentFilter
            ) {

                FilterType.ALL ->
                    allTransactions


                FilterType.INCOME ->
                    allTransactions.filter {

                        it.type ==
                                "INCOME"
                    }


                FilterType.EXPENSE ->
                    allTransactions.filter {

                        it.type ==
                                "EXPENSE"
                    }
            }


        // ---------------------------------------------------------
        // SORT
        // ---------------------------------------------------------

        val sortedList =
            if (
                newestFirst
            ) {

                filteredList.sortedByDescending {

                    it.createdAt
                }

            } else {

                filteredList.sortedBy {

                    it.createdAt
                }
            }


        // ---------------------------------------------------------
        // ADAPTER
        // ---------------------------------------------------------

        transactionAdapter
            .updateList(
                sortedList
            )


        // ---------------------------------------------------------
        // EMPTY STATE
        // ---------------------------------------------------------

        if (
            sortedList.isEmpty()
        ) {

            emptyState.visibility =
                View.VISIBLE


            rvAllTransactions.visibility =
                View.GONE

        } else {

            emptyState.visibility =
                View.GONE


            rvAllTransactions.visibility =
                View.VISIBLE
        }
    }


// =============================================================
// FILTER UI
// =============================================================

    private fun updateFilterSelection() {

        // =========================================================
        // ALL
        // =========================================================

        if (
            currentFilter ==
            FilterType.ALL
        ) {

            filterAll.setBackgroundResource(
                R.drawable.bg_filter_selected
            )

            filterAll.setTextColor(
                getColor(
                    R.color.neutral_0
                )
            )

        } else {

            filterAll.setBackgroundResource(
                R.drawable.bg_filter_unselected
            )

            filterAll.setTextColor(
                getColor(
                    R.color.neutral_500
                )
            )
        }


        // =========================================================
        // INCOME
        // =========================================================

        if (
            currentFilter ==
            FilterType.INCOME
        ) {

            filterIncome.setBackgroundResource(
                R.drawable.bg_filter_income
            )

            filterIncome.setTextColor(
                getColor(
                    R.color.neutral_0
                )
            )

        } else {

            filterIncome.setBackgroundResource(
                R.drawable.bg_filter_unselected
            )

            filterIncome.setTextColor(
                getColor(
                    R.color.neutral_500
                )
            )
        }


        // =========================================================
        // EXPENSE
        // =========================================================

        if (
            currentFilter ==
            FilterType.EXPENSE
        ) {

            filterExpense.setBackgroundResource(
                R.drawable.bg_filter_expense
            )

            filterExpense.setTextColor(
                getColor(
                    R.color.neutral_0
                )
            )

        } else {

            filterExpense.setBackgroundResource(
                R.drawable.bg_filter_unselected
            )

            filterExpense.setTextColor(
                getColor(
                    R.color.neutral_500
                )
            )
        }
    }


}
