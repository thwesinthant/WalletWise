package com.example.walletwise.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.walletwise.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertTransaction(
        transaction: Transaction
    )

    @Query("""
        SELECT *
        FROM transactions
        WHERE user_id = :userId
        ORDER BY created_at DESC
    """)
    fun getAllTransactions(
        userId: Int
    ): Flow<List<Transaction>>

    @Query("""
        SELECT *
        FROM transactions
        WHERE user_id = :userId
        ORDER BY created_at DESC
        LIMIT 10
    """)
    fun getRecent10Transactions(
        userId: Int
    ): Flow<List<Transaction>>

    @Query("""
        SELECT SUM(amount)
        FROM transactions
        WHERE user_id = :userId
        AND type = 'EXPENSE'
    """)
    fun getTotalExpense(
        userId: Int
    ): Flow<Double?>

    @Query("""
        SELECT SUM(amount)
        FROM transactions
        WHERE user_id = :userId
        AND type = 'INCOME'
    """)
    fun getTotalIncome(
        userId: Int
    ): Flow<Double?>
}