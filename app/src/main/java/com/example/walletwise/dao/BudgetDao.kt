package com.example.walletwise.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.walletwise.entity.Budget
import com.example.walletwise.entity.BudgetCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    // =========================
    // BUDGET
    // =========================

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

    @Query("""
        SELECT *
        FROM budgets
        WHERE user_id = :userId
        ORDER BY start_date DESC
    """)
    fun getBudgetsByUser(
        userId: Int
    ): Flow<List<Budget>>

    @Query("""
        SELECT *
        FROM budgets
        WHERE budgetId = :budgetId
        LIMIT 1
    """)
    suspend fun getBudgetById(
        budgetId: Int
    ): Budget?

    @Query("""
        SELECT *
        FROM budgets
        WHERE user_id = :userId
        AND start_date <= :date
        AND end_date >= :date
        ORDER BY start_date DESC
        LIMIT 1
    """)
    suspend fun getActiveBudget(
        userId: Int,
        date: Long
    ): Budget?


    // =========================
    // BUDGET CATEGORIES
    // =========================

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

    @Query("""
        SELECT *
        FROM budget_categories
        WHERE budget_id = :budgetId
        ORDER BY budget_category_id ASC
    """)
    fun getBudgetCategories(
        budgetId: Int
    ): Flow<List<BudgetCategory>>

    @Query("""
        SELECT *
        FROM budget_categories
        WHERE budget_id = :budgetId
    """)
    suspend fun getBudgetCategoriesOnce(
        budgetId: Int
    ): List<BudgetCategory>

    @Query("""
        DELETE FROM budget_categories
        WHERE budget_id = :budgetId
    """)
    suspend fun deleteCategoriesForBudget(
        budgetId: Int
    )
}