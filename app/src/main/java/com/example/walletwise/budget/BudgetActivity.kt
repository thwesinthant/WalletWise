package com.example.walletwise.budget

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope

import com.example.walletwise.R
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.entity.Budget
import com.example.walletwise.entity.CategoryEntity
import com.example.walletwise.util.BottomNavHelper
import com.example.walletwise.util.NavTab

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import java.text.NumberFormat
import java.util.Date
import java.util.Locale


class BudgetActivity : AppCompatActivity() {


    // ============================================================
    // DATABASE
    // ============================================================

    private lateinit var database: AppDatabase


    // ============================================================
    // BUDGET LIST
    // ============================================================

    private lateinit var btnAddBudget: TextView

    private lateinit var budgetListContainer: LinearLayout


    // ============================================================
    // MONTH SUMMARY
    // ============================================================

    private lateinit var tvTotalBudget: TextView

    private lateinit var tvBudgetSpent: TextView

    private lateinit var monthBudgetProgress: ProgressBar

    private lateinit var tvProgressPercent: TextView

    private lateinit var tvMonthRemaining: TextView


    // ============================================================
    // USER
    // ============================================================

    private var userId: Int = -1


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
            R.layout.activity_budgets
        )


        // ========================================================
        // USER ID
        // ========================================================

        userId =
            intent.getIntExtra(
                "USER_ID",
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
        // BOTTOM NAVIGATION
        // ========================================================

        BottomNavHelper.setup(
            activity = this,
            root = findViewById(
                android.R.id.content
            ),
            current = NavTab.HOME,
            userId = userId
        )


        // ========================================================
        // INITIALIZE
        // ========================================================

        initializeViews()

        setupAddBudgetButton()

        observeBudgets()
    }


    // ============================================================
    // INITIALIZE VIEWS
    // ============================================================

    private fun initializeViews() {

        btnAddBudget =
            findViewById(
                R.id.btnAddBudget
            )


        budgetListContainer =
            findViewById(
                R.id.budgetListContainer
            )


        tvTotalBudget =
            findViewById(
                R.id.tvTotalBudget
            )


        tvBudgetSpent =
            findViewById(
                R.id.tvBudgetSpent
            )


        monthBudgetProgress =
            findViewById(
                R.id.monthBudgetProgress
            )


        tvProgressPercent =
            findViewById(
                R.id.tvProgressPercent
            )


        tvMonthRemaining =
            findViewById(
                R.id.tvMonthRemaining
            )
    }


    // ============================================================
    // ADD BUDGET
    // ============================================================

    private fun setupAddBudgetButton() {

        btnAddBudget.setOnClickListener {

            val intent =
                Intent(
                    this,
                    AddBudgetActivity::class.java
                )


            intent.putExtra(
                AddBudgetActivity.EXTRA_USER_ID,
                userId
            )


            startActivity(
                intent
            )
        }
    }


    // ============================================================
    // OBSERVE BUDGETS
    // ============================================================

    private fun observeBudgets() {

        lifecycleScope.launch {

            database
                .budgetDao()
                .getBudgetsByUser(
                    userId
                )
                .collectLatest { budgets ->


                    displayBudgets(
                        budgets
                    )


                    updateBudgetSummary(
                        budgets
                    )
                }
        }
    }


    // ============================================================
    // DISPLAY BUDGETS
    // ============================================================

    private suspend fun displayBudgets(
        budgets: List<Budget>
    ) {

        budgetListContainer.removeAllViews()


        // ========================================================
        // EMPTY STATE
        // ========================================================

        if (budgets.isEmpty()) {

            val emptyView =
                TextView(
                    this
                )


            emptyView.text =
                "No budgets yet.\nTap + to create your first budget."


            emptyView.textSize =
                14f


            emptyView.setPadding(
                16,
                30,
                16,
                30
            )


            emptyView.gravity =
                Gravity.CENTER


            budgetListContainer.addView(
                emptyView
            )


            return
        }


        // ========================================================
        // GET USER CATEGORIES
        // ========================================================

        val userCategories =
            database
                .categoryDao()
                .observeAll(
                    userId
                )
                .first()


        // ========================================================
        // CREATE BUDGET CARDS
        // ========================================================

        budgets.forEach { budget ->


            val budgetView =
                LayoutInflater
                    .from(
                        this
                    )
                    .inflate(
                        R.layout.item_budget,
                        budgetListContainer,
                        false
                    )


            budgetListContainer.addView(
                budgetView
            )


            loadAndBindBudget(
                view = budgetView,
                budget = budget,
                userCategories = userCategories
            )
        }
    }


    // ============================================================
    // LOAD BUDGET SPENDING
    // ============================================================

    private fun loadAndBindBudget(
        view: View,
        budget: Budget,
        userCategories: List<CategoryEntity>
    ) {

        lifecycleScope.launch {

            val spent =
                database
                    .transactionDao()
                    .getBudgetExpense(
                        userId = userId,
                        startDate = budget.startDate,
                        endDate = budget.endDate
                    )

            bindBudgetCard(
                view = view,
                budget = budget,
                spent = spent,
                userCategories = userCategories
            )
        }
    }


    // ============================================================
    // BIND BUDGET CARD
    // ============================================================

    private suspend fun bindBudgetCard(
        view: View,
        budget: Budget,
        spent: Double,
        userCategories: List<CategoryEntity>
    ) {


        // ========================================================
        // FIND VIEWS
        // ========================================================

        val tvBudgetName =
            view.findViewById<TextView>(
                R.id.tvBudgetName
            )


        val tvBudgetDate =
            view.findViewById<TextView>(
                R.id.tvBudgetDate
            )


        val tvBudgetStatus =
            view.findViewById<TextView>(
                R.id.tvBudgetStatus
            )


        val tvBudgetAmount =
            view.findViewById<TextView>(
                R.id.tvBudgetAmount
            )


        val budgetProgress =
            view.findViewById<ProgressBar>(
                R.id.budgetProgress
            )


        val tvSpent =
            view.findViewById<TextView>(
                R.id.tvSpent
            )


        val tvBudgetRemaining =
            view.findViewById<TextView>(
                R.id.tvBudgetRemaining
            )


        val btnToggleCategories =
            view.findViewById<TextView>(
                R.id.btnToggleCategories
            )


        val categorySummary =
            view.findViewById<LinearLayout>(
                R.id.categorySummary
            )


        val btnEditBudget =
            view.findViewById<TextView>(
                R.id.btnEditBudget
            )


        val btnDeleteBudget =
            view.findViewById<TextView>(
                R.id.btnDeleteBudget
            )


        // ========================================================
        // BASIC INFORMATION
        // ========================================================

        tvBudgetName.text =
            budget.name


        tvBudgetAmount.text =
            formatMoney(
                budget.amount
            )


        tvBudgetDate.text =
            formatBudgetDate(
                budget.startDate,
                budget.endDate
            )


        tvBudgetStatus.text =
            getBudgetStatus(
                budget
            )


        // ========================================================
        // SPENDING
        // ========================================================

        val safeSpent =
            spent.coerceAtLeast(
                0.0
            )


        val remaining =
            (
                    budget.amount -
                            safeSpent
                    )
                .coerceAtLeast(
                    0.0
                )


        val progress =
            if (
                budget.amount > 0
            ) {

                (
                        safeSpent /
                                budget.amount *
                                100
                        )
                    .toInt()
                    .coerceIn(
                        0,
                        100
                    )

            } else {

                0
            }


        budgetProgress.progress =
            progress


        tvSpent.text =
            "Spent ${
                formatMoney(
                    safeSpent
                )
            }"


        tvBudgetRemaining.text =
            "${
                formatMoney(
                    remaining
                )
            } left"


        // ========================================================
        // CATEGORY DETAILS
        // ========================================================

        loadCategorySummary(
            categorySummary = categorySummary,
            toggleButton = btnToggleCategories,
            budget = budget,
            userCategories = userCategories
        )


        // ========================================================
        // EDIT
        // ========================================================

        btnEditBudget.setOnClickListener {

            openEditBudget(
                budget
            )
        }


        // ========================================================
        // DELETE
        // ========================================================

        btnDeleteBudget.setOnClickListener {

            showDeleteConfirmation(
                budget
            )
        }
    }


    // ============================================================
    // LOAD CATEGORY SUMMARY
    // ============================================================

    private suspend fun loadCategorySummary(
        categorySummary: LinearLayout,
        toggleButton: TextView,
        budget: Budget,
        userCategories: List<CategoryEntity>
    ) {


        // ========================================================
        // RESET
        // ========================================================

        categorySummary.removeAllViews()


        categorySummary.visibility =
            View.GONE


        toggleButton.visibility =
            View.GONE


        // ========================================================
        // GET BUDGET CATEGORIES
        // ========================================================

        val budgetCategories =
            database
                .budgetDao()
                .getBudgetCategoriesOnce(
                    budget.budgetId
                )


        if (
            budgetCategories.isEmpty()
        ) {

            return
        }


        // ========================================================
        // CREATE CATEGORY VIEWS
        // ========================================================

        var validCategoryCount =
            0


        budgetCategories.forEach { budgetCategory ->


            val category =
                userCategories.find {

                    it.id ==
                            budgetCategory.categoryId
                }


            if (
                category == null
            ) {

                return@forEach
            }


            // ====================================================
            // CATEGORY SPENDING
            // ====================================================

            val categorySpent =
                database
                    .transactionDao()
                    .getCategoryExpenseForPeriod(
                        userId = userId,
                        categoryId = category.id,
                        startDate = budget.startDate,
                        endDate = budget.endDate
                    )


            // ====================================================
            // CREATE CATEGORY VIEW
            // ====================================================

            val categoryView =
                createCategoryProgressView(
                    category = category,
                    spent = categorySpent,
                    limit = budgetCategory.limitAmount
                )


            categorySummary.addView(
                categoryView
            )


            validCategoryCount++
        }


        // ========================================================
        // SHOW TOGGLE
        // ========================================================

        if (
            validCategoryCount > 0
        ) {


            toggleButton.visibility =
                View.VISIBLE


            toggleButton.text =
                "Show category details ($validCategoryCount) ▼"


            // ====================================================
            // TOGGLE CLICK
            // ====================================================

            toggleButton.setOnClickListener {


                val isExpanded =
                    categorySummary.visibility ==
                            View.VISIBLE


                if (
                    isExpanded
                ) {


                    // --------------------------------------------
                    // COLLAPSE
                    // --------------------------------------------

                    categorySummary.visibility =
                        View.GONE


                    toggleButton.text =
                        "Show category details ($validCategoryCount) ▼"


                } else {


                    // --------------------------------------------
                    // EXPAND
                    // --------------------------------------------

                    categorySummary.visibility =
                        View.VISIBLE


                    toggleButton.text =
                        "Hide category details ($validCategoryCount) ▲"
                }
            }
        }
    }


    // ============================================================
    // CREATE CATEGORY PROGRESS VIEW
    // ============================================================

    private fun createCategoryProgressView(
        category: CategoryEntity,
        spent: Double,
        limit: Double
    ): LinearLayout {


        // ========================================================
        // CONTAINER
        // ========================================================

        val container =
            LinearLayout(
                this
            )


        container.orientation =
            LinearLayout.VERTICAL


        container.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                // Smaller spacing than before
                topMargin =
                    dpToPx(
                        8
                    )
            }


        // ========================================================
        // CATEGORY NAME
        // ========================================================

        val categoryName =
            TextView(
                this
            )


        categoryName.text =
            category.label


        categoryName.textSize =
            12f


        categoryName.typeface =
            ResourcesCompat.getFont(
                this,
                R.font.dosis_semibold
            )


        categoryName.setTextColor(
            category.tintColor
        )


        container.addView(
            categoryName
        )


        // ========================================================
        // PROGRESS BAR
        // ========================================================

        val progressBar =
            ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
            )


        progressBar.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(
                    6
                )
            ).apply {

                topMargin =
                    dpToPx(
                        5
                    )
            }


        progressBar.max =
            100


        val progress =
            if (
                limit > 0
            ) {

                (
                        spent /
                                limit *
                                100
                        )
                    .toInt()
                    .coerceIn(
                        0,
                        100
                    )

            } else {

                0
            }


        progressBar.progress =
            progress


        progressBar.progressTintList =
            ColorStateList.valueOf(
                category.tintColor
            )


        progressBar.progressBackgroundTintList =
            ColorStateList.valueOf(
                category.bgColor
            )


        container.addView(
            progressBar
        )


        // ========================================================
        // SPENT / LIMIT TEXT
        // ========================================================

        val amountText =
            TextView(
                this
            )


        amountText.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                topMargin =
                    dpToPx(
                        3
                    )
            }


        amountText.text =
            "${
                formatShortMoney(
                    spent
                )
            } / ${
                formatShortMoney(
                    limit
                )
            }"


        amountText.textSize =
            10f


        amountText.gravity =
            Gravity.END


        amountText.typeface =
            ResourcesCompat.getFont(
                this,
                R.font.dosis_medium
            )


        amountText.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.neutral_500
            )
        )


        container.addView(
            amountText
        )


        return container
    }


    // ============================================================
    // EDIT BUDGET
    // ============================================================

    private fun openEditBudget(
        budget: Budget
    ) {

        val intent =
            Intent(
                this,
                AddBudgetActivity::class.java
            )


        intent.putExtra(
            AddBudgetActivity.EXTRA_USER_ID,
            userId
        )


        intent.putExtra(
            AddBudgetActivity.EXTRA_BUDGET_ID,
            budget.budgetId
        )


        startActivity(
            intent
        )
    }


    // ============================================================
    // DELETE CONFIRMATION
    // ============================================================

    private fun showDeleteConfirmation(
        budget: Budget
    ) {

        AlertDialog.Builder(
            this
        )

            .setTitle(
                "Delete Budget?"
            )

            .setMessage(
                "Are you sure you want to delete \"${budget.name}\"?\n\n" +
                        "The category limits for this budget will also be removed."
            )

            .setNegativeButton(
                "Cancel",
                null
            )

            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                deleteBudget(
                    budget
                )
            }

            .show()
    }


    // ============================================================
    // DELETE BUDGET
    // ============================================================

    private fun deleteBudget(
        budget: Budget
    ) {

        lifecycleScope.launch {

            try {


                val existingBudget =
                    database
                        .budgetDao()
                        .getBudgetById(
                            budget.budgetId
                        )


                if (
                    existingBudget == null
                ) {

                    Toast.makeText(
                        this@BudgetActivity,
                        "Budget not found",
                        Toast.LENGTH_SHORT
                    ).show()


                    return@launch
                }


                if (
                    existingBudget.userId !=
                    userId
                ) {

                    Toast.makeText(
                        this@BudgetActivity,
                        "You cannot delete this budget",
                        Toast.LENGTH_SHORT
                    ).show()


                    return@launch
                }


                database
                    .budgetDao()
                    .deleteBudgetCompletely(
                        existingBudget
                    )


                Toast.makeText(
                    this@BudgetActivity,
                    "Budget deleted",
                    Toast.LENGTH_SHORT
                ).show()


            } catch (
                e: Exception
            ) {

                Toast.makeText(
                    this@BudgetActivity,
                    "Failed to delete budget: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // ============================================================
    // UPDATE MONTH SUMMARY
    // ============================================================
    private fun updateBudgetSummary(
        budgets: List<Budget>
    ) {

        lifecycleScope.launch {

            var totalBudget = 0.0

            var totalSpent = 0.0

            budgets.forEach { budget ->

                totalBudget += budget.amount

                val spent =
                    database
                        .transactionDao()
                        .getBudgetExpense(
                            userId = userId,
                            startDate = budget.startDate,
                            endDate = budget.endDate
                        )

                totalSpent +=
                    spent.coerceAtLeast(0.0)
            }

            val remaining =
                (
                        totalBudget -
                                totalSpent
                        ).coerceAtLeast(0.0)

            val progress =
                if (totalBudget > 0) {

                    (
                            totalSpent /
                                    totalBudget *
                                    100
                            )
                        .toInt()
                        .coerceIn(0, 100)

                } else {

                    0
                }

            tvTotalBudget.text =
                formatMoney(totalBudget)

            tvBudgetSpent.text =
                "${formatMoney(totalSpent)} spent"

            tvMonthRemaining.text =
                "${formatMoney(remaining)} remaining"

            tvProgressPercent.text =
                "$progress% used"

            monthBudgetProgress.progress =
                progress
        }
    }


    // ============================================================
    // GET BUDGET STATUS
    // ============================================================

    private fun getBudgetStatus(
        budget: Budget
    ): String {


        val currentTime =
            System.currentTimeMillis()


        return when {


            currentTime <
                    budget.startDate ->

                "Upcoming"


            currentTime >
                    budget.endDate ->

                "Completed"


            else ->

                "Active"
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
            2


        formatter.minimumFractionDigits =
            0


        return "${
            formatter.format(
                amount
            )
        } MMK"
    }


    // ============================================================
    // FORMAT SHORT MONEY
    // ============================================================

    private fun formatShortMoney(
        amount: Double
    ): String {


        return when {


            amount >= 1_000_000 ->

                String.format(
                    Locale.US,
                    "%.1fM",
                    amount /
                            1_000_000
                )


            amount >= 1_000 ->

                String.format(
                    Locale.US,
                    "%.0fK",
                    amount /
                            1_000
                )


            else ->

                String.format(
                    Locale.US,
                    "%.0f",
                    amount
                )
        }
    }


    // ============================================================
    // FORMAT DATE
    // ============================================================

    private fun formatBudgetDate(
        startDate: Long,
        endDate: Long
    ): String {


        val start =
            android
                .text
                .format
                .DateFormat
                .format(
                    "MMM dd",
                    Date(
                        startDate
                    )
                )


        val end =
            android
                .text
                .format
                .DateFormat
                .format(
                    "MMM dd",
                    Date(
                        endDate
                    )
                )


        return "$start - $end"
    }


    // ============================================================
    // DP TO PX
    // ============================================================

    private fun dpToPx(
        dp: Int
    ): Int {


        return (
                dp *
                        resources
                            .displayMetrics
                            .density
                )
            .toInt()
    }
}