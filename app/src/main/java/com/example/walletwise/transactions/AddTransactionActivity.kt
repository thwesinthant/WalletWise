package com.example.walletwise.transactions

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.walletwise.R
import com.example.walletwise.category.SelectCategoryActivity
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.entity.Account
import com.example.walletwise.entity.CategoryEntity
import com.example.walletwise.entity.Notification
import com.example.walletwise.entity.Transaction
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddTransactionActivity : AppCompatActivity() {

    // ============================================================
    // DATABASE
    // ============================================================

    private lateinit var database: AppDatabase


    // ============================================================
    // USER
    // ============================================================

    private var currentUserId: Int = -1

    private var userCurrency: String = "MMK"


    // ============================================================
    // TRANSACTION STATE
    // ============================================================

    private var isExpense = true

    private var currentAmountStr = ""


    private var editingTransactionId: Int = -1

    private var isEditMode = false

    private var editingCreatedAt: Long = 0L


    // ============================================================
    // SELECTED CATEGORY
    // ============================================================

    private var selectedCategoryId: Long? = null

    private var selectedCategoryLabel = ""


    // ============================================================
    // CATEGORY
    // ============================================================

    private var categoryList: List<CategoryEntity> = emptyList()

    private lateinit var categoryChipContainer: LinearLayout

    private lateinit var tvSelectedCategory: TextView

    // ============================================================
    // SELECTED ACCOUNT / PAYMENT METHOD
    // ============================================================

    private var selectedAccountId: Int? = null

    private var accountList: List<Account> = emptyList()

    private lateinit var paymentChipContainer: LinearLayout


    // ============================================================
    // VIEWS
    // ============================================================

    private lateinit var tvAmount: TextView

    private lateinit var tvExpense: TextView

    private lateinit var tvIncome: TextView

    private lateinit var btnAdd: MaterialButton

    private lateinit var etNote: EditText

    private lateinit var tvTitle: TextView


    // ============================================================
    // INTENT EXTRA
    // ============================================================

    companion object {

        const val EXTRA_USER_ID =
            "USER_ID"

        const val EXTRA_TRANSACTION_ID =
            "TRANSACTION_ID"
    }


    // ============================================================
    // LOAD USER CURRENCY
    // ============================================================

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
                        user.currency

                    updateAmountDisplay()
                }
        }
    }


    // ============================================================
    // CATEGORY RESULT
    // ============================================================

    private val selectCategoryLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (
                result.resultCode == RESULT_OK &&
                result.data != null
            ) {

                val categoryId =
                    result.data?.getLongExtra(
                        SelectCategoryActivity.RESULT_CATEGORY_ID,
                        -1L
                    ) ?: -1L


                val categoryLabel =
                    result.data?.getStringExtra(
                        SelectCategoryActivity.RESULT_CATEGORY_LABEL
                    )


                if (
                    categoryId != -1L &&
                    !categoryLabel.isNullOrEmpty()
                ) {

                    selectedCategoryId =
                        categoryId

                    selectedCategoryLabel =
                        categoryLabel

                    updateSelectedCategoryText()

                    updateCategoryChips()
                }
            }
        }


    // ============================================================
    // ON CREATE
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_add_transaction
        )


        // ========================================================
        // WINDOW INSETS
        // ========================================================

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }


        // ========================================================
        // DATABASE
        // ========================================================

        database =
            AppDatabase.getDatabase(this)


        // ========================================================
// USER
// ========================================================

        currentUserId =
            intent.getIntExtra(
                EXTRA_USER_ID,
                -1
            )


        if (
            currentUserId == -1
        ) {

            Toast.makeText(
                this,
                "User session not found",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }


        // ========================================================
        // EDIT MODE
        // ========================================================

        editingTransactionId =
            intent.getIntExtra(
                EXTRA_TRANSACTION_ID,
                -1
            )


        isEditMode =
            editingTransactionId != -1


        // ========================================================
        // FIND VIEWS
        // ========================================================

        val btnBack =
            findViewById<ImageView>(
                R.id.btnBack
            )

        tvAmount =
            findViewById(
                R.id.tvAmount
            )

        tvExpense =
            findViewById(
                R.id.tvExpense
            )

        tvIncome =
            findViewById(
                R.id.tvIncome
            )

        btnAdd =
            findViewById(
                R.id.btnAdd
            )

        etNote =
            findViewById(
                R.id.etNote
            )

        categoryChipContainer =
            findViewById(
                R.id.categoryChipContainer
            )

        tvSelectedCategory =
            findViewById(
                R.id.tvSelectedCategory
            )

        tvTitle =
            findViewById(
                R.id.tvTitle
            )

        paymentChipContainer =
            findViewById(
                R.id.paymentChipContainer
            )

        // ========================================================
        // BACK
        // ========================================================

        btnBack.setOnClickListener {

            finish()
        }


        // ========================================================
        // INITIAL UI
        // ========================================================

        loadUserCurrency()

        updateAmountDisplay()

        updateSelectedCategoryText()

        setupTypeSelector()

        setupCategoryChips()

        setupPaymentChips()

        setupCustomKeypad()

        if (
            isEditMode
        ) {

            tvTitle.text =
                "Edit Transaction"

            loadTransactionForEditing()

        } else {

            tvTitle.text =
                "Add Expense"
        }


        // ========================================================
        // SAVE
        // ========================================================

        btnAdd.setOnClickListener {

            if (
                isEditMode
            ) {

                updateTransaction()

            } else {

                saveTransactionAndNotify()
            }
        }
    }

    // ============================================================
// UPDATE TRANSACTION
// ============================================================

    private fun updateTransaction() {

        val amount =
            currentAmountStr
                .toDoubleOrNull()


        // ========================================================
        // VALIDATE AMOUNT
        // ========================================================

        if (
            amount == null ||
            amount <= 0.0
        ) {

            Toast.makeText(
                this,
                "Please enter an amount greater than 0",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ========================================================
        // VALIDATE CATEGORY
        // ========================================================

        if (
            selectedCategoryId == null
        ) {

            Toast.makeText(
                this,
                "Please select a category",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // ========================================================
        // VALIDATE ACCOUNT
        // ========================================================

        if (
            accountList.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Please add a payment account first",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        if (
            selectedAccountId == null
        ) {

            Toast.makeText(
                this,
                "Please select a payment account",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // ========================================================
        // NOTE
        // ========================================================

        val note =
            etNote.text
                .toString()
                .trim()


        // ========================================================
        // TITLE
        // ========================================================

        val title =
            if (
                note.isNotEmpty()
            ) {

                note

            } else {

                selectedCategoryLabel
            }


        // ========================================================
        // TYPE
        // ========================================================

        val txnType =
            if (
                isExpense
            ) {

                "EXPENSE"

            } else {

                "INCOME"
            }


        // ========================================================
        // DATABASE UPDATE
        // ========================================================

        lifecycleScope.launch(
            Dispatchers.IO
        ) {

            try {

                database
                    .transactionDao()
                    .updateTransaction(
                        transactionId =
                            editingTransactionId,

                        userId =
                            currentUserId,

                        title =
                            title,

                        amount =
                            amount,

                        type =
                            txnType,

                        categoryId =
                            selectedCategoryId,

                        accountId =
                            selectedAccountId,

                        note =
                            note.ifEmpty {
                                null
                            }
                    )


                withContext(
                    Dispatchers.Main
                ) {

                    Toast.makeText(
                        this@AddTransactionActivity,
                        "Transaction updated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()


                    finish()
                }

            } catch (
                e: Exception
            ) {

                e.printStackTrace()


                withContext(
                    Dispatchers.Main
                ) {

                    Toast.makeText(
                        this@AddTransactionActivity,
                        "Error updating transaction: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ============================================================
// LOAD TRANSACTION FOR EDITING
// ============================================================

    private fun loadTransactionForEditing() {

        lifecycleScope.launch(
            Dispatchers.IO
        ) {

            val transaction =
                database
                    .transactionDao()
                    .getTransactionById(
                        editingTransactionId,
                        currentUserId
                    )


            if (
                transaction == null
            ) {

                withContext(
                    Dispatchers.Main
                ) {

                    Toast.makeText(
                        this@AddTransactionActivity,
                        "Transaction not found",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }

                return@launch
            }


            withContext(
                Dispatchers.Main
            ) {

                // ====================================================
                // AMOUNT
                // ====================================================

                currentAmountStr =
                    transaction.amount
                        .toString()


                // Remove unnecessary .0

                if (
                    currentAmountStr.endsWith(
                        ".0"
                    )
                ) {

                    currentAmountStr =
                        currentAmountStr
                            .removeSuffix(
                                ".0"
                            )
                }


                updateAmountDisplay()


                // ====================================================
                // TYPE
                // ====================================================

                isExpense =
                    transaction.type ==
                            "EXPENSE"


                updateTypeUI()


                // ====================================================
                // CATEGORY
                // ====================================================

                selectedCategoryId =
                    transaction.categoryId


                selectedCategoryLabel =
                    categoryList
                        .firstOrNull {
                            it.id ==
                                    selectedCategoryId
                        }
                        ?.label
                        ?: ""


                updateSelectedCategoryText()

                updateCategoryChips()

                // ====================================================
                // ACCOUNT / PAYMENT METHOD
                // ====================================================

                selectedAccountId =
                    transaction.accountId


                // ====================================================
                // REFRESH PAYMENT CHIPS
                // ====================================================

                updatePaymentChips()


                // ====================================================
                // NOTE
                // ====================================================

                etNote.setText(
                    transaction.note
                        ?: ""
                )


                // ====================================================
                // CREATED DATE
                // ====================================================

                editingCreatedAt =
                    transaction.createdAt


                // ====================================================
                // BUTTON
                // ====================================================

                btnAdd.text =
                    if (
                        isExpense
                    ) {

                        "Update Expense"

                    } else {

                        "Update Income"
                    }
            }
        }
    }

    // ============================================================
// UPDATE TYPE UI
// ============================================================

    private fun updateTypeUI() {

        if (
            isExpense
        ) {

            tvExpense.setBackgroundResource(
                R.drawable.selector_selected_bg
            )

            tvExpense.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.neutral_0
                )
            )


            tvIncome.background =
                null


            tvIncome.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.neutral_500
                )
            )


            btnAdd.text =
                if (
                    isEditMode
                ) {

                    "Update Expense"

                } else {

                    "Add Expense"
                }

        } else {

            tvIncome.setBackgroundResource(
                R.drawable.selector_selected_bg
            )

            tvIncome.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.neutral_0
                )
            )


            tvExpense.background =
                null


            tvExpense.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.neutral_500
                )
            )


            btnAdd.text =
                if (
                    isEditMode
                ) {

                    "Update Income"

                } else {

                    "Add Income"
                }
        }
    }

    // ============================================================
// EXPENSE / INCOME SELECTOR
// ============================================================

    private fun setupTypeSelector() {

        tvExpense.setOnClickListener {

            if (
                !isExpense
            ) {

                isExpense =
                    true

                updateTypeUI()
            }
        }


        tvIncome.setOnClickListener {

            if (
                isExpense
            ) {

                isExpense =
                    false

                updateTypeUI()
            }
        }


        updateTypeUI()
    }


    // ============================================================
// CATEGORY OBSERVER
// ============================================================

    private fun setupCategoryChips() {

        lifecycleScope.launch {

            database
                .categoryDao()
                .observeAll(
                    currentUserId
                )
                .collectLatest { categories ->

                    categoryList =
                        categories


                    // =================================================
                    // FIX EDIT CATEGORY LABEL
                    // =================================================

                    if (
                        isEditMode &&
                        selectedCategoryId != null &&
                        selectedCategoryLabel.isEmpty()
                    ) {

                        selectedCategoryLabel =
                            categoryList
                                .firstOrNull {

                                    it.id ==
                                            selectedCategoryId
                                }
                                ?.label
                                ?: ""
                    }


                    updateSelectedCategoryText()

                    updateCategoryChips()
                }
        }
    }


    // ============================================================
    // UPDATE CATEGORY CHIPS
    // ============================================================

    private fun updateCategoryChips() {

        categoryChipContainer.removeAllViews()


        val displayCategories =
            mutableListOf<CategoryEntity>()


        // ========================================================
        // FIND SELECTED CATEGORY
        // ========================================================

        val selectedCategoryEntity =
            categoryList.firstOrNull { category ->

                category.id ==
                        selectedCategoryId
            }


        // ========================================================
        // PUT SELECTED CATEGORY FIRST
        // ========================================================

        if (selectedCategoryEntity != null) {

            displayCategories.add(
                selectedCategoryEntity
            )
        }


        // ========================================================
        // ADD OTHER CATEGORIES
        // ========================================================

        categoryList
            .filter { category ->

                category.id !=
                        selectedCategoryId
            }
            .take(
                if (
                    selectedCategoryEntity != null
                ) {
                    2
                } else {
                    3
                }
            )
            .forEach { category ->

                displayCategories.add(
                    category
                )
            }


        // ========================================================
        // ADD CATEGORY CHIPS
        // ========================================================

        displayCategories.forEach { category ->

            val chip =
                createCategoryChip(
                    category
                )

            categoryChipContainer.addView(
                chip
            )
        }


        // ========================================================
        // OTHER BUTTON
        // ========================================================

        val otherChip =
            createOtherChip()

        otherChip.setOnClickListener {

            openCategorySelector()
        }

        categoryChipContainer.addView(
            otherChip
        )
    }


    // ============================================================
    // CREATE CATEGORY CHIP
    // ============================================================

    private fun createCategoryChip(
        category: CategoryEntity
    ): LinearLayout {

        val chip =
            LinearLayout(this)

        chip.orientation =
            LinearLayout.HORIZONTAL

        chip.gravity =
            Gravity.CENTER_VERTICAL

        chip.setPadding(
            dpToPx(14),
            dpToPx(10),
            dpToPx(14),
            dpToPx(10)
        )

        chip.setBackgroundResource(
            R.drawable.chip_bg
        )


        val chipParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        chipParams.marginEnd =
            dpToPx(10)

        chip.layoutParams =
            chipParams


        // ========================================================
        // ICON
        // ========================================================

        val icon =
            ImageView(this)

        icon.layoutParams =
            LinearLayout.LayoutParams(
                dpToPx(22),
                dpToPx(22)
            )

        icon.setImageResource(
            category.iconRes
        )

        icon.setColorFilter(
            category.tintColor
        )


        // ========================================================
        // TEXT
        // ========================================================

        val text =
            TextView(this)

        val textParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        textParams.marginStart =
            dpToPx(8)

        text.layoutParams =
            textParams

        text.text =
            category.label

        text.textSize =
            14f

        text.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.primary_900
            )
        )


        // ========================================================
        // SELECTED STATE
        // ========================================================

        chip.alpha =
            if (
                selectedCategoryId == null
            ) {

                1.0f

            } else if (
                selectedCategoryId ==
                category.id
            ) {

                1.0f

            } else {

                0.65f
            }


        // ========================================================
        // CLICK
        // ========================================================

        chip.setOnClickListener {

            selectedCategoryId =
                category.id

            selectedCategoryLabel =
                category.label

            updateSelectedCategoryText()

            updateCategoryChips()
        }


        chip.addView(
            icon
        )

        chip.addView(
            text
        )

        return chip
    }


    // ============================================================
    // CREATE OTHER CHIP
    // ============================================================

    private fun createOtherChip(): LinearLayout {

        val chip =
            LinearLayout(this)

        chip.orientation =
            LinearLayout.HORIZONTAL

        chip.gravity =
            Gravity.CENTER_VERTICAL

        chip.setPadding(
            dpToPx(14),
            dpToPx(10),
            dpToPx(14),
            dpToPx(10)
        )

        chip.setBackgroundResource(
            R.drawable.chip_bg
        )


        val chipParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        chipParams.marginEnd =
            dpToPx(10)

        chip.layoutParams =
            chipParams


        val icon =
            TextView(this)

        icon.text =
            "⋯"

        icon.textSize =
            22f


        val text =
            TextView(this)

        val textParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        textParams.marginStart =
            dpToPx(8)

        text.layoutParams =
            textParams

        text.text =
            "Other"

        text.textSize =
            14f

        text.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.primary_900
            )
        )


        val firstThreeIds =
            categoryList
                .take(3)
                .map {
                    it.id
                }


        chip.alpha =
            when {

                selectedCategoryId == null ->
                    1.0f

                selectedCategoryId !in firstThreeIds ->
                    1.0f

                else ->
                    0.65f
            }


        chip.addView(
            icon
        )

        chip.addView(
            text
        )

        return chip
    }


    // ============================================================
    // SELECTED CATEGORY TEXT
    // ============================================================

    private fun updateSelectedCategoryText() {

        tvSelectedCategory.text =
            if (
                selectedCategoryId == null
            ) {

                "No category selected"

            } else {

                "Selected: $selectedCategoryLabel"
            }
    }


    // ============================================================
    // OPEN CATEGORY SELECTOR
    // ============================================================

    private fun openCategorySelector() {

        val intent =
            Intent(
                this,
                SelectCategoryActivity::class.java
            )

        intent.putExtra(
            SelectCategoryActivity.EXTRA_SELECT_MODE,
            true
        )

        intent.putExtra(
            SelectCategoryActivity.EXTRA_USER_ID,
            currentUserId
        )

        selectCategoryLauncher.launch(
            intent
        )
    }


    // ============================================================
// PAYMENT / ACCOUNT OBSERVER
// ============================================================

    private fun setupPaymentChips() {

        lifecycleScope.launch {

            database
                .accountDao()
                .getAccountsForUser(
                    currentUserId
                )
                .collectLatest { accounts ->

                    accountList =
                        accounts

                    // =============================================
                    // If selected account was deleted,
                    // clear the selection.
                    // =============================================

                    if (
                        selectedAccountId != null &&
                        accountList.none {
                            it.accountId ==
                                    selectedAccountId
                        }
                    ) {

                        selectedAccountId =
                            null
                    }


                    updatePaymentChips()
                }
        }
    }

    // ============================================================
// UPDATE PAYMENT / ACCOUNT CHIPS
// ============================================================

    private fun updatePaymentChips() {

        paymentChipContainer.removeAllViews()


        // ========================================================
        // NO ACCOUNTS
        // ========================================================

        if (
            accountList.isEmpty()
        ) {

            val text =
                TextView(this)

            text.text =
                "No payment accounts available"

            text.textSize =
                14f

            text.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.neutral_500
                )
            )

            paymentChipContainer.addView(
                text
            )

            return
        }


        // ========================================================
        // CREATE ACCOUNT CHIPS
        // ========================================================

        accountList.forEach { account ->

            val chip =
                createPaymentChip(
                    account
                )

            paymentChipContainer.addView(
                chip
            )
        }
    }


    // ============================================================
// CREATE PAYMENT / ACCOUNT CHIP
// ============================================================

    private fun createPaymentChip(
        account: Account
    ): LinearLayout {

        val chip =
            LinearLayout(this)

        chip.orientation =
            LinearLayout.HORIZONTAL

        chip.gravity =
            Gravity.CENTER_VERTICAL

        chip.setPadding(
            dpToPx(14),
            dpToPx(10),
            dpToPx(14),
            dpToPx(10)
        )

        chip.setBackgroundResource(
            R.drawable.chip_bg
        )


        // ========================================================
        // LAYOUT
        // ========================================================

        val chipParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        chipParams.marginEnd =
            dpToPx(10)

        chip.layoutParams =
            chipParams


        // ========================================================
        // ICON
        // ========================================================

        val icon =
            TextView(this)

        icon.text =
            getAccountEmoji(
                account.name
            )

        icon.textSize =
            16f


        // ========================================================
        // ACCOUNT NAME
        // ========================================================

        val text =
            TextView(this)

        val textParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        textParams.marginStart =
            dpToPx(8)

        text.layoutParams =
            textParams

        text.text =
            account.name

        text.textSize =
            14f

        text.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.primary_900
            )
        )


        // ========================================================
        // SELECTED STATE
        // ========================================================

        chip.alpha =
            if (
                selectedAccountId == null
            ) {

                1.0f

            } else if (
                selectedAccountId ==
                account.accountId
            ) {

                1.0f

            } else {

                0.5f
            }


        // ========================================================
        // CLICK
        // ========================================================

        chip.setOnClickListener {

            selectedAccountId =
                account.accountId

            updatePaymentChips()
        }


        chip.addView(
            icon
        )

        chip.addView(
            text
        )


        return chip
    }



    // ============================================================
// ACCOUNT ICON
// ============================================================

    private fun getAccountEmoji(
        accountName: String
    ): String {

        val name =
            accountName.lowercase()


        return when {

            name.contains("cash") ->
                "💵"

            name.contains("card") ||
                    name.contains("visa") ||
                    name.contains("master") ->
                "💳"

            name.contains("bank") ->
                "🏦"

            name.contains("wallet") ->
                "👛"

            else ->
                "💰"
        }
    }






    // ============================================================
    // CUSTOM KEYPAD
    // ============================================================

    private fun setupCustomKeypad() {

        val numKeys =
            listOf(
                R.id.key0 to "0",
                R.id.key1 to "1",
                R.id.key2 to "2",
                R.id.key3 to "3",
                R.id.key4 to "4",
                R.id.key5 to "5",
                R.id.key6 to "6",
                R.id.key7 to "7",
                R.id.key8 to "8",
                R.id.key9 to "9"
            )


        numKeys.forEach { (id, digit) ->

            findViewById<View>(
                id
            ).setOnClickListener {

                if (
                    currentAmountStr.length < 9
                ) {

                    currentAmountStr +=
                        digit

                    updateAmountDisplay()
                }
            }
        }


        findViewById<View>(
            R.id.keyDot
        ).setOnClickListener {

            if (
                !currentAmountStr.contains(".") &&
                currentAmountStr.isNotEmpty()
            ) {

                currentAmountStr +=
                    "."

                updateAmountDisplay()
            }
        }


        findViewById<View>(
            R.id.keyDel
        ).setOnClickListener {

            if (
                currentAmountStr.isNotEmpty()
            ) {

                currentAmountStr =
                    currentAmountStr.dropLast(1)

                updateAmountDisplay()
            }
        }
    }


    // ============================================================
    // AMOUNT DISPLAY
    // ============================================================

    private fun updateAmountDisplay() {

        tvAmount.text =
            if (
                currentAmountStr.isEmpty()
            ) {

                "$userCurrency 0"

            } else {

                "$userCurrency $currentAmountStr"
            }
    }


    // ============================================================
    // SAVE TRANSACTION
    // ============================================================

    private fun saveTransactionAndNotify() {

        val amount =
            currentAmountStr.toDoubleOrNull()


        // ========================================================
        // VALIDATE AMOUNT
        // ========================================================

        if (
            amount == null ||
            amount <= 0.0
        ) {

            Toast.makeText(
                this,
                "Please enter an amount greater than 0",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // ========================================================
        // VALIDATE CATEGORY
        // ========================================================

        if (
            selectedCategoryId == null
        ) {

            Toast.makeText(
                this,
                "Please select a category",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // ========================================================
        // VALIDATE ACCOUNT
        // ========================================================

        // No accounts have been created yet
        if (
            accountList.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Please add a payment account first",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // Accounts exist, but none is selected
        if (
            selectedAccountId == null
        ) {

            Toast.makeText(
                this,
                "Please select a payment account",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // ========================================================
        // VALIDATE USER
        // ========================================================

        if (
            currentUserId == -1
        ) {

            Toast.makeText(
                this,
                "Invalid user session",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        val note =
            etNote.text
                .toString()
                .trim()


        val title =
            if (
                note.isNotEmpty()
            ) {

                note

            } else {

                selectedCategoryLabel
            }


        val txnType =
            if (
                isExpense
            ) {

                "EXPENSE"

            } else {

                "INCOME"
            }


        lifecycleScope.launch(
            Dispatchers.IO
        ) {

            try {

                val transaction =
                    Transaction(

                        transactionId =
                            0,

                        userId =
                            currentUserId,

                        title =
                            title,

                        amount =
                            amount,

                        type =
                            txnType,

                        categoryId =
                            selectedCategoryId,

                        accountId =
                            selectedAccountId,

                        note =
                            note.ifEmpty {
                                null
                            },

                        createdAt =
                            System.currentTimeMillis()
                    )


                database
                    .transactionDao()
                    .insertTransaction(
                        transaction
                    )


                // =================================================
                // NOTIFICATION
                // =================================================

                val notiTitle: String

                val notiMessage: String

                val notiType: String


                if (isExpense) {

                    if (amount >= 500.0) {

                        notiTitle =
                            "Large transaction detected"

                        notiMessage =
                            "A $userCurrency $amount charge was made for $title today."

                        notiType =
                            "TRANSACTION"

                    } else {

                        notiTitle =
                            "Expense Added"

                        notiMessage =
                            "You spent $userCurrency $amount on $title ($selectedCategoryLabel)."

                        notiType =
                            "EXPENSE"
                    }

                } else {

                    notiTitle =
                        "Income Received"

                    notiMessage =
                        "You received $userCurrency $amount from $title."

                    notiType =
                        "INCOME"
                }


                val newNotification =
                    Notification(

                        notificationId =
                            0,

                        userId =
                            currentUserId,

                        title =
                            notiTitle,

                        message =
                            notiMessage,

                        type =
                            notiType,

                        referenceType =
                            txnType,

                        referenceId =
                            null,

                        isRead =
                            false,

                        timeAgo =
                            null,

                        createdAt =
                            System.currentTimeMillis()
                                .toString()
                    )


                database
                    .notificationDao()
                    .insertNotification(
                        newNotification
                    )


                withContext(
                    Dispatchers.Main
                ) {

                    Toast.makeText(
                        this@AddTransactionActivity,

                        if (isExpense) {
                            "Expense saved successfully!"
                        } else {
                            "Income saved successfully!"
                        },

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
                        this@AddTransactionActivity,

                        "Error saving transaction: ${e.localizedMessage}",

                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    // ============================================================
    // DP → PX
    // ============================================================

    private fun dpToPx(
        dp: Int
    ): Int {

        return (
                dp *
                        resources
                            .displayMetrics
                            .density
                ).toInt()
    }
}

