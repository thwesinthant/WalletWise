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

    private var selectedCategory = ""

    private var selectedPaymentMethod = "Cash"


    // ============================================================
    // CATEGORY
    // ============================================================

    private var categoryList: List<CategoryEntity> = emptyList()

    private lateinit var categoryChipContainer: LinearLayout

    private lateinit var tvSelectedCategory: TextView


    // ============================================================
    // VIEWS
    // ============================================================

    private lateinit var tvAmount: TextView

    private lateinit var tvExpense: TextView

    private lateinit var tvIncome: TextView

    private lateinit var btnAdd: MaterialButton

    private lateinit var etNote: EditText


    // ============================================================
    // INTENT EXTRA
    // ============================================================

    companion object {

        const val EXTRA_USER_ID = "USER_ID"
    }


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

                val categoryLabel =
                    result.data?.getStringExtra(
                        SelectCategoryActivity.RESULT_CATEGORY_LABEL
                    )

                if (!categoryLabel.isNullOrEmpty()) {

                    selectedCategory = categoryLabel

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


        // ========================================================
        // BACK
        // ========================================================

        btnBack.setOnClickListener {

            finish()
        }


        // ========================================================
        // INITIAL UI
        // ========================================================

        updateAmountDisplay()

        updateSelectedCategoryText()

        setupTypeSelector()

        setupCategoryChips()

        setupPaymentChips()

        setupCustomKeypad()


        // ========================================================
        // SAVE
        // ========================================================

        btnAdd.setOnClickListener {

            saveTransactionAndNotify()
        }
    }


    // ============================================================
    // EXPENSE / INCOME SELECTOR
    // ============================================================

    private fun setupTypeSelector() {

        tvExpense.setOnClickListener {

            if (!isExpense) {

                isExpense = true

                tvExpense.setBackgroundResource(
                    R.drawable.selector_selected_bg
                )

                tvExpense.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.neutral_0
                    )
                )

                tvIncome.background = null

                tvIncome.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.neutral_500
                    )
                )

                btnAdd.text =
                    "Add Expense"
            }
        }


        tvIncome.setOnClickListener {

            if (isExpense) {

                isExpense = false

                tvIncome.setBackgroundResource(
                    R.drawable.selector_selected_bg
                )

                tvIncome.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.neutral_0
                    )
                )

                tvExpense.background = null

                tvExpense.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.neutral_500
                    )
                )

                btnAdd.text =
                    "Add Income"
            }
        }


        // Initial state

        tvExpense.setBackgroundResource(
            R.drawable.selector_selected_bg
        )

        tvExpense.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.neutral_0
            )
        )

        tvIncome.background = null

        tvIncome.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.neutral_500
            )
        )

        btnAdd.text =
            "Add Expense"
    }


    // ============================================================
    // CATEGORY OBSERVER
    // ============================================================

    private fun setupCategoryChips() {

        lifecycleScope.launch {

            database
                .categoryDao()
                .observeAll(currentUserId)
                .collectLatest { categories ->

                    categoryList =
                        categories

                    updateCategoryChips()
                }
        }
    }


    // ============================================================
    // UPDATE CATEGORY CHIPS
    // ============================================================

    private fun updateCategoryChips() {

        categoryChipContainer.removeAllViews()


        // ============================================================
        // DISPLAY CATEGORIES
        // ============================================================

        val displayCategories =
            mutableListOf<CategoryEntity>()


        // ------------------------------------------------------------
        // FIND SELECTED CATEGORY
        // ------------------------------------------------------------

        val selectedCategoryEntity =
            categoryList.firstOrNull { category ->

                category.label == selectedCategory
            }


        // ------------------------------------------------------------
        // PUT SELECTED CATEGORY FIRST
        // ------------------------------------------------------------

        if (selectedCategoryEntity != null) {

            displayCategories.add(
                selectedCategoryEntity
            )
        }


        // ------------------------------------------------------------
        // ADD OTHER CATEGORIES
        //
        // Maximum visible category chips = 3
        // Selected category is already first.
        // ------------------------------------------------------------

        categoryList
            .filter { category ->

                category.label != selectedCategory
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


        // ============================================================
        // ADD CATEGORY CHIPS
        // ============================================================

        displayCategories.forEach { category ->

            val chip =
                createCategoryChip(
                    category
                )

            categoryChipContainer.addView(
                chip
            )
        }


        // ============================================================
        // ALWAYS ADD OTHER BUTTON
        // ============================================================

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


        // --------------------------------------------------------
        // SIZE
        // --------------------------------------------------------

        val chipParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        chipParams.marginEnd =
            dpToPx(10)

        chip.layoutParams =
            chipParams


        // --------------------------------------------------------
        // ICON
        // --------------------------------------------------------

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


        // --------------------------------------------------------
        // TEXT
        // --------------------------------------------------------

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


        // --------------------------------------------------------
        // SELECTED STATE
        // --------------------------------------------------------

        chip.alpha =
            if (selectedCategory.isEmpty()) {

                1.0f

            } else if (selectedCategory == category.label) {

                1.0f

            } else {

                0.65f
            }


        // --------------------------------------------------------
        // CLICK
        // --------------------------------------------------------

        chip.setOnClickListener {

            selectedCategory =
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


        // --------------------------------------------------------
        // SIZE
        // --------------------------------------------------------

        val chipParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        chipParams.marginEnd =
            dpToPx(10)

        chip.layoutParams =
            chipParams


        // --------------------------------------------------------
        // ICON
        // --------------------------------------------------------

        val icon =
            TextView(this)

        icon.text =
            "⋯"

        icon.textSize =
            22f


        // --------------------------------------------------------
        // TEXT
        // --------------------------------------------------------

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


        // --------------------------------------------------------
        // SELECTED STATE
        // --------------------------------------------------------

        val firstThreeLabels =
            categoryList
                .take(3)
                .map {
                    it.label
                }


        chip.alpha =
            when {

                selectedCategory.isEmpty() ->
                    1.0f

                selectedCategory !in firstThreeLabels ->
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
            if (selectedCategory.isEmpty()) {

                "No category selected"

            } else {

                "Selected: $selectedCategory"
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
    // PAYMENT METHOD
    // ============================================================

    private fun setupPaymentChips() {

        val chipCash =
            findViewById<LinearLayout>(
                R.id.chipCash
            )

        val chipCard =
            findViewById<LinearLayout>(
                R.id.chipCard
            )

        val chipBank =
            findViewById<LinearLayout>(
                R.id.chipBank
            )


        val chips =
            listOf(
                chipCash to "Cash",
                chipCard to "Card",
                chipBank to "Bank"
            )


        chips.forEach { (chip, name) ->

            chip.setOnClickListener {

                selectedPaymentMethod =
                    name

                chips.forEach { (otherChip, _) ->

                    otherChip.alpha =
                        0.5f
                }

                chip.alpha =
                    1.0f
            }
        }


        selectedPaymentMethod =
            "Cash"

        chipCash.alpha =
            1.0f

        chipCard.alpha =
            0.5f

        chipBank.alpha =
            0.5f
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


        // --------------------------------------------------------
        // DOT
        // --------------------------------------------------------

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


        // --------------------------------------------------------
        // DELETE
        // --------------------------------------------------------

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


        // --------------------------------------------------------
        // VALIDATE AMOUNT
        // --------------------------------------------------------

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


        // --------------------------------------------------------
        // VALIDATE CATEGORY
        // --------------------------------------------------------

        if (
            selectedCategory.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Please select a category",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // --------------------------------------------------------
        // VALIDATE USER
        // --------------------------------------------------------

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

                selectedCategory
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

                        category =
                            selectedCategory,

                        paymentMethod =
                            selectedPaymentMethod,

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
                            "A $userCurrency $amount charge was made for $title today via $selectedPaymentMethod."

                        notiType =
                            "TRANSACTION"

                    } else {

                        notiTitle =
                            "Expense Added"

                        notiMessage =
                            "You spent $userCurrency $amount on $title ($selectedCategory)."

                        notiType =
                            "EXPENSE"
                    }

                } else {

                    notiTitle =
                        "Income Received"

                    notiMessage =
                        "You received $userCurrency $amount from $title via $selectedPaymentMethod."

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