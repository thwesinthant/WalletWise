package com.example.walletwise.database

import android.content.Context
import androidx.room.withTransaction
import com.example.walletwise.entity.Account
import com.example.walletwise.entity.Budget
import com.example.walletwise.entity.BudgetCategory
import com.example.walletwise.entity.CategoryEntity
import com.example.walletwise.entity.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Temporary presentation/demo data for the WalletWise project book.
 *
 * IMPORTANT:
 * - This seed is ONLY for the presentation user.
 * - It does NOT create a User.
 * - It does NOT create Goals.
 * - It does NOT create Notifications.
 * - It uses only INCOME and EXPENSE transactions.
 *
 * Presentation account:
 *     yyyykittyyyy@gmail.com
 *
 * Expected user ID:
 *     4
 *
 * After taking screenshots, remove the call to this seed
 * from the Activity. The inserted Room data will remain.
 */
object PresentationSeed {

    private const val PRESENTATION_EMAIL = "yyyykittyyyy@gmail.com"
    private const val EXPECTED_USER_ID = 4

    // =========================================================
    // PUBLIC ENTRY POINT
    // =========================================================

    suspend fun seedForPresentationUser(
        context: Context
    ): Boolean = withContext(Dispatchers.IO) {

        val db = AppDatabase.getDatabase(context.applicationContext)

        // -----------------------------------------------------
        // 1. Make sure User ID 4 is really the presentation user
        // -----------------------------------------------------

        val user = db.userDao()
            .getUserByIdOnce(EXPECTED_USER_ID)

        if (user == null) {
            throw IllegalStateException(
                "Presentation user with ID 4 was not found. " +
                        "Please register $PRESENTATION_EMAIL first."
            )
        }

        if (!user.email.equals(PRESENTATION_EMAIL, ignoreCase = true)) {
            throw IllegalStateException(
                "User ID 4 belongs to ${user.email}, not $PRESENTATION_EMAIL."
            )
        }

        val userId = user.userId

        // -----------------------------------------------------
        // 2. Prevent accidental duplicate seeding
        // -----------------------------------------------------

        val existingTransactions = db.transactionDao()
            .getAllTransactions(userId)
            .first()

        val septemberSalaryDate = date(
            year = 2026,
            month = 9,
            day = 1,
            hour = 10
        )

        val alreadySeeded = existingTransactions.any {
            it.title == "Monthly Salary" &&
                    it.amount == 1_100_000.0 &&
                    it.createdAt == septemberSalaryDate
        }

        if (alreadySeeded) {
            return@withContext false
        }

        // -----------------------------------------------------
        // 3. Load categories for this user
        // -----------------------------------------------------

        var categories = db.categoryDao()
            .observeAll(userId)
            .first()

        /*
         * Normally categories are already created during
         * registration. If they are not, create them here.
         */
        if (categories.isEmpty()) {
            categories = CategorySeedLoader
                .loadDefaultEntities(context.applicationContext, userId)

            db.categoryDao().insertAll(categories)

            categories = db.categoryDao()
                .observeAll(userId)
                .first()
        }

        val categoryMap: Map<String, CategoryEntity> =
            categories.associateBy { it.label }

        // -----------------------------------------------------
        // Make sure every category required by this seed exists
        // -----------------------------------------------------

        val requiredCategories = listOf(
            "Salary",
            "Freelance",
            "Groceries",
            "Dining",
            "Fuel",
            "Shopping",
            "Rent",
            "Electricity",
            "Gym",
            "Entertainment"
        )

        requiredCategories.forEach { label ->
            if (!categoryMap.containsKey(label)) {
                throw IllegalStateException(
                    "Required category '$label' was not found for user $userId."
                )
            }
        }

        // -----------------------------------------------------
        // 4. Create / reuse presentation accounts
        // -----------------------------------------------------

        val existingAccounts = db.accountDao()
            .getAccountsForUser(userId)
            .first()

        val accountsByName = existingAccounts
            .associateBy { it.name }
            .toMutableMap()

        suspend fun getOrCreateAccount(
            name: String,
            openingBalance: Double
        ): Int {

            val existing = accountsByName[name]

            if (existing != null) {
                return existing.accountId
            }

            val account = Account(
                userId = userId,
                name = name,
                openingBalance = openingBalance,
                createdAt = date(2025, 10, 1, 9)
            )

            val accountId = db.accountDao()
                .insert(account)
                .toInt()

            accountsByName[name] = account.copy(
                accountId = accountId
            )

            return accountId
        }

        val cashWalletId = getOrCreateAccount(
            name = "Cash Wallet",
            openingBalance = 800_000.0
        )

        val kbzBankId = getOrCreateAccount(
            name = "KBZ Bank",
            openingBalance = 300_000.0
        )

        val waveMoneyId = getOrCreateAccount(
            name = "Wave Money",
            openingBalance = 500_000.0
        )

        val savingsId = getOrCreateAccount(
            name = "Savings",
            openingBalance = 500_000.0
        )

        // -----------------------------------------------------
        // 5. Helper for category IDs
        // -----------------------------------------------------

        fun categoryId(label: String): Long {
            return categoryMap[label]?.id
                ?: throw IllegalStateException(
                    "Category '$label' not found."
                )
        }

        // -----------------------------------------------------
        // 6. Helper for creating transactions
        // -----------------------------------------------------

        fun transaction(
            title: String,
            amount: Double,
            type: String,
            category: String,
            accountId: Int,
            year: Int,
            month: Int,
            day: Int,
            hour: Int = 12
        ): Transaction {

            return Transaction(
                userId = userId,
                title = title,
                amount = amount,
                type = type,
                categoryId = categoryId(category),
                accountId = accountId,
                note = null,
                createdAt = date(
                    year = year,
                    month = month,
                    day = day,
                    hour = hour
                ),
                transferGroupId = null
            )
        }

        // =====================================================
        // 7. PRESENTATION TRANSACTIONS
        // =====================================================
        //
        // Data range:
        //
        // Oct 2025
        // Nov 2025
        // Dec 2025
        //
        // Jan 2026
        // Feb 2026
        // Mar 2026
        // Apr 2026
        //
        // May 2026
        // Jun 2026
        // Jul 2026
        // Aug 2026
        // Sep 2026
        //
        // Total = 40 transactions
        // =====================================================

        val transactions = listOf(

            // -------------------------------------------------
            // OCTOBER 2025
            // -------------------------------------------------

            transaction(
                title = "Monthly Salary",
                amount = 850_000.0,
                type = "INCOME",
                category = "Salary",
                accountId = kbzBankId,
                year = 2025,
                month = 10,
                day = 3
            ),

            transaction(
                title = "Apartment Rent",
                amount = 350_000.0,
                type = "EXPENSE",
                category = "Rent",
                accountId = kbzBankId,
                year = 2025,
                month = 10,
                day = 5
            ),

            // -------------------------------------------------
            // NOVEMBER 2025
            // -------------------------------------------------

            transaction(
                title = "Monthly Salary",
                amount = 900_000.0,
                type = "INCOME",
                category = "Salary",
                accountId = savingsId,
                year = 2025,
                month = 11,
                day = 3
            ),

            transaction(
                title = "Grocery Shopping",
                amount = 80_000.0,
                type = "EXPENSE",
                category = "Groceries",
                accountId = cashWalletId,
                year = 2025,
                month = 11,
                day = 7
            ),

            // -------------------------------------------------
            // DECEMBER 2025
            // -------------------------------------------------

            transaction(
                title = "Monthly Salary",
                amount = 900_000.0,
                type = "INCOME",
                category = "Salary",
                accountId = kbzBankId,
                year = 2025,
                month = 12,
                day = 3
            ),

            transaction(
                title = "Holiday Shopping",
                amount = 100_000.0,
                type = "EXPENSE",
                category = "Shopping",
                accountId = waveMoneyId,
                year = 2025,
                month = 12,
                day = 12
            ),

            // =================================================
            // JANUARY 2026
            // =================================================

            transaction(
                title = "Monthly Salary",
                amount = 950_000.0,
                type = "INCOME",
                category = "Salary",
                accountId = savingsId,
                year = 2026,
                month = 1,
                day = 3
            ),

            transaction(
                title = "Apartment Rent",
                amount = 350_000.0,
                type = "EXPENSE",
                category = "Rent",
                accountId = kbzBankId,
                year = 2026,
                month = 1,
                day = 5
            ),

            // =================================================
            // FEBRUARY 2026
            // =================================================

            transaction(
                title = "Monthly Salary",
                amount = 950_000.0,
                type = "INCOME",
                category = "Salary",
                accountId = kbzBankId,
                year = 2026,
                month = 2,
                day = 3
            ),

            transaction(
                title = "Weekend Dining",
                amount = 35_000.0,
                type = "EXPENSE",
                category = "Dining",
                accountId = waveMoneyId,
                year = 2026,
                month = 2,
                day = 10
            ),

            // =================================================
            // MARCH 2026
            // =================================================

            transaction(
                title = "Monthly Salary",
                amount = 1_000_000.0,
                type = "INCOME",
                category = "Salary",
                accountId = savingsId,
                year = 2026,
                month = 3,
                day = 3
            ),

            transaction(
                title = "Fuel",
                amount = 55_000.0,
                type = "EXPENSE",
                category = "Fuel",
                accountId = cashWalletId,
                year = 2026,
                month = 3,
                day = 8
            ),

            // =================================================
            // APRIL 2026
            // =================================================

            transaction(
                title = "Monthly Salary",
                amount = 1_000_000.0,
                type = "INCOME",
                category = "Salary",
                accountId = kbzBankId,
                year = 2026,
                month = 4,
                day = 3
            ),

            transaction(
                title = "Electricity Bill",
                amount = 60_000.0,
                type = "EXPENSE",
                category = "Electricity",
                accountId = kbzBankId,
                year = 2026,
                month = 4,
                day = 12
            ),

            // =================================================
            // MAY 2026
            // =================================================

            transaction(
                title = "Monthly Salary",
                amount = 1_050_000.0,
                type = "INCOME",
                category = "Salary",
                accountId = kbzBankId,
                year = 2026,
                month = 5,
                day = 3
            ),

            transaction(
                title = "Freelance Project",
                amount = 220_000.0,
                type = "INCOME",
                category = "Freelance",
                accountId = waveMoneyId,
                year = 2026,
                month = 5,
                day = 5
            ),

            transaction(
                title = "Apartment Rent",
                amount = 400_000.0,
                type = "EXPENSE",
                category = "Rent",
                accountId = kbzBankId,
                year = 2026,
                month = 5,
                day = 7
            ),

            transaction(
                title = "Grocery Shopping",
                amount = 95_000.0,
                type = "EXPENSE",
                category = "Groceries",
                accountId = cashWalletId,
                year = 2026,
                month = 5,
                day = 12
            ),

            transaction(
                title = "Online Shopping",
                amount = 120_000.0,
                type = "EXPENSE",
                category = "Shopping",
                accountId = waveMoneyId,
                year = 2026,
                month = 5,
                day = 20
            ),

            // =================================================
            // JUNE 2026
            // =================================================

            transaction(
                title = "Monthly Salary",
                amount = 1_050_000.0,
                type = "INCOME",
                category = "Salary",
                accountId = savingsId,
                year = 2026,
                month = 6,
                day = 3
            ),

            transaction(
                title = "Freelance Project",
                amount = 200_000.0,
                type = "INCOME",
                category = "Freelance",
                accountId = waveMoneyId,
                year = 2026,
                month = 6,
                day = 6
            ),

            transaction(
                title = "Grocery Shopping",
                amount = 105_000.0,
                type = "EXPENSE",
                category = "Groceries",
                accountId = cashWalletId,
                year = 2026,
                month = 6,
                day = 9
            ),

            transaction(
                title = "Electricity Bill",
                amount = 65_000.0,
                type = "EXPENSE",
                category = "Electricity",
                accountId = kbzBankId,
                year = 2026,
                month = 6,
                day = 15
            ),

            transaction(
                title = "Monthly Gym Fee",
                amount = 60_000.0,
                type = "EXPENSE",
                category = "Gym",
                accountId = waveMoneyId,
                year = 2026,
                month = 6,
                day = 22
            ),

            // =================================================
            // JULY 2026
            // =================================================

            transaction(
                title = "Monthly Salary",
                amount = 1_100_000.0,
                type = "INCOME",
                category = "Salary",
                accountId = kbzBankId,
                year = 2026,
                month = 7,
                day = 3
            ),

            transaction(
                title = "Freelance Project",
                amount = 180_000.0,
                type = "INCOME",
                category = "Freelance",
                accountId = waveMoneyId,
                year = 2026,
                month = 7,
                day = 6
            ),

            transaction(
                title = "Apartment Rent",
                amount = 400_000.0,
                type = "EXPENSE",
                category = "Rent",
                accountId = kbzBankId,
                year = 2026,
                month = 7,
                day = 8
            ),

            transaction(
                title = "Fuel",
                amount = 65_000.0,
                type = "EXPENSE",
                category = "Fuel",
                accountId = cashWalletId,
                year = 2026,
                month = 7,
                day = 15
            ),

            transaction(
                title = "Movie & Entertainment",
                amount = 50_000.0,
                type = "EXPENSE",
                category = "Entertainment",
                accountId = waveMoneyId,
                year = 2026,
                month = 7,
                day = 23
            ),

            // =================================================
            // AUGUST 2026
            // =================================================

            transaction(
                title = "Monthly Salary",
                amount = 1_100_000.0,
                type = "INCOME",
                category = "Salary",
                accountId = savingsId,
                year = 2026,
                month = 8,
                day = 1
            ),

            transaction(
                title = "Freelance Project",
                amount = 240_000.0,
                type = "INCOME",
                category = "Freelance",
                accountId = waveMoneyId,
                year = 2026,
                month = 8,
                day = 2
            ),

            transaction(
                title = "Grocery Shopping",
                amount = 110_000.0,
                type = "EXPENSE",
                category = "Groceries",
                accountId = cashWalletId,
                year = 2026,
                month = 8,
                day = 4
            ),

            transaction(
                title = "Online Shopping",
                amount = 140_000.0,
                type = "EXPENSE",
                category = "Shopping",
                accountId = waveMoneyId,
                year = 2026,
                month = 8,
                day = 6
            ),

            transaction(
                title = "Weekend Dining",
                amount = 45_000.0,
                type = "EXPENSE",
                category = "Dining",
                accountId = waveMoneyId,
                year = 2026,
                month = 8,
                day = 8
            ),

            // =================================================
            // SEPTEMBER 2026
            // =================================================
            //
            // IMPORTANT:
            // Today is September 8, 2026.
            //
            // Therefore all September transactions are
            // deliberately placed between September 1–8.
            // This makes "This Month" show daily bars.
            // =================================================

            transaction(
                title = "Monthly Salary",
                amount = 1_100_000.0,
                type = "INCOME",
                category = "Salary",
                accountId = kbzBankId,
                year = 2026,
                month = 9,
                day = 1,
                hour = 10
            ),

            transaction(
                title = "Grocery Shopping",
                amount = 75_000.0,
                type = "EXPENSE",
                category = "Groceries",
                accountId = cashWalletId,
                year = 2026,
                month = 9,
                day = 2,
                hour = 18
            ),

            transaction(
                title = "Weekend Dining",
                amount = 30_000.0,
                type = "EXPENSE",
                category = "Dining",
                accountId = waveMoneyId,
                year = 2026,
                month = 9,
                day = 3,
                hour = 19
            ),

            transaction(
                title = "Fuel",
                amount = 45_000.0,
                type = "EXPENSE",
                category = "Fuel",
                accountId = cashWalletId,
                year = 2026,
                month = 9,
                day = 4,
                hour = 17
            ),

            transaction(
                title = "Online Shopping",
                amount = 95_000.0,
                type = "EXPENSE",
                category = "Shopping",
                accountId = waveMoneyId,
                year = 2026,
                month = 9,
                day = 6,
                hour = 15
            ),

            transaction(
                title = "Freelance Project",
                amount = 160_000.0,
                type = "INCOME",
                category = "Freelance",
                accountId = waveMoneyId,
                year = 2026,
                month = 9,
                day = 8,
                hour = 11
            )
        )

        // =====================================================
        // 8. BUDGETS
        // =====================================================
        //
        // Three non-overlapping budgets:
        //
        // August 2026
        // September 2026
        // October 2026
        //
        // This gives the Budget screen realistic historical,
        // current and upcoming budget data.
        // =====================================================

        val augustBudget = Budget(
            userId = userId,
            name = "August Lifestyle",
            amount = 600_000.0,
            startDate = startOfDay(2026, 8, 1),
            endDate = endOfDay(2026, 8, 31),
            createdAt = date(2026, 7, 28, 10)
        )

        val septemberBudget = Budget(
            userId = userId,
            name = "September Essentials",
            amount = 500_000.0,
            startDate = startOfDay(2026, 9, 1),
            endDate = endOfDay(2026, 9, 30),
            createdAt = date(2026, 8, 28, 10)
        )

        val octoberBudget = Budget(
            userId = userId,
            name = "October Essentials",
            amount = 900_000.0,
            startDate = startOfDay(2026, 10, 1),
            endDate = endOfDay(2026, 10, 31),
            createdAt = date(2026, 9, 1, 10)
        )

        // =====================================================
        // 9. INSERT EVERYTHING ATOMICALLY
        // =====================================================

        db.withTransaction {

            // -----------------------------------------------
            // Transactions
            // -----------------------------------------------

            transactions.forEach { tx ->
                db.transactionDao()
                    .insertTransaction(tx)
            }

            // -----------------------------------------------
            // August budget
            // -----------------------------------------------

            db.budgetDao().insertBudgetWithCategories(
                budget = augustBudget,
                categories = listOf(

                    BudgetCategory(
                        budgetId = 0,
                        categoryId = categoryId("Groceries"),
                        limitAmount = 150_000.0
                    ),

                    BudgetCategory(
                        budgetId = 0,
                        categoryId = categoryId("Shopping"),
                        limitAmount = 180_000.0
                    ),

                    BudgetCategory(
                        budgetId = 0,
                        categoryId = categoryId("Dining"),
                        limitAmount = 80_000.0
                    ),

                    BudgetCategory(
                        budgetId = 0,
                        categoryId = categoryId("Fuel"),
                        limitAmount = 80_000.0
                    )
                )
            )

            // -----------------------------------------------
            // September budget
            // -----------------------------------------------

            db.budgetDao().insertBudgetWithCategories(
                budget = septemberBudget,
                categories = listOf(

                    BudgetCategory(
                        budgetId = 0,
                        categoryId = categoryId("Groceries"),
                        limitAmount = 120_000.0
                    ),

                    BudgetCategory(
                        budgetId = 0,
                        categoryId = categoryId("Dining"),
                        limitAmount = 60_000.0
                    ),

                    BudgetCategory(
                        budgetId = 0,
                        categoryId = categoryId("Fuel"),
                        limitAmount = 80_000.0
                    ),

                    BudgetCategory(
                        budgetId = 0,
                        categoryId = categoryId("Shopping"),
                        limitAmount = 150_000.0
                    ),

                    BudgetCategory(
                        budgetId = 0,
                        categoryId = categoryId("Electricity"),
                        limitAmount = 60_000.0
                    )
                )
            )

            // -----------------------------------------------
            // October budget
            // -----------------------------------------------

            db.budgetDao().insertBudgetWithCategories(
                budget = octoberBudget,
                categories = listOf(

                    BudgetCategory(
                        budgetId = 0,
                        categoryId = categoryId("Rent"),
                        limitAmount = 400_000.0
                    ),

                    BudgetCategory(
                        budgetId = 0,
                        categoryId = categoryId("Groceries"),
                        limitAmount = 150_000.0
                    ),

                    BudgetCategory(
                        budgetId = 0,
                        categoryId = categoryId("Electricity"),
                        limitAmount = 80_000.0
                    ),

                    BudgetCategory(
                        budgetId = 0,
                        categoryId = categoryId("Fuel"),
                        limitAmount = 80_000.0
                    ),

                    BudgetCategory(
                        budgetId = 0,
                        categoryId = categoryId("Internet"),
                        limitAmount = 60_000.0
                    )
                )
            )
        }

        // true = data was inserted
        return@withContext true
    }


    // =========================================================
    // DATE HELPERS
    // =========================================================

    /**
     * Creates a local-time timestamp.
     *
     * Month is 1-based:
     * January = 1
     * February = 2
     * ...
     * September = 9
     */
    private fun date(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 12,
        minute: Int = 0
    ): Long {

        return Calendar.getInstance().apply {

            clear()

            set(
                Calendar.YEAR,
                year
            )

            set(
                Calendar.MONTH,
                month - 1
            )

            set(
                Calendar.DAY_OF_MONTH,
                day
            )

            set(
                Calendar.HOUR_OF_DAY,
                hour
            )

            set(
                Calendar.MINUTE,
                minute
            )

            set(
                Calendar.SECOND,
                0
            )

            set(
                Calendar.MILLISECOND,
                0
            )

        }.timeInMillis
    }


    private fun startOfDay(
        year: Int,
        month: Int,
        day: Int
    ): Long {

        return date(
            year = year,
            month = month,
            day = day,
            hour = 0,
            minute = 0
        )
    }


    private fun endOfDay(
        year: Int,
        month: Int,
        day: Int
    ): Long {

        return Calendar.getInstance().apply {

            clear()

            set(
                Calendar.YEAR,
                year
            )

            set(
                Calendar.MONTH,
                month - 1
            )

            set(
                Calendar.DAY_OF_MONTH,
                day
            )

            set(
                Calendar.HOUR_OF_DAY,
                23
            )

            set(
                Calendar.MINUTE,
                59
            )

            set(
                Calendar.SECOND,
                59
            )

            set(
                Calendar.MILLISECOND,
                999
            )

        }.timeInMillis
    }
}