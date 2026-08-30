package com.example.walletwise.transactions

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
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
import java.util.UUID
import com.example.walletwise.category.getCategoryIconRes

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
    private var isTransfer = false
    private var currentAmountStr = ""

    private var editingTransactionId: Int = -1
    private var isEditMode = false

    private var editingCreatedAt: Long = 0L
    private var editingTransferGroupId: String? = null

    // ============================================================
    // CATEGORY
    // ============================================================

    private var selectedCategoryId: Long? = null
    private var selectedCategoryLabel = ""
    private var selectedCategoryIconRes: Int = 0


    private var categoryList: List<CategoryEntity> = emptyList()

    private lateinit var categoryChipContainer: LinearLayout
    private lateinit var categoryScroll: HorizontalScrollView
    private lateinit var tvSelectedCategory: TextView

    // ============================================================
    // SOURCE ACCOUNT
    // ============================================================

    private var selectedAccountId: Int? = null
    private var accountList: List<Account> = emptyList()

    private lateinit var paymentChipContainer: LinearLayout

    // ============================================================
    // DESTINATION ACCOUNT
    // ============================================================

    private var selectedDestinationAccountId: Int? = null

    private lateinit var destinationAccountChipContainer: LinearLayout
    private lateinit var destinationAccountScroll: HorizontalScrollView

    // ============================================================
    // VIEWS
    // ============================================================

    private lateinit var tvAmount: TextView
    private lateinit var tvExpense: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvTransfer: TextView

    private lateinit var btnAdd: MaterialButton
    private lateinit var etNote: EditText
    private lateinit var tvTitle: TextView
    private lateinit var tvDestinationLabel: TextView

    // ============================================================
    // INTENT EXTRA
    // ============================================================

    companion object {

        const val EXTRA_USER_ID = "USER_ID"
        const val EXTRA_TRANSACTION_ID = "TRANSACTION_ID"
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

                val categoryIcon =
                    result.data?.getIntExtra(
                        SelectCategoryActivity.RESULT_CATEGORY_ICON,
                        0
                    ) ?: 0

                if (
                    categoryId != -1L &&
                    !categoryLabel.isNullOrEmpty()
                ) {

                    selectedCategoryId =
                        categoryId

                    selectedCategoryLabel =
                        categoryLabel

                    selectedCategoryIconRes =
                        categoryIcon

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

        if (currentUserId == -1) {

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

        tvTransfer =
            findViewById(
                R.id.tvTransfer
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

        tvDestinationLabel =
            findViewById(
                R.id.tvDestinationLabel
            )

        destinationAccountChipContainer =
            findViewById(
                R.id.destinationAccountChipContainer
            )

        // IMPORTANT:
        // This is the parent HorizontalScrollView.
        // The previous version only changed the child's visibility.
        destinationAccountScroll =
            findViewById(
                R.id.destinationAccountScroll
            )

        categoryScroll =
            findViewById(
                R.id.categoryScroll
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

        // ========================================================
        // EDIT / ADD
        // ========================================================

        if (isEditMode) {

            tvTitle.text =
                "Edit Transaction"

            loadTransactionForEditing()

        } else {

            tvTitle.text =
                "Add Expense"
        }

        // ========================================================
        // SAVE / UPDATE
        // ========================================================

        btnAdd.setOnClickListener {

            if (isEditMode) {

                if (isTransfer) {

                    updateTransfer()

                } else {

                    updateTransaction()
                }

            } else {

                if (isTransfer) {

                    saveTransfer()

                } else {

                    saveTransactionAndNotify()
                }
            }
        }
    }

    // ============================================================
    // LOAD USER CURRENCY
    // ============================================================

    private fun loadUserCurrency() {

        lifecycleScope.launch {

            database
                .userDao()
                .getUserById(currentUserId)
                .collect { user ->

                    user ?: return@collect

                    userCurrency =
                        user.currency

                    updateAmountDisplay()
                }
        }
    }

    // ============================================================
    // TYPE SELECTOR
    // ============================================================

    private fun setupTypeSelector() {

        tvExpense.setOnClickListener {

            isExpense = true
            isTransfer = false

            updateTypeUI()
        }

        tvIncome.setOnClickListener {

            isExpense = false
            isTransfer = false

            updateTypeUI()
        }

        tvTransfer.setOnClickListener {

            isExpense = false
            isTransfer = true

            updateTypeUI()
        }

        updateTypeUI()
    }

    // ============================================================
    // TYPE UI
    // ============================================================

    private fun updateTypeUI() {

        tvExpense.background = null
        tvIncome.background = null
        tvTransfer.background = null

        tvExpense.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.neutral_500
            )
        )

        tvIncome.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.neutral_500
            )
        )

        tvTransfer.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.neutral_500
            )
        )

        when {

            // ====================================================
            // TRANSFER
            // ====================================================

            isTransfer -> {

                tvTransfer.setBackgroundResource(
                    R.drawable.selector_selected_bg
                )

                tvTransfer.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.neutral_0
                    )
                )

                tvDestinationLabel.visibility =
                    View.VISIBLE

                // IMPORTANT:
                // Show the HorizontalScrollView, not only
                // destinationAccountChipContainer.
                destinationAccountScroll.visibility =
                    View.VISIBLE

                categoryScroll.visibility =
                    View.GONE

                tvSelectedCategory.visibility =
                    View.GONE

                btnAdd.text =
                    if (isEditMode) {
                        "Update Transfer"
                    } else {
                        "Transfer Money"
                    }

                updateDestinationAccountChips()
            }

            // ====================================================
            // EXPENSE
            // ====================================================

            isExpense -> {

                tvExpense.setBackgroundResource(
                    R.drawable.selector_selected_bg
                )

                tvExpense.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.neutral_0
                    )
                )

                tvDestinationLabel.visibility =
                    View.GONE

                destinationAccountScroll.visibility =
                    View.GONE

                categoryScroll.visibility =
                    View.VISIBLE

                tvSelectedCategory.visibility =
                    View.VISIBLE

                btnAdd.text =
                    if (isEditMode) {
                        "Update Expense"
                    } else {
                        "Add Expense"
                    }
            }

            // ====================================================
            // INCOME
            // ====================================================

            else -> {

                tvIncome.setBackgroundResource(
                    R.drawable.selector_selected_bg
                )

                tvIncome.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.neutral_0
                    )
                )

                tvDestinationLabel.visibility =
                    View.GONE

                destinationAccountScroll.visibility =
                    View.GONE

                categoryChipContainer.visibility =
                    View.VISIBLE

                categoryScroll.visibility =
                    View.VISIBLE

                tvSelectedCategory.visibility =
                    View.VISIBLE

                btnAdd.text =
                    if (isEditMode) {
                        "Update Income"
                    } else {
                        "Add Income"
                    }
            }
        }
    }

    // ============================================================
    // SAVE TRANSFER
    // ============================================================

    private fun saveTransfer() {

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
        // VALIDATE SOURCE
        // ========================================================

        if (selectedAccountId == null) {

            Toast.makeText(
                this,
                "Please select the source account",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // ========================================================
        // VALIDATE DESTINATION
        // ========================================================

        if (selectedDestinationAccountId == null) {

            Toast.makeText(
                this,
                "Please select the destination account",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // ========================================================
        // SAME ACCOUNT
        // ========================================================

        if (
            selectedAccountId ==
            selectedDestinationAccountId
        ) {

            Toast.makeText(
                this,
                "Source and destination accounts must be different",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val sourceAccount =
                    database
                        .accountDao()
                        .getAccountBalanceById(
                            selectedAccountId!!,
                            currentUserId
                        )

                if (sourceAccount == null) {

                    withContext(Dispatchers.Main) {

                        Toast.makeText(
                            this@AddTransactionActivity,
                            "Source account not found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    return@launch
                }

                // =================================================
                // INSUFFICIENT BALANCE
                // =================================================

                if (
                    sourceAccount.currentBalance < amount
                ) {

                    withContext(Dispatchers.Main) {

                        Toast.makeText(
                            this@AddTransactionActivity,
                            "Insufficient balance in ${sourceAccount.name}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    return@launch
                }

                val destinationAccount =
                    database
                        .accountDao()
                        .getAccountById(
                            selectedDestinationAccountId!!,
                            currentUserId
                        )

                if (destinationAccount == null) {

                    withContext(Dispatchers.Main) {

                        Toast.makeText(
                            this@AddTransactionActivity,
                            "Destination account not found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    return@launch
                }

                val note =
                    etNote.text
                        .toString()
                        .trim()

                val now =
                    System.currentTimeMillis()

                // =================================================
                // SAME GROUP ID FOR BOTH TRANSACTIONS
                // =================================================

                val transferGroupId =
                    UUID.randomUUID().toString()

                // =================================================
                // SOURCE TRANSACTION
                // =================================================

                val transferOut =
                    Transaction(

                        transactionId = 0,

                        userId =
                            currentUserId,

                        title =
                            if (note.isNotEmpty()) {
                                note
                            } else {
                                "Transfer to ${destinationAccount.name}"
                            },

                        amount =
                            amount,

                        type =
                            "TRANSFER_OUT",

                        categoryId =
                            null,

                        accountId =
                            selectedAccountId,

                        note =
                            note.ifEmpty {
                                "Transfer to ${destinationAccount.name}"
                            },

                        createdAt =
                            now,

                        transferGroupId =
                            transferGroupId
                    )

                // =================================================
                // DESTINATION TRANSACTION
                // =================================================

                val transferIn =
                    Transaction(

                        transactionId = 0,

                        userId =
                            currentUserId,

                        title =
                            "Transfer from ${sourceAccount.name}",

                        amount =
                            amount,

                        type =
                            "TRANSFER_IN",

                        categoryId =
                            null,

                        accountId =
                            selectedDestinationAccountId,

                        note =
                            note.ifEmpty {
                                "Transfer from ${sourceAccount.name}"
                            },

                        createdAt =
                            now,

                        transferGroupId =
                            transferGroupId
                    )

                // =================================================
                // INSERT BOTH
                // =================================================

                database
                    .transactionDao()
                    .insertTransfer(
                        transferOut,
                        transferIn
                    )

                // =================================================
                // NOTIFICATION
                // =================================================

                val notification =
                    Notification(

                        notificationId = 0,

                        userId =
                            currentUserId,

                        title =
                            "Money Transferred",

                        message =
                            "$userCurrency $amount transferred from ${sourceAccount.name} to ${destinationAccount.name}.",

                        type =
                            "TRANSFER",

                        referenceType =
                            "TRANSFER",

                        referenceId =
                            null,

                        isRead =
                            false,

                        timeAgo =
                            null,

                        createdAt =
                            now.toString()
                    )

                database
                    .notificationDao()
                    .insertNotification(
                        notification
                    )

                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        this@AddTransactionActivity,
                        "Transfer completed successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }

            } catch (e: Exception) {

                e.printStackTrace()

                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        this@AddTransactionActivity,
                        "Error transferring money: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ============================================================
    // PAYMENT / SOURCE ACCOUNT OBSERVER
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

                    if (
                        selectedDestinationAccountId != null &&
                        accountList.none {
                            it.accountId ==
                                    selectedDestinationAccountId
                        }
                    ) {

                        selectedDestinationAccountId =
                            null
                    }

                    updatePaymentChips()

                    updateDestinationAccountChips()
                }
        }
    }

    // ============================================================
    // SOURCE ACCOUNT CHIPS
    // ============================================================

    private fun updatePaymentChips() {

        paymentChipContainer.removeAllViews()

        if (accountList.isEmpty()) {

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

        accountList.forEach { account ->

            paymentChipContainer.addView(
                createSourceAccountChip(
                    account
                )
            )
        }
    }

    // ============================================================
    // SOURCE ACCOUNT CHIP
    // ============================================================

    private fun createSourceAccountChip(
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

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.marginEnd =
            dpToPx(10)

        chip.layoutParams =
            params

        val icon =
            TextView(this)

        icon.text =
            getAccountEmoji(
                account.name
            )

        icon.textSize =
            16f

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

        chip.alpha =
            if (
                selectedAccountId == null ||
                selectedAccountId == account.accountId
            ) {
                1.0f
            } else {
                0.5f
            }

        chip.setOnClickListener {

            selectedAccountId =
                account.accountId

            // If the selected source is the same as the
            // currently selected destination, clear destination.
            if (
                selectedDestinationAccountId ==
                selectedAccountId
            ) {

                selectedDestinationAccountId =
                    null
            }

            updatePaymentChips()

            updateDestinationAccountChips()
        }

        chip.addView(icon)
        chip.addView(text)

        return chip
    }

    // ============================================================
    // DESTINATION ACCOUNT CHIPS
    // ============================================================

    private fun updateDestinationAccountChips() {

        destinationAccountChipContainer.removeAllViews()

        if (accountList.isEmpty()) {
            return
        }

        accountList
            .filter {
                it.accountId != selectedAccountId
            }
            .forEach { account ->

                destinationAccountChipContainer.addView(
                    createDestinationAccountChip(
                        account
                    )
                )
            }
    }

    // ============================================================
    // DESTINATION ACCOUNT CHIP
    // ============================================================

    private fun createDestinationAccountChip(
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

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.marginEnd =
            dpToPx(10)

        chip.layoutParams =
            params

        val icon =
            TextView(this)

        icon.text =
            getAccountEmoji(
                account.name
            )

        icon.textSize =
            16f

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

        chip.alpha =
            if (
                selectedDestinationAccountId ==
                account.accountId
            ) {
                1.0f
            } else {
                0.65f
            }

        chip.setOnClickListener {

            selectedDestinationAccountId =
                account.accountId

            updateDestinationAccountChips()
        }

        chip.addView(icon)
        chip.addView(text)

        return chip
    }

    // ============================================================
    // ACCOUNT EMOJI
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

            name.contains("bank") ||
                    name.contains("kbz") ->
                "🏦"

            name.contains("wallet") ||
                    name.contains("wave") ->
                "👛"

            else ->
                "💰"
        }
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
    // CATEGORY CHIPS
    // ============================================================
    private fun updateCategoryChips() {

        categoryChipContainer.removeAllViews()

        val displayCategories =
            mutableListOf<CategoryEntity>()

        // ========================================================
        // FIND SELECTED CATEGORY
        // ========================================================

        val selectedCategoryEntity =
            categoryList.firstOrNull {
                it.id == selectedCategoryId
            }

        // ========================================================
        // ADD SELECTED CATEGORY FIRST
        // ========================================================

        if (selectedCategoryEntity != null) {

            displayCategories.add(
                selectedCategoryEntity
            )

            selectedCategoryLabel =
                selectedCategoryEntity.label

            selectedCategoryIconRes =
                getCategoryIconRes(
                    selectedCategoryEntity.iconName
                )
        }

        // ========================================================
        // ADD OTHER CATEGORIES
        // ========================================================

        categoryList
            .filter {
                it.id != selectedCategoryId
            }
            .take(
                if (selectedCategoryEntity != null) {
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
        // CREATE CATEGORY CHIPS
        // ========================================================

        displayCategories.forEach { category ->

            categoryChipContainer.addView(
                createCategoryChip(
                    category
                )
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
    // CATEGORY CHIP
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

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.marginEnd =
            dpToPx(10)

        chip.layoutParams =
            params

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

        val iconRes =
            getCategoryIconRes(
                category.iconName
            )

        if (iconRes != 0) {

            icon.setImageResource(
                iconRes
            )
        }

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
                selectedCategoryId == null ||
                selectedCategoryId == category.id
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

            selectedCategoryIconRes =
                getCategoryIconRes(
                    category.iconName
                )

            updateSelectedCategoryText()

            updateCategoryChips()
        }

        chip.addView(icon)
        chip.addView(text)

        return chip
    }

    // ============================================================
    // OTHER CATEGORY CHIP
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

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.marginEnd =
            dpToPx(10)

        chip.layoutParams =
            params

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

        chip.addView(icon)
        chip.addView(text)

        return chip
    }

    // ============================================================
    // SELECTED CATEGORY
    // ============================================================

    private fun updateSelectedCategoryText() {

        tvSelectedCategory.text =
            if (selectedCategoryId == null) {

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
    // LOAD TRANSACTION FOR EDITING
    // ============================================================

    private fun loadTransactionForEditing() {

        lifecycleScope.launch(Dispatchers.IO) {

            val transaction =
                database
                    .transactionDao()
                    .getTransactionById(
                        editingTransactionId,
                        currentUserId
                    )

            if (transaction == null) {

                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        this@AddTransactionActivity,
                        "Transaction not found",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }

                return@launch
            }

            // ====================================================
            // LOAD BASIC TRANSACTION DATA
            // ====================================================

            withContext(Dispatchers.Main) {

                currentAmountStr =
                    transaction.amount.toString()

                if (
                    currentAmountStr.endsWith(".0")
                ) {

                    currentAmountStr =
                        currentAmountStr
                            .removeSuffix(".0")
                }

                updateAmountDisplay()

                editingCreatedAt =
                    transaction.createdAt

                etNote.setText(
                    transaction.note ?: ""
                )
            }

            // ====================================================
            // NORMAL TRANSACTION
            // ====================================================

            if (
                transaction.type != "TRANSFER_OUT" &&
                transaction.type != "TRANSFER_IN"
            ) {

                withContext(Dispatchers.Main) {

                    isTransfer = false

                    isExpense =
                        transaction.type == "EXPENSE"

                    selectedCategoryId =
                        transaction.categoryId

                    val category =
                        categoryList.firstOrNull {
                            it.id == selectedCategoryId
                        }

                    selectedCategoryLabel =
                        category?.label
                            ?: ""

                    selectedCategoryIconRes =
                        category?.let {

                            getCategoryIconRes(
                                it.iconName
                            )

                        } ?: 0

                    selectedAccountId =
                        transaction.accountId

                    updateTypeUI()

                    updateSelectedCategoryText()
                    updateCategoryChips()
                    updatePaymentChips()
                }

                return@launch
            }

            // ====================================================
            // TRANSFER EDIT
            //
            // Load the COMPLETE transfer pair using the group ID.
            // This allows editing from either TRANSFER_OUT or
            // TRANSFER_IN.
            // ====================================================

            val groupId =
                transaction.transferGroupId

            if (groupId.isNullOrEmpty()) {

                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        this@AddTransactionActivity,
                        "This transfer has no transfer group ID and cannot be edited safely.",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                }

                return@launch
            }

            val transferTransactions =
                database
                    .transactionDao()
                    .getTransactionsByTransferGroupId(
                        groupId,
                        currentUserId
                    )

            val transferOut =
                transferTransactions
                    .firstOrNull {
                        it.type == "TRANSFER_OUT"
                    }

            val transferIn =
                transferTransactions
                    .firstOrNull {
                        it.type == "TRANSFER_IN"
                    }

            if (
                transferOut == null ||
                transferIn == null
            ) {

                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        this@AddTransactionActivity,
                        "Complete transfer pair not found.",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                }

                return@launch
            }

            // ====================================================
            // LOAD TRANSFER VALUES
            // ====================================================

            withContext(Dispatchers.Main) {

                isExpense = false
                isTransfer = true

                editingTransferGroupId =
                    groupId

                // SOURCE
                selectedAccountId =
                    transferOut.accountId

                // DESTINATION
                selectedDestinationAccountId =
                    transferIn.accountId

                // Use the transfer amount from OUT.
                currentAmountStr =
                    transferOut.amount.toString()

                if (
                    currentAmountStr.endsWith(".0")
                ) {

                    currentAmountStr =
                        currentAmountStr
                            .removeSuffix(".0")
                }

                updateAmountDisplay()

                // Prefer the user's note.
                // If empty, use the transfer OUT note.
                etNote.setText(
                    transferOut.note
                        ?.takeIf { it.isNotBlank() }
                        ?: ""
                )

                updateTypeUI()

                updateDestinationAccountChips()

                updatePaymentChips()
            }
        }
    }

    // ============================================================
    // UPDATE NORMAL TRANSACTION
    // ============================================================

    private fun updateTransaction() {

        val amount =
            currentAmountStr.toDoubleOrNull()

        if (
            amount == null ||
            amount <= 0
        ) {

            Toast.makeText(
                this,
                "Please enter an amount greater than 0",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (selectedCategoryId == null) {

            Toast.makeText(
                this,
                "Please select a category",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (selectedAccountId == null) {

            Toast.makeText(
                this,
                "Please select a payment account",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val note =
            etNote.text
                .toString()
                .trim()

        val title =
            if (note.isNotEmpty()) {
                note
            } else {
                selectedCategoryLabel
            }

        val txnType =
            if (isExpense) {
                "EXPENSE"
            } else {
                "INCOME"
            }

        lifecycleScope.launch(Dispatchers.IO) {

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

                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        this@AddTransactionActivity,
                        "Transaction updated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }

            } catch (e: Exception) {

                e.printStackTrace()

                withContext(Dispatchers.Main) {

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
    // UPDATE TRANSFER
    // ============================================================

    private fun updateTransfer() {

        val amount =
            currentAmountStr.toDoubleOrNull()

        if (
            amount == null ||
            amount <= 0
        ) {

            Toast.makeText(
                this,
                "Please enter an amount greater than 0",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (selectedAccountId == null) {

            Toast.makeText(
                this,
                "Please select the source account",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (selectedDestinationAccountId == null) {

            Toast.makeText(
                this,
                "Please select the destination account",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (
            selectedAccountId ==
            selectedDestinationAccountId
        ) {

            Toast.makeText(
                this,
                "Source and destination accounts must be different",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val groupId =
            editingTransferGroupId

        if (groupId.isNullOrEmpty()) {

            Toast.makeText(
                this,
                "This transfer has no transfer group ID and cannot be safely edited.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val note =
            etNote.text
                .toString()
                .trim()

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                // =================================================
                // LOAD COMPLETE TRANSFER PAIR
                // =================================================

                val transferTransactions =
                    database
                        .transactionDao()
                        .getTransactionsByTransferGroupId(
                            groupId,
                            currentUserId
                        )

                val transferOut =
                    transferTransactions
                        .firstOrNull {
                            it.type == "TRANSFER_OUT"
                        }

                val transferIn =
                    transferTransactions
                        .firstOrNull {
                            it.type == "TRANSFER_IN"
                        }

                if (
                    transferOut == null ||
                    transferIn == null
                ) {

                    withContext(Dispatchers.Main) {

                        Toast.makeText(
                            this@AddTransactionActivity,
                            "Complete transfer pair not found.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    return@launch
                }

                // =================================================
                // CHECK SOURCE BALANCE
                //
                // If the old TRANSFER_OUT came from the same
                // source account, add its old amount back because
                // it is already reflected in the current balance.
                // =================================================

                val sourceAccount =
                    database
                        .accountDao()
                        .getAccountBalanceById(
                            selectedAccountId!!,
                            currentUserId
                        )

                if (sourceAccount == null) {

                    withContext(Dispatchers.Main) {

                        Toast.makeText(
                            this@AddTransactionActivity,
                            "Source account not found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    return@launch
                }

                var availableBalance =
                    sourceAccount.currentBalance

                if (
                    transferOut.accountId ==
                    selectedAccountId
                ) {

                    availableBalance +=
                        transferOut.amount
                }

                if (
                    availableBalance < amount
                ) {

                    withContext(Dispatchers.Main) {

                        Toast.makeText(
                            this@AddTransactionActivity,
                            "Insufficient balance in ${sourceAccount.name}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    return@launch
                }

                // =================================================
                // DESTINATION ACCOUNT
                // =================================================

                val destinationAccount =
                    database
                        .accountDao()
                        .getAccountById(
                            selectedDestinationAccountId!!,
                            currentUserId
                        )

                if (destinationAccount == null) {

                    withContext(Dispatchers.Main) {

                        Toast.makeText(
                            this@AddTransactionActivity,
                            "Destination account not found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    return@launch
                }

                // =================================================
                // CREATE UPDATED OUT
                // =================================================

                val newTransferOut =
                    transferOut.copy(

                        accountId =
                            selectedAccountId,

                        amount =
                            amount,

                        title =
                            if (note.isNotEmpty()) {
                                note
                            } else {
                                "Transfer to ${destinationAccount.name}"
                            },

                        note =
                            note.ifEmpty {
                                "Transfer to ${destinationAccount.name}"
                            }
                    )

                // =================================================
                // CREATE UPDATED IN
                // =================================================

                val newTransferIn =
                    transferIn.copy(

                        accountId =
                            selectedDestinationAccountId,

                        amount =
                            amount,

                        title =
                            "Transfer from ${sourceAccount.name}",

                        note =
                            note.ifEmpty {
                                "Transfer from ${sourceAccount.name}"
                            }
                    )

                // =================================================
                // UPDATE BOTH ATOMICALLY
                // =================================================

                database
                    .transactionDao()
                    .updateTransfer(
                        newTransferOut,
                        newTransferIn
                    )

                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        this@AddTransactionActivity,
                        "Transfer updated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }

            } catch (e: Exception) {

                e.printStackTrace()

                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        this@AddTransactionActivity,
                        "Error updating transfer: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ============================================================
    // SAVE EXPENSE / INCOME
    // ============================================================

    private fun saveTransactionAndNotify() {

        val amount =
            currentAmountStr.toDoubleOrNull()

        if (
            amount == null ||
            amount <= 0
        ) {

            Toast.makeText(
                this,
                "Please enter an amount greater than 0",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (selectedCategoryId == null) {

            Toast.makeText(
                this,
                "Please select a category",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (selectedAccountId == null) {

            Toast.makeText(
                this,
                "Please select a payment account",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val note =
            etNote.text
                .toString()
                .trim()

        val title =
            if (note.isNotEmpty()) {
                note
            } else {
                selectedCategoryLabel
            }

        val txnType =
            if (isExpense) {
                "EXPENSE"
            } else {
                "INCOME"
            }

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val transaction =
                    Transaction(

                        transactionId = 0,

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
                            System.currentTimeMillis(),

                        transferGroupId =
                            null
                    )

                database
                    .transactionDao()
                    .insertTransaction(
                        transaction
                    )

                val notiTitle: String
                val notiMessage: String
                val notiType: String

                if (isExpense) {

                    if (amount >= 500) {

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

                val notification =
                    Notification(

                        notificationId = 0,

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
                        notification
                    )

                withContext(Dispatchers.Main) {

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

                withContext(Dispatchers.Main) {

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

                currentAmountStr += "."

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
            if (currentAmountStr.isEmpty()) {

                "$userCurrency 0"

            } else {

                "$userCurrency $currentAmountStr"
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

