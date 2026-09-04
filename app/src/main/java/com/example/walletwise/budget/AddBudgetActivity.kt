package com.example.walletwise.budget

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walletwise.R
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.entity.Budget
import com.example.walletwise.entity.BudgetCategory
import com.example.walletwise.entity.CategoryEntity
import com.example.walletwise.entity.Notification
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddBudgetActivity : AppCompatActivity() {

    companion object {

        const val EXTRA_USER_ID =
            "USER_ID"

        const val EXTRA_BUDGET_ID =
            "BUDGET_ID"

        const val EXTRA_NEW_NOTIFICATION_ID =
            "NEW_NOTIFICATION_ID"
    }


    // ============================================================
    // DATABASE
    // ============================================================

    private lateinit var database: AppDatabase


    // ============================================================
    // USER
    // ============================================================

    private var userId: Int = -1

    private var userCurrency: String = "MMK"


    // ============================================================
    // VIEWS
    // ============================================================

    private lateinit var etBudgetName: TextInputEditText

    private lateinit var etBudgetAmount: TextInputEditText

    private lateinit var etStartDate: TextInputEditText

    private lateinit var etEndDate: TextInputEditText

    private lateinit var tilBudgetAmount: TextInputLayout

    private lateinit var categoryContainer: LinearLayout

    private lateinit var btnAddCategory: Button

    private lateinit var btnCreateBudget: Button

    private lateinit var btnBack: View


    // ============================================================
    // MODE
    // ============================================================

    private var budgetId: Int = -1

    private var isEditMode = false


    // ============================================================
    // DATE
    // ============================================================

    private var startDateMillis: Long = 0L

    private var endDateMillis: Long = 0L


    // ============================================================
    // CATEGORIES
    // ============================================================

    private var userCategories: List<CategoryEntity> =
        emptyList()


    private val dateFormatter =
        SimpleDateFormat(
            "MMMM dd, yyyy",
            Locale.getDefault()
        )


    // ============================================================
    // ON CREATE
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_add_budget
        )


        userId =
            intent.getIntExtra(
                EXTRA_USER_ID,
                -1
            )


        budgetId =
            intent.getIntExtra(
                EXTRA_BUDGET_ID,
                -1
            )


        isEditMode =
            budgetId != -1


        if (userId == -1) {

            Toast.makeText(
                this,
                "User not found",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }


        database =
            AppDatabase.getDatabase(
                applicationContext
            )


        bindViews()

        setupClickListeners()


        lifecycleScope.launch {

            loadUserCurrency()

            loadCategories()


            if (isEditMode) {

                loadBudgetForEdit()

            } else {

                setupDefaultDates()
            }
        }
    }


    // ============================================================
    // LOAD USER CURRENCY
    // ============================================================

    private suspend fun loadUserCurrency() {

        val user =
            database
                .userDao()
                .getUserByIdOnce(userId)


        userCurrency =
            user
                ?.currency
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "MMK"


        tilBudgetAmount.suffixText =
            userCurrency
    }


    // ============================================================
    // BIND VIEWS
    // ============================================================

    private fun bindViews() {

        btnBack =
            findViewById(R.id.btnBack)

        etBudgetName =
            findViewById(R.id.etBudgetName)

        etBudgetAmount =
            findViewById(R.id.etBudgetAmount)

        etStartDate =
            findViewById(R.id.etStartDate)

        etEndDate =
            findViewById(R.id.etEndDate)

        tilBudgetAmount =
            findViewById(R.id.tilBudgetAmount)

        categoryContainer =
            findViewById(R.id.categoryContainer)

        btnAddCategory =
            findViewById(R.id.btnAddCategory)

        btnCreateBudget =
            findViewById(R.id.btnCreateBudget)
    }


    // ============================================================
    // CLICK LISTENERS
    // ============================================================

    private fun setupClickListeners() {

        btnBack.setOnClickListener {
            finish()
        }


        etStartDate.setOnClickListener {
            showStartDatePicker()
        }


        etEndDate.setOnClickListener {
            showEndDatePicker()
        }


        btnAddCategory.setOnClickListener {

            if (userCategories.isEmpty()) {

                Toast.makeText(
                    this,
                    "No categories available",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            addCategoryRow()
        }


        btnCreateBudget.setOnClickListener {
            createOrUpdateBudget()
        }
    }


    // ============================================================
    // LOAD CATEGORIES
    // ============================================================

    private suspend fun loadCategories() {

        userCategories =
            database
                .categoryDao()
                .observeAll(userId)
                .first()
    }


    // ============================================================
    // DEFAULT DATES
    // ============================================================

    private fun setupDefaultDates() {

        val calendar =
            Calendar.getInstance()


        calendar.set(
            Calendar.DAY_OF_MONTH,
            1
        )


        startDateMillis =
            getStartOfDay(
                calendar.timeInMillis
            )


        calendar.set(
            Calendar.DAY_OF_MONTH,
            calendar.getActualMaximum(
                Calendar.DAY_OF_MONTH
            )
        )


        endDateMillis =
            getEndOfDay(
                calendar.timeInMillis
            )


        updateDateFields()
    }


    // ============================================================
    // UPDATE DATE FIELDS
    // ============================================================

    private fun updateDateFields() {

        etStartDate.setText(
            dateFormatter.format(
                startDateMillis
            )
        )


        etEndDate.setText(
            dateFormatter.format(
                endDateMillis
            )
        )
    }


    // ============================================================
    // LOAD BUDGET FOR EDIT
    // ============================================================

    private suspend fun loadBudgetForEdit() {

        val budget =
            database
                .budgetDao()
                .getBudgetById(budgetId)


        if (budget == null) {

            Toast.makeText(
                this,
                "Budget not found",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }


        if (budget.userId != userId) {

            Toast.makeText(
                this,
                "You cannot edit this budget",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }


        btnCreateBudget.text =
            "Update Budget"


        etBudgetName.setText(
            budget.name
        )


        etBudgetAmount.setText(
            budget.amount.toString()
        )


        startDateMillis =
            budget.startDate


        endDateMillis =
            budget.endDate


        updateDateFields()


        val budgetCategories =
            database
                .budgetDao()
                .getBudgetCategoriesOnce(
                    budgetId
                )


        categoryContainer.removeAllViews()


        budgetCategories.forEach {

            addCategoryRow(
                selectedCategoryId =
                    it.categoryId,

                categoryAmount =
                    it.limitAmount
            )
        }
    }


    // ============================================================
    // ADD CATEGORY ROW
    // ============================================================

    private fun addCategoryRow(
        selectedCategoryId: Long? = null,
        categoryAmount: Double? = null
    ) {

        val row =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.item_budget_category,
                    categoryContainer,
                    false
                )


        val categoryDropdown =
            row.findViewById<MaterialAutoCompleteTextView>(
                R.id.spCategory
            )


        val etCategoryAmount =
            row.findViewById<TextInputEditText>(
                R.id.etCategoryAmount
            )


        val tilCategoryAmount =
            row.findViewById<TextInputLayout>(
                R.id.tilCategoryAmount
            )


        val btnRemove =
            row.findViewById<TextView>(
                R.id.btnRemoveCategory
            )


        tilCategoryAmount.suffixText =
            userCurrency


        val categoryNames =
            userCategories.map {
                it.label
            }


        val adapter =
            ArrayAdapter(
                this,
                R.layout.item_category_dropdown,
                categoryNames
            )


        categoryDropdown.setAdapter(adapter)


        categoryDropdown.setOnClickListener {
            categoryDropdown.showDropDown()
        }


        if (selectedCategoryId != null) {

            val position =
                userCategories.indexOfFirst {

                    it.id ==
                            selectedCategoryId
                }


            if (position >= 0) {

                categoryDropdown.setText(
                    userCategories[position].label,
                    false
                )
            }
        }


        if (categoryAmount != null) {

            etCategoryAmount.setText(
                categoryAmount.toString()
            )
        }


        btnRemove.setOnClickListener {

            categoryContainer.removeView(row)
        }


        categoryContainer.addView(row)
    }


    // ============================================================
    // START DATE PICKER
    // ============================================================

    private fun showStartDatePicker() {

        val calendar =
            Calendar.getInstance()

        calendar.timeInMillis =
            startDateMillis


        val dialog =
            DatePickerDialog(
                this,
                R.style.Theme_WalletWise_DatePicker,
                { _, year, month, day ->

                    val selected =
                        Calendar.getInstance()

                    selected.set(
                        year,
                        month,
                        day,
                        0,
                        0,
                        0
                    )

                    selected.set(
                        Calendar.MILLISECOND,
                        0
                    )


                    startDateMillis =
                        selected.timeInMillis


                    if (
                        endDateMillis <
                        startDateMillis
                    ) {

                        endDateMillis =
                            getEndOfDay(
                                startDateMillis
                            )
                    }


                    updateDateFields()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )


        dialog.setOnShowListener {

            dialog.getButton(
                DatePickerDialog.BUTTON_POSITIVE
            ).setTextColor(
                getColor(
                    R.color.primary_500
                )
            )


            dialog.getButton(
                DatePickerDialog.BUTTON_NEGATIVE
            ).setTextColor(
                getColor(
                    R.color.primary_500
                )
            )
        }


        dialog.show()
    }


    // ============================================================
    // END DATE PICKER
    // ============================================================

    private fun showEndDatePicker() {

        val calendar =
            Calendar.getInstance()

        calendar.timeInMillis =
            endDateMillis


        val dialog =
            DatePickerDialog(
                this,
                R.style.Theme_WalletWise_DatePicker,
                { _, year, month, day ->

                    val selected =
                        Calendar.getInstance()

                    selected.set(
                        year,
                        month,
                        day,
                        23,
                        59,
                        59
                    )

                    selected.set(
                        Calendar.MILLISECOND,
                        999
                    )


                    val selectedEndDate =
                        selected.timeInMillis


                    if (
                        selectedEndDate <
                        startDateMillis
                    ) {

                        Toast.makeText(
                            this,
                            "End date cannot be before start date",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@DatePickerDialog
                    }


                    endDateMillis =
                        selectedEndDate


                    updateDateFields()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )


        dialog.setOnShowListener {

            dialog.getButton(
                DatePickerDialog.BUTTON_POSITIVE
            ).setTextColor(
                getColor(
                    R.color.primary_500
                )
            )


            dialog.getButton(
                DatePickerDialog.BUTTON_NEGATIVE
            ).setTextColor(
                getColor(
                    R.color.primary_500
                )
            )
        }


        dialog.show()
    }


    // ============================================================
    // CREATE OR UPDATE BUDGET
    // ============================================================

    private fun createOrUpdateBudget() {

        val name =
            etBudgetName.text
                ?.toString()
                ?.trim()
                .orEmpty()


        val amountText =
            etBudgetAmount.text
                ?.toString()
                ?.trim()
                .orEmpty()


        if (name.isEmpty()) {

            etBudgetName.error =
                "Enter budget name"

            etBudgetName.requestFocus()

            return
        }


        if (amountText.isEmpty()) {

            etBudgetAmount.error =
                "Enter budget amount"

            etBudgetAmount.requestFocus()

            return
        }


        val totalAmount =
            amountText.toDoubleOrNull()


        if (
            totalAmount == null ||
            totalAmount <= 0
        ) {

            etBudgetAmount.error =
                "Enter a valid amount"

            etBudgetAmount.requestFocus()

            return
        }


        if (
            endDateMillis <
            startDateMillis
        ) {

            Toast.makeText(
                this,
                "End date cannot be before start date",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ========================================================
        // CATEGORY LIMITS
        // ========================================================

        val categoryLimits =
            mutableListOf<Pair<Long, Double>>()


        for (
        index in
        0 until categoryContainer.childCount
        ) {

            val row =
                categoryContainer.getChildAt(index)


            val categoryDropdown =
                row.findViewById<MaterialAutoCompleteTextView>(
                    R.id.spCategory
                )


            val amountInput =
                row.findViewById<TextInputEditText>(
                    R.id.etCategoryAmount
                )


            val selectedCategoryName =
                categoryDropdown.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()


            if (
                selectedCategoryName.isEmpty()
            ) {

                Toast.makeText(
                    this,
                    "Please select a category",
                    Toast.LENGTH_SHORT
                ).show()

                categoryDropdown.requestFocus()

                return
            }


            val category =
                userCategories.firstOrNull {

                    it.label ==
                            selectedCategoryName
                }


            if (category == null) {

                Toast.makeText(
                    this,
                    "Please select a valid category",
                    Toast.LENGTH_SHORT
                ).show()

                categoryDropdown.requestFocus()

                return
            }


            val limit =
                amountInput.text
                    ?.toString()
                    ?.trim()
                    ?.toDoubleOrNull()


            if (
                limit == null ||
                limit <= 0
            ) {

                amountInput.error =
                    "Enter valid amount"

                amountInput.requestFocus()

                return
            }


            categoryLimits.add(
                Pair(
                    category.id,
                    limit
                )
            )
        }


        // ========================================================
        // DUPLICATE CATEGORY CHECK
        // ========================================================

        val duplicateCategory =
            categoryLimits
                .groupBy {
                    it.first
                }
                .any {
                    it.value.size > 1
                }


        if (duplicateCategory) {

            Toast.makeText(
                this,
                "Each category can only be added once",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ========================================================
        // CATEGORY TOTAL
        // ========================================================

        val categoryTotal =
            categoryLimits.sumOf {
                it.second
            }


        if (
            categoryTotal >
            totalAmount
        ) {

            Toast.makeText(
                this,
                "Category limits cannot exceed total budget",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ========================================================
        // SAVE
        // ========================================================

        lifecycleScope.launch {

            try {

                // ==================================================
                // CREATE
                // ==================================================

                if (!isEditMode) {

                    val budget =
                        Budget(
                            userId = userId,
                            name = name,
                            amount = totalAmount,
                            startDate = startDateMillis,
                            endDate = endDateMillis
                        )


                    val newBudgetId =
                        database
                            .budgetDao()
                            .insertBudget(
                                budget
                            )
                            .toInt()


                    if (
                        categoryLimits.isNotEmpty()
                    ) {

                        val budgetCategories =
                            categoryLimits.map {

                                BudgetCategory(

                                    budgetId =
                                        newBudgetId,

                                    categoryId =
                                        it.first,

                                    limitAmount =
                                        it.second
                                )
                            }


                        database
                            .budgetDao()
                            .insertBudgetCategories(
                                budgetCategories
                            )
                    }


                    Toast.makeText(
                        this@AddBudgetActivity,
                        "Budget created successfully",
                        Toast.LENGTH_SHORT
                    ).show()


                    setResult(
                        RESULT_OK
                    )


                    finish()

                    return@launch
                }


                // ==================================================
                // EDIT
                // ==================================================

                val existingBudget =
                    database
                        .budgetDao()
                        .getBudgetById(
                            budgetId
                        )


                if (existingBudget == null) {

                    Toast.makeText(
                        this@AddBudgetActivity,
                        "Budget not found",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }


                // ==================================================
                // OWNERSHIP CHECK
                // ==================================================

                if (
                    existingBudget.userId !=
                    userId
                ) {

                    Toast.makeText(
                        this@AddBudgetActivity,
                        "You cannot edit this budget",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }


                // ==================================================
                // UPDATED BUDGET
                // ==================================================

                val updatedBudget =
                    existingBudget.copy(

                        name =
                            name,

                        amount =
                            totalAmount,

                        startDate =
                            startDateMillis,

                        endDate =
                            endDateMillis
                    )


                // ==================================================
                // UPDATED CATEGORY LIMITS
                // ==================================================

                val budgetCategories =
                    categoryLimits.map {

                        BudgetCategory(

                            budgetId =
                                budgetId,

                            categoryId =
                                it.first,

                            limitAmount =
                                it.second
                        )
                    }


                // ==================================================
                // UPDATE BUDGET + CATEGORIES
                // ==================================================

                database
                    .budgetDao()
                    .updateBudgetWithCategories(

                        budget =
                            updatedBudget,

                        categories =
                            budgetCategories
                    )


                // ==================================================
                // CHECK OVERALL BUDGET EXCEEDED
                //
                // IMPORTANT:
                // We check AFTER saving the new budget.
                //
                // Example:
                // Old = 100,000
                // New = 90,000
                // Spent = 95,000
                //
                // 95,000 > 90,000
                // => create notification
                // ==================================================

                var newNotificationId =
                    -1


                val totalSpent =
                    database
                        .transactionDao()
                        .getBudgetExpense(

                            userId =
                                userId,

                            startDate =
                                updatedBudget.startDate,

                            endDate =
                                updatedBudget.endDate
                        )


                if (
                    totalSpent >
                    updatedBudget.amount
                ) {

                    val existingNotification =
                        database
                            .notificationDao()
                            .countBudgetExceededNotification(

                                userId =
                                    userId,

                                budgetId =
                                    updatedBudget.budgetId
                            )


                    // ==================================================
                    // DUPLICATE PREVENTION
                    // ==================================================

                    if (
                        existingNotification == 0
                    ) {

                        val notification =
                            Notification(

                                userId =
                                    userId,

                                title =
                                    "Budget Exceeded ⚠️",

                                message =
                                    "\"${updatedBudget.name}\" budget has been exceeded. " +
                                            "You spent " +
                                            "${formatMoney(totalSpent)} " +
                                            "of " +
                                            "${formatMoney(updatedBudget.amount)}.",

                                type =
                                    "BUDGET_EXCEEDED",

                                referenceType =
                                    "BUDGET",

                                referenceId =
                                    updatedBudget.budgetId,

                                isRead =
                                    false,

                                timeAgo =
                                    "Just now",

                                createdAt =
                                    System.currentTimeMillis()
                                        .toString()
                            )


                        newNotificationId =
                            database
                                .notificationDao()
                                .insertNotificationAndGetId(
                                    notification
                                )
                                .toInt()
                    }
                }


                // ==================================================
                // RETURN RESULT TO BUDGET ACTIVITY
                // ==================================================

                val resultIntent =
                    Intent().apply {

                        putExtra(
                            EXTRA_NEW_NOTIFICATION_ID,
                            newNotificationId
                        )
                    }


                setResult(
                    RESULT_OK,
                    resultIntent
                )


                Toast.makeText(
                    this@AddBudgetActivity,
                    "Budget updated successfully",
                    Toast.LENGTH_SHORT
                ).show()


                finish()

            } catch (
                e: Exception
            ) {

                Toast.makeText(
                    this@AddBudgetActivity,
                    "Failed to save budget: ${e.message}",
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

        return String.format(
            Locale.US,
            "%,.0f %s",
            amount,
            userCurrency
        )
    }


    // ============================================================
    // START OF DAY
    // ============================================================

    private fun getStartOfDay(
        time: Long
    ): Long {

        val calendar =
            Calendar.getInstance()

        calendar.timeInMillis =
            time

        calendar.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        calendar.set(
            Calendar.MINUTE,
            0
        )

        calendar.set(
            Calendar.SECOND,
            0
        )

        calendar.set(
            Calendar.MILLISECOND,
            0
        )

        return calendar.timeInMillis
    }


    // ============================================================
    // END OF DAY
    // ============================================================

    private fun getEndOfDay(
        time: Long
    ): Long {

        val calendar =
            Calendar.getInstance()

        calendar.timeInMillis =
            time

        calendar.set(
            Calendar.HOUR_OF_DAY,
            23
        )

        calendar.set(
            Calendar.MINUTE,
            59
        )

        calendar.set(
            Calendar.SECOND,
            59
        )

        calendar.set(
            Calendar.MILLISECOND,
            999
        )

        return calendar.timeInMillis
    }
}