package com.example.walletwise.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.walletwise.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

// ============================================================
// INSERT TRANSACTION
// ============================================================

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertTransaction(
        transaction: Transaction
    )


// ============================================================
// GET ALL TRANSACTIONS
// ============================================================

    @Query(
        """
    SELECT *
    FROM transactions
    WHERE user_id = :userId
    ORDER BY created_at DESC
    """
    )
    fun getAllTransactions(
        userId: Int
    ): Flow<List<Transaction>>


// ============================================================
// GET RECENT 10 TRANSACTIONS
// ============================================================

    @Query(
        """
    SELECT *
    FROM transactions
    WHERE user_id = :userId
    ORDER BY created_at DESC
    LIMIT 10
    """
    )
    fun getRecent10Transactions(
        userId: Int
    ): Flow<List<Transaction>>


// ============================================================
// GET SINGLE TRANSACTION
// ============================================================

    @Query(
        """
    SELECT *
    FROM transactions
    WHERE transaction_id = :transactionId
    AND user_id = :userId
    LIMIT 1
    """
    )
    suspend fun getTransactionById(
        transactionId: Int,
        userId: Int
    ): Transaction?


// ============================================================
// DELETE TRANSACTION
// ============================================================

    @Query(
        """
    DELETE FROM transactions
    WHERE transaction_id = :transactionId
    AND user_id = :userId
    """
    )
    suspend fun deleteTransaction(
        transactionId: Int,
        userId: Int
    )


// ============================================================
// TOTAL EXPENSE
// ============================================================

    @Query(
        """
    SELECT SUM(amount)
    FROM transactions
    WHERE user_id = :userId
    AND type = 'EXPENSE'
    """
    )
    fun getTotalExpense(
        userId: Int
    ): Flow<Double?>


// ============================================================
// TOTAL INCOME
// ============================================================

    @Query(
        """
    SELECT SUM(amount)
    FROM transactions
    WHERE user_id = :userId
    AND type = 'INCOME'
    """
    )
    fun getTotalIncome(
        userId: Int
    ): Flow<Double?>


// ============================================================
// TOTAL EXPENSE FOR PERIOD
// ============================================================

    @Query(
        """
    SELECT COALESCE(SUM(amount), 0)
    FROM transactions
    WHERE user_id = :userId
    AND type = 'EXPENSE'
    AND created_at >= :startDate
    AND created_at <= :endDate
    """
    )
    suspend fun getExpenseForPeriod(
        userId: Int,
        startDate: Long,
        endDate: Long
    ): Double


// ============================================================
// CATEGORY EXPENSE FOR PERIOD
// ============================================================

    @Query(
        """
    SELECT COALESCE(SUM(amount), 0)
    FROM transactions
    WHERE user_id = :userId
    AND category_id = :categoryId
    AND type = 'EXPENSE'
    AND created_at >= :startDate
    AND created_at <= :endDate
    """
    )
    suspend fun getCategoryExpenseForPeriod(
        userId: Int,
        categoryId: Long,
        startDate: Long,
        endDate: Long
    ): Double


// ============================================================
// UPDATE TRANSACTION
// ============================================================

    // ============================================================
// UPDATE TRANSACTION
// ============================================================

    @Query(
        """
    UPDATE transactions
    SET
        title = :title,
        amount = :amount,
        type = :type,
        category_id = :categoryId,
        account_id = :accountId,
        note = :note
    WHERE transaction_id = :transactionId
    AND user_id = :userId
    """
    )
    suspend fun updateTransaction(
        transactionId: Int,
        userId: Int,
        title: String,
        amount: Double,
        type: String,
        categoryId: Long?,
        accountId: Int?,
        note: String?
    )


}
