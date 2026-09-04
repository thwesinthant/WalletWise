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
        WHERE budget_id = :budgetId
        LIMIT 1
        """
    )
    suspend fun getBudgetById(
        budgetId: Int
    ): Budget?


    // ============================================================
    // GET ACTIVE BUDGET
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
    // ============================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM budgets
        WHERE user_id = :userId
        AND budget_id != :budgetId
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
    // GET ALL BUDGET CATEGORIES FOR A USER (across every one of
    // their budgets) — used by the Analytics screen's Budget vs
    // Actual card, which combines this with getBudgetsByUser
    // rather than adding a Repository/ViewModel layer.
    // ============================================================

    @Query(
        """
        SELECT budget_categories.*
        FROM budget_categories
        INNER JOIN budgets ON budgets.budget_id = budget_categories.budget_id
        WHERE budgets.user_id = :userId
        """
    )
    fun getBudgetCategoriesForUser(
        userId: Int
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
    // ============================================================

    @Transaction
    suspend fun insertBudgetWithCategories(
        budget: Budget,
        categories: List<BudgetCategory>
    ): Long {

        val budgetId =
            insertBudget(
                budget
            )

        if (categories.isNotEmpty()) {

            val updatedCategories =
                categories.map { category ->

                    category.copy(
                        budgetId =
                            budgetId.toInt()
                    )
                }

            insertBudgetCategories(
                updatedCategories
            )
        }

        return budgetId
    }



    @Transaction
    suspend fun updateBudgetWithCategories(
        budget: Budget,
        categories: List<BudgetCategory>
    ) {
        // Update main budget
        updateBudget(budget)

        // Get existing category rows
        val existingCategories =
            getBudgetCategoriesOnce(
                budget.budgetId
            )

        // Category IDs currently selected by user
        val newCategoryIds =
            categories
                .map { it.categoryId }
                .toSet()

        // Remove categories that no longer exist
        existingCategories
            .filter {
                it.categoryId !in newCategoryIds
            }
            .forEach { existing ->

                deleteBudgetCategory(
                    existing
                )
            }

        // Update existing categories
        // or insert new ones
        categories.forEach { newCategory ->

            val existing =
                existingCategories.firstOrNull {
                    it.categoryId ==
                            newCategory.categoryId
                }

            if (existing != null) {

                updateBudgetCategory(
                    existing.copy(
                        limitAmount =
                            newCategory.limitAmount
                    )
                )

            } else {

                insertBudgetCategory(
                    newCategory.copy(
                        budgetId =
                            budget.budgetId,

                        budgetCategoryId =
                            0
                    )
                )
            }
        }
    }


    // ============================================================
    // GET ALL ACTIVE BUDGETS
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