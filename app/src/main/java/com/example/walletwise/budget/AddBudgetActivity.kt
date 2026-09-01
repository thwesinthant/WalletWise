package com.example.walletwise.budget

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walletwise.R
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.entity.Budget
import com.example.walletwise.entity.BudgetCategory
import com.example.walletwise.entity.CategoryEntity
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddBudgetActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USER_ID = "USER_ID"
        const val EXTRA_BUDGET_ID = "BUDGET_ID"
    }

    private lateinit var database: AppDatabase

    private lateinit var etBudgetName: TextInputEditText
    private lateinit var etBudgetAmount: TextInputEditText
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText

    private lateinit var categoryContainer: LinearLayout
    private lateinit var btnAddCategory: Button
    private lateinit var btnCreateBudget: Button
    private lateinit var btnBack: View

    private var userId: Int = -1

    private var budgetId: Int = -1

    private var isEditMode = false

    private var startDateMillis: Long = 0L
    private var endDateMillis: Long = 0L

    private var userCategories: List<CategoryEntity> = emptyList()

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

        // --------------------------------------------------------
        // GET USER ID
        // --------------------------------------------------------

        userId =
            intent.getIntExtra(
                EXTRA_USER_ID,
                -1
            )

        // --------------------------------------------------------
        // GET BUDGET ID
        // --------------------------------------------------------

        budgetId =
            intent.getIntExtra(
                EXTRA_BUDGET_ID,
                -1
            )

        isEditMode =
            budgetId != -1


        // --------------------------------------------------------
        // VALIDATE USER
        // --------------------------------------------------------

        if (userId == -1) {

            Toast.makeText(
                this,
                "User not found",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }


        // --------------------------------------------------------
        // DATABASE
        // --------------------------------------------------------

        database =
            AppDatabase.getDatabase(
                applicationContext
            )


        // --------------------------------------------------------
        // BIND VIEWS
        // --------------------------------------------------------

        bindViews()


        // --------------------------------------------------------
        // CLICK LISTENERS
        // --------------------------------------------------------

        setupClickListeners()


        // --------------------------------------------------------
        // LOAD DATA
        // --------------------------------------------------------

        lifecycleScope.launch {

            loadCategories()

            if (isEditMode) {

                loadBudgetForEdit()

            } else {

                setupDefaultDates()
            }
        }
    }


    // ============================================================
    // BIND VIEWS
    // ============================================================

    private fun bindViews() {

        btnBack =
            findViewById(
                R.id.btnBack
            )

        etBudgetName =
            findViewById(
                R.id.etBudgetName
            )

        etBudgetAmount =
            findViewById(
                R.id.etBudgetAmount
            )

        etStartDate =
            findViewById(
                R.id.etStartDate
            )

        etEndDate =
            findViewById(
                R.id.etEndDate
            )

        categoryContainer =
            findViewById(
                R.id.categoryContainer
            )

        btnAddCategory =
            findViewById(
                R.id.btnAddCategory
            )

        btnCreateBudget =
            findViewById(
                R.id.btnCreateBudget
            )
    }


    // ============================================================
    // CLICK LISTENERS
    // ============================================================

    private fun setupClickListeners() {

        // --------------------------------------------------------
        // BACK
        // --------------------------------------------------------

        btnBack.setOnClickListener {

            finish()
        }


        // --------------------------------------------------------
        // START DATE
        // --------------------------------------------------------

        etStartDate.setOnClickListener {

            showStartDatePicker()
        }


        // --------------------------------------------------------
        // END DATE
        // --------------------------------------------------------

        etEndDate.setOnClickListener {

            showEndDatePicker()
        }


        // --------------------------------------------------------
        // ADD CATEGORY
        // --------------------------------------------------------

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


        // --------------------------------------------------------
        // CREATE / UPDATE
        // --------------------------------------------------------

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


        // --------------------------------------------------------
        // FIRST DAY OF CURRENT MONTH
        // --------------------------------------------------------

        calendar.set(
            Calendar.DAY_OF_MONTH,
            1
        )

        startDateMillis =
            getStartOfDay(
                calendar.timeInMillis
            )


        // --------------------------------------------------------
        // LAST DAY OF CURRENT MONTH
        // --------------------------------------------------------

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
    // LOAD EXISTING BUDGET FOR EDIT
    // ============================================================

    private suspend fun loadBudgetForEdit() {

        val budget =
            database
                .budgetDao()
                .getBudgetById(
                    budgetId
                )


        // --------------------------------------------------------
        // BUDGET NOT FOUND
        // --------------------------------------------------------

        if (budget == null) {

            Toast.makeText(
                this@AddBudgetActivity,
                "Budget not found",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }


        // --------------------------------------------------------
        // SECURITY CHECK
        // --------------------------------------------------------

        if (budget.userId != userId) {

            Toast.makeText(
                this@AddBudgetActivity,
                "You cannot edit this budget",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }


        // --------------------------------------------------------
        // CHANGE BUTTON
        // --------------------------------------------------------

        btnCreateBudget.text =
            "Update Budget"


        // --------------------------------------------------------
        // LOAD BASIC INFORMATION
        // --------------------------------------------------------

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


        // --------------------------------------------------------
        // LOAD CATEGORY LIMITS
        // --------------------------------------------------------

        val budgetCategories =
            database
                .budgetDao()
                .getBudgetCategoriesOnce(
                    budgetId
                )


        categoryContainer.removeAllViews()


        // --------------------------------------------------------
        // ADD EXISTING CATEGORY ROWS
        // --------------------------------------------------------

        budgetCategories.forEach { budgetCategory ->

            addCategoryRow(
                selectedCategoryId =
                    budgetCategory.categoryId,
                categoryAmount =
                    budgetCategory.limitAmount
            )
        }

        /*
         * IMPORTANT:
         *
         * If the budget has NO category limits,
         * we intentionally leave categoryContainer empty.
         *
         * This means:
         *
         * Budget
         * ├── Total amount
         * ├── Start date
         * ├── End date
         * └── No category limits
         *
         * This is a valid budget.
         */
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


        // --------------------------------------------------------
        // VIEWS
        // --------------------------------------------------------

        val spinner =
            row.findViewById<Spinner>(
                R.id.spCategory
            )

        val etCategoryAmount =
            row.findViewById<TextInputEditText>(
                R.id.etCategoryAmount
            )

        val btnRemove =
            row.findViewById<TextView>(
                R.id.btnRemoveCategory
            )


        // --------------------------------------------------------
        // CATEGORY NAMES
        // --------------------------------------------------------

        val categoryNames =
            userCategories.map {

                it.label
            }


        // --------------------------------------------------------
        // SPINNER ADAPTER
        // --------------------------------------------------------

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                categoryNames
            )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner.adapter =
            adapter


        // --------------------------------------------------------
        // SELECT EXISTING CATEGORY
        // --------------------------------------------------------

        if (selectedCategoryId != null) {

            val position =
                userCategories.indexOfFirst {

                    it.id ==
                            selectedCategoryId
                }


            if (position >= 0) {

                spinner.setSelection(
                    position
                )
            }
        }


        // --------------------------------------------------------
        // SET EXISTING CATEGORY LIMIT
        // --------------------------------------------------------

        if (categoryAmount != null) {

            etCategoryAmount.setText(
                categoryAmount.toString()
            )
        }


        // --------------------------------------------------------
        // REMOVE CATEGORY
        // --------------------------------------------------------

        btnRemove.setOnClickListener {

            categoryContainer.removeView(
                row
            )
        }


        // --------------------------------------------------------
        // ADD ROW
        // --------------------------------------------------------

        categoryContainer.addView(
            row
        )
    }


    // ============================================================
    // START DATE PICKER
    // ============================================================

    private fun showStartDatePicker() {

        val calendar =
            Calendar.getInstance()

        calendar.timeInMillis =
            startDateMillis


        DatePickerDialog(
            this,
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


                // ------------------------------------------------
                // Automatically move end date if necessary
                // ------------------------------------------------

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
            calendar.get(
                Calendar.YEAR
            ),
            calendar.get(
                Calendar.MONTH
            ),
            calendar.get(
                Calendar.DAY_OF_MONTH
            )
        ).show()
    }


    // ============================================================
    // END DATE PICKER
    // ============================================================

    private fun showEndDatePicker() {

        val calendar =
            Calendar.getInstance()

        calendar.timeInMillis =
            endDateMillis


        DatePickerDialog(
            this,
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


                // ------------------------------------------------
                // VALIDATE
                // ------------------------------------------------

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
            calendar.get(
                Calendar.YEAR
            ),
            calendar.get(
                Calendar.MONTH
            ),
            calendar.get(
                Calendar.DAY_OF_MONTH
            )
        ).show()
    }


    // ============================================================
    // CREATE / UPDATE BUDGET
    // ============================================================

    private fun createOrUpdateBudget() {

        // --------------------------------------------------------
        // GET NAME
        // --------------------------------------------------------

        val name =
            etBudgetName
                .text
                ?.toString()
                ?.trim()
                .orEmpty()


        // --------------------------------------------------------
        // GET AMOUNT
        // --------------------------------------------------------

        val amountText =
            etBudgetAmount
                .text
                ?.toString()
                ?.trim()
                .orEmpty()


        // ========================================================
        // NAME VALIDATION
        // ========================================================

        if (name.isEmpty()) {

            etBudgetName.error =
                "Enter budget name"

            etBudgetName.requestFocus()

            return
        }


        // ========================================================
        // AMOUNT VALIDATION
        // ========================================================

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


        // ========================================================
        // DATE VALIDATION
        // ========================================================

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
        // READ CATEGORY LIMITS
        //
        // CATEGORY LIMITS ARE OPTIONAL.
        //
        // 0 categories = VALID
        // 1 category   = VALID
        // 2 categories = VALID
        // etc.
        // ========================================================

        val categoryLimits =
            mutableListOf<Pair<Long, Double>>()


        for (
        index in
        0 until categoryContainer.childCount
        ) {

            val row =
                categoryContainer
                    .getChildAt(
                        index
                    )


            val spinner =
                row.findViewById<Spinner>(
                    R.id.spCategory
                )


            val amountInput =
                row.findViewById<TextInputEditText>(
                    R.id.etCategoryAmount
                )


            // ----------------------------------------------------
            // SPINNER VALIDATION
            // ----------------------------------------------------

            val selectedPosition =
                spinner.selectedItemPosition


            if (
                selectedPosition < 0 ||
                selectedPosition >=
                userCategories.size
            ) {

                Toast.makeText(
                    this,
                    "Please select a category",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }


            val category =
                userCategories[
                    selectedPosition
                ]


            // ----------------------------------------------------
            // CATEGORY AMOUNT
            // ----------------------------------------------------

            val limit =
                amountInput
                    .text
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
        //
        // ONLY CHECK IF USER ACTUALLY ADDED CATEGORIES.
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
        // SAVE TO ROOM
        // ========================================================

        lifecycleScope.launch {

            try {

                // =================================================
                // CREATE NEW BUDGET
                // =================================================

                if (!isEditMode) {

                    val budget =
                        Budget(
                            userId =
                                userId,

                            name =
                                name,

                            amount =
                                totalAmount,

                            startDate =
                                startDateMillis,

                            endDate =
                                endDateMillis
                        )


                    val newBudgetId =
                        database
                            .budgetDao()
                            .insertBudget(
                                budget
                            )
                            .toInt()


                    // ---------------------------------------------
                    // INSERT CATEGORY LIMITS ONLY IF PRESENT
                    // ---------------------------------------------

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
                }


                // =================================================
                // UPDATE EXISTING BUDGET
                // =================================================

                else {

                    val existingBudget =
                        database
                            .budgetDao()
                            .getBudgetById(
                                budgetId
                            )


                    // ---------------------------------------------
                    // CHECK BUDGET EXISTS
                    // ---------------------------------------------

                    if (existingBudget == null) {

                        Toast.makeText(
                            this@AddBudgetActivity,
                            "Budget not found",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@launch
                    }


                    // ---------------------------------------------
                    // SECURITY CHECK
                    // ---------------------------------------------

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


                    // ---------------------------------------------
                    // UPDATED BUDGET
                    // ---------------------------------------------

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


                    // ---------------------------------------------
                    // PREPARE CATEGORY LIMITS
                    // ---------------------------------------------

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


                    // ---------------------------------------------
                    // UPDATE BUDGET + CATEGORIES
                    //
                    // This method should:
                    //
                    // 1. Update budget
                    // 2. Delete old category limits
                    // 3. Insert new category limits
                    //
                    // If categoryLimits is empty,
                    // old category limits are deleted.
                    // ---------------------------------------------

                    database
                        .budgetDao()
                        .updateBudgetWithCategories(

                            budget =
                                updatedBudget,

                            categories =
                                budgetCategories
                        )


                    Toast.makeText(
                        this@AddBudgetActivity,
                        "Budget updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }


                // =================================================
                // FINISH
                // =================================================

                finish()

            } catch (e: Exception) {

                Toast.makeText(
                    this@AddBudgetActivity,
                    "Failed to save budget: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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