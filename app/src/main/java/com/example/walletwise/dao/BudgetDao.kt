package com.example.walletwise.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.walletwise.entity.Budget
import com.example.walletwise.entity.BudgetCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    // ============================================================
    // BUDGET
    // ============================================================

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertBudget(
        budget: Budget
    ): Long


    @Update
    suspend fun updateBudget(
        budget: Budget
    )


    @Delete
    suspend fun deleteBudget(
        budget: Budget
    )


    // ============================================================
    // GET ALL BUDGETS FOR USER
    // ============================================================

    @Query(
        """
        SELECT *
        FROM budgets
        WHERE user_id = :userId
        ORDER BY start_date DESC
        """
    )
    fun getBudgetsByUser(
        userId: Int
    ): Flow<List<Budget>>


    // ============================================================
    // GET ONE BUDGET
    // ============================================================

    @Query(
        """
        SELECT *
        FROM budgets
        WHERE budgetId = :budgetId
        LIMIT 1
        """
    )
    suspend fun getBudgetById(
        budgetId: Int
    ): Budget?


    // ============================================================
    // GET ACTIVE BUDGET
    //
    // A budget is active when:
    //
    // start_date <= current date
    // AND
    // end_date > current date
    //
    // end_date should represent the beginning of the day
    // AFTER the budget's final day.
    // ============================================================

    @Query(
        """
        SELECT *
        FROM budgets
        WHERE user_id = :userId
        AND start_date <= :date
        AND end_date > :date
        ORDER BY start_date DESC
        LIMIT 1
        """
    )
    suspend fun getActiveBudget(
        userId: Int,
        date: Long
    ): Budget?


    // ============================================================
    // CHECK OVERLAPPING BUDGET
    //
    // Used before creating/updating a budget.
    //
    // Returns the number of budgets that overlap the requested
    // period.
    //
    // When editing, pass the current budgetId so it is excluded.
    // ============================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM budgets
        WHERE user_id = :userId
        AND budgetId != :budgetId
        AND start_date < :endDate
        AND end_date > :startDate
        """
    )
    suspend fun countOverlappingBudgets(
        userId: Int,
        budgetId: Int,
        startDate: Long,
        endDate: Long
    ): Int


    // ============================================================
    // BUDGET CATEGORIES
    // ============================================================

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertBudgetCategory(
        budgetCategory: BudgetCategory
    ): Long


    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertBudgetCategories(
        budgetCategories: List<BudgetCategory>
    )


    @Update
    suspend fun updateBudgetCategory(
        budgetCategory: BudgetCategory
    )


    @Delete
    suspend fun deleteBudgetCategory(
        budgetCategory: BudgetCategory
    )


    // ============================================================
    // GET BUDGET CATEGORIES
    // ============================================================

    @Query(
        """
        SELECT *
        FROM budget_categories
        WHERE budget_id = :budgetId
        ORDER BY budget_category_id ASC
        """
    )
    fun getBudgetCategories(
        budgetId: Int
    ): Flow<List<BudgetCategory>>


    // ============================================================
    // GET BUDGET CATEGORIES ONCE
    // ============================================================

    @Query(
        """
        SELECT *
        FROM budget_categories
        WHERE budget_id = :budgetId
        ORDER BY budget_category_id ASC
        """
    )
    suspend fun getBudgetCategoriesOnce(
        budgetId: Int
    ): List<BudgetCategory>


    // ============================================================
    // DELETE ALL CATEGORY LIMITS FOR A BUDGET
    // ============================================================

    @Query(
        """
        DELETE FROM budget_categories
        WHERE budget_id = :budgetId
        """
    )
    suspend fun deleteCategoriesForBudget(
        budgetId: Int
    )


    // ============================================================
    // INSERT BUDGET + CATEGORIES
    //
    // Creates the budget and its category limits atomically.
    //
    // IMPORTANT:
    // The categories must use the newly-created budget ID.
    // ============================================================

    @Transaction
    suspend fun insertBudgetWithCategories(
        budget: Budget,
        categories: List<BudgetCategory>
    ): Long {

        val budgetId = insertBudget(budget)

        if (categories.isNotEmpty()) {

            val updatedCategories =
                categories.map { category ->

                    category.copy(
                        budgetId = budgetId.toInt()
                    )
                }

            insertBudgetCategories(
                updatedCategories
            )
        }

        return budgetId
    }


    // ============================================================
    // UPDATE BUDGET + CATEGORIES
    //
    // 1. Update budget
    // 2. Delete old category limits
    // 3. Insert new category limits
    //
    // Everything happens inside one Room transaction.
    // ============================================================

    @Transaction
    suspend fun updateBudgetWithCategories(
        budget: Budget,
        categories: List<BudgetCategory>
    ) {

        updateBudget(
            budget
        )

        deleteCategoriesForBudget(
            budget.budgetId
        )

        if (categories.isNotEmpty()) {

            val updatedCategories =
                categories.map { category ->

                    category.copy(
                        budgetId = budget.budgetId
                    )
                }

            insertBudgetCategories(
                updatedCategories
            )
        }
    }

    // ============================================================
// GET ALL ACTIVE BUDGETS
//
// Returns every budget that is currently active for the user.
// ============================================================

    @Query(
        """
    SELECT *
    FROM budgets
    WHERE user_id = :userId
    AND start_date <= :date
    AND end_date > :date
    ORDER BY start_date DESC
    """
    )
    suspend fun getActiveBudgets(
        userId: Int,
        date: Long
    ): List<Budget>

    // ============================================================
    // DELETE BUDGET COMPLETELY
    //
    // BudgetCategory rows are automatically deleted because
    // BudgetCategory has:
    //
    // onDelete = ForeignKey.CASCADE
    // ============================================================

    @Transaction
    suspend fun deleteBudgetCompletely(
        budget: Budget
    ) {

        deleteBudget(
            budget
        )
    }
}