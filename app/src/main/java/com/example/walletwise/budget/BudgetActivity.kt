package com.example.walletwise.budget

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.example.walletwise.R
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.entity.Budget
import com.example.walletwise.entity.CategoryEntity
import com.example.walletwise.notification.NotificationPopupManager
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

    private lateinit var notificationPopupManager:
            NotificationPopupManager


    // ============================================================
    // USER
    // ============================================================

    private var userId: Int = -1

    private var userCurrency: String = "MMK"


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
        // NOTIFICATION POPUP
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

        BottomNavHelper.setup(
            activity = this,
            root = findViewById(
                android.R.id.content
            ),
            current = NavTab.BUDGETS,
            userId = userId
        )


        // ========================================================
        // ADD BUDGET BUTTON
        // ========================================================

        setupAddBudgetButton()


        // ========================================================
        // LOAD USER CURRENCY THEN OBSERVE BUDGETS
        // ========================================================

        lifecycleScope.launch {

            loadUserCurrency()

            observeBudgets()
        }
    }


    // ============================================================
    // LOAD USER CURRENCY
    // ============================================================

    private suspend fun loadUserCurrency() {

        val user =
            database
                .userDao()
                .getUserByIdOnce(
                    userId
                )


        userCurrency =
            user
                ?.currency
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "MMK"
    }


    // ============================================================
    // INITIALIZE VIEWS
    // ============================================================

    private fun initializeViews() {

        val btnBack =
            findViewById<ImageView>(
                R.id.btnBack
            )


        btnBack.setOnClickListener {

            onBackPressedDispatcher
                .onBackPressed()
        }


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

    private suspend fun observeBudgets() {

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


    // ============================================================
    // DISPLAY BUDGETS
    // ============================================================

    private suspend fun displayBudgets(
        budgets: List<Budget>
    ) {

        budgetListContainer.removeAllViews()


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
                dpToPx(16),
                dpToPx(30),
                dpToPx(16),
                dpToPx(30)
            )


            emptyView.gravity =
                Gravity.CENTER


            budgetListContainer.addView(
                emptyView
            )


            return
        }


        val userCategories =
            database
                .categoryDao()
                .observeAll(
                    userId
                )
                .first()


        budgets.forEach { budget ->

            val budgetView =
                LayoutInflater
                    .from(this)
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
        // BASIC BUDGET INFORMATION
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
                    ).coerceAtLeast(
                    0.0
                )


        val progress =
            if (budget.amount > 0) {

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
        // CATEGORY SUMMARY
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

        categorySummary.removeAllViews()

        categorySummary.visibility =
            View.GONE

        toggleButton.visibility =
            View.GONE


        val budgetCategories =
            database
                .budgetDao()
                .getBudgetCategoriesOnce(
                    budget.budgetId
                )


        if (budgetCategories.isEmpty()) {
            return
        }


        var validCategoryCount =
            0


        budgetCategories.forEach { budgetCategory ->

            val category =
                userCategories.find {

                    it.id ==
                            budgetCategory.categoryId
                }


            if (category == null) {
                return@forEach
            }


            val categorySpent =
                database
                    .transactionDao()
                    .getCategoryExpenseForPeriod(
                        userId = userId,
                        categoryId = category.id,
                        startDate = budget.startDate,
                        endDate = budget.endDate
                    )


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


        if (validCategoryCount > 0) {

            toggleButton.visibility =
                View.VISIBLE


            toggleButton.text =
                "Show category details ($validCategoryCount) ▼"


            toggleButton.setOnClickListener {

                val isExpanded =
                    categorySummary.visibility ==
                            View.VISIBLE


                if (isExpanded) {

                    categorySummary.visibility =
                        View.GONE

                    toggleButton.text =
                        "Show category details ($validCategoryCount) ▼"

                } else {

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

                topMargin =
                    dpToPx(8)
            }


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


        val progressBar =
            ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
            )


        progressBar.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(6)
            ).apply {

                topMargin =
                    dpToPx(5)
            }


        progressBar.max =
            100


        val progress =
            if (limit > 0) {

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
                    dpToPx(3)
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
            } $userCurrency"


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


        // IMPORTANT:
        // Use Activity Result instead of startActivity()
        // so AddBudgetActivity can return the newly created
        // notification ID.
        budgetEditResult.launch(
            intent
        )
    }


    // ============================================================
    // RECEIVE RESULT FROM ADD BUDGET
    // ============================================================

    private val budgetEditResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            // ----------------------------------------------------
            // Make sure the edit Activity completed successfully.
            // ----------------------------------------------------

            if (
                result.resultCode != RESULT_OK
            ) {
                return@registerForActivityResult
            }


            // ----------------------------------------------------
            // Get notification ID returned by AddBudgetActivity.
            // ----------------------------------------------------

            val notificationId =
                result.data?.getIntExtra(
                    AddBudgetActivity.EXTRA_NEW_NOTIFICATION_ID,
                    -1
                ) ?: -1


            // ----------------------------------------------------
            // -1 means no NEW notification was created.
            // ----------------------------------------------------

            if (
                notificationId == -1
            ) {
                return@registerForActivityResult
            }


            // ----------------------------------------------------
            // Load notification from Room.
            // ----------------------------------------------------

            lifecycleScope.launch {

                val notification =
                    database
                        .notificationDao()
                        .getNotificationById(
                            notificationId
                        )


                // ------------------------------------------------
                // Show popup if notification exists.
                // ------------------------------------------------

                if (notification != null) {

                    notificationPopupManager.show(
                        notification
                    )
                }
            }
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


                if (existingBudget == null) {

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

            } catch (e: Exception) {

                Toast.makeText(
                    this@BudgetActivity,
                    "Failed to delete budget: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // ============================================================
    // UPDATE BUDGET SUMMARY
    // ============================================================

    private fun updateBudgetSummary(
        budgets: List<Budget>
    ) {

        lifecycleScope.launch {

            var totalBudget =
                0.0

            var totalSpent =
                0.0


            budgets.forEach { budget ->

                totalBudget +=
                    budget.amount


                val spent =
                    database
                        .transactionDao()
                        .getBudgetExpense(
                            userId = userId,
                            startDate = budget.startDate,
                            endDate = budget.endDate
                        )


                totalSpent +=
                    spent.coerceAtLeast(
                        0.0
                    )
            }


            val remaining =
                (
                        totalBudget -
                                totalSpent
                        ).coerceAtLeast(
                        0.0
                    )


            val progress =
                if (totalBudget > 0) {

                    (
                            totalSpent /
                                    totalBudget *
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


            tvTotalBudget.text =
                formatMoney(
                    totalBudget
                )


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


        return "${formatter.format(amount)} $userCurrency"
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
                    amount / 1_000_000
                )


            amount >= 1_000 ->

                String.format(
                    Locale.US,
                    "%.0fK",
                    amount / 1_000
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
            android.text.format.DateFormat.format(
                "MMM dd",
                Date(startDate)
            )


        val end =
            android.text.format.DateFormat.format(
                "MMM dd",
                Date(endDate)
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
                        resources.displayMetrics.density
                ).toInt()
    }


    // ============================================================
    // CLEANUP POPUP
    // ============================================================

    override fun onDestroy() {

        notificationPopupManager.dismiss()

        super.onDestroy()
    }
}

