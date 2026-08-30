package com.example.walletwise.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction as RoomTransaction
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
    // INSERT TRANSFER
    //
    // One transfer creates TWO transactions:
    //
    // TRANSFER_OUT
    // TRANSFER_IN
    //
    // Both must contain the SAME transferGroupId.
    // ============================================================

    @RoomTransaction
    suspend fun insertTransfer(
        transferOut: Transaction,
        transferIn: Transaction
    ) {
        insertTransaction(transferOut)
        insertTransaction(transferIn)
    }


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
    // GET TRANSFER PAIR
    // ============================================================

    @Query(
        """
        SELECT *
        FROM transactions
        WHERE transfer_group_id = :transferGroupId
        AND user_id = :userId
        ORDER BY transaction_id ASC
        """
    )
    suspend fun getTransactionsByTransferGroupId(
        transferGroupId: String,
        userId: Int
    ): List<Transaction>


    // ============================================================
    // DELETE SINGLE TRANSACTION
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
    // DELETE COMPLETE TRANSFER
    //
    // Deletes both TRANSFER_OUT and TRANSFER_IN.
    // ============================================================

    @Query(
        """
        DELETE FROM transactions
        WHERE transfer_group_id = :transferGroupId
        AND user_id = :userId
        """
    )
    suspend fun deleteTransfer(
        transferGroupId: String,
        userId: Int
    )


    // ============================================================
    // TOTAL EXPENSE
    // ============================================================

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE user_id = :userId
        AND type = 'EXPENSE'
        """
    )
    fun getTotalExpense(
        userId: Int
    ): Flow<Double>


    // ============================================================
    // TOTAL INCOME
    // ============================================================

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE user_id = :userId
        AND type = 'INCOME'
        """
    )
    fun getTotalIncome(
        userId: Int
    ): Flow<Double>


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
    // UPDATE NORMAL TRANSACTION
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


    // ============================================================
    // UPDATE TRANSFER TRANSACTION
    // ============================================================

    @Query(
        """
        UPDATE transactions
        SET
            title = :title,
            amount = :amount,
            account_id = :accountId,
            note = :note
        WHERE transaction_id = :transactionId
        AND user_id = :userId
        AND transfer_group_id = :transferGroupId
        """
    )
    suspend fun updateTransferTransaction(
        transactionId: Int,
        userId: Int,
        transferGroupId: String,
        title: String,
        amount: Double,
        accountId: Int,
        note: String?
    )


    // ============================================================
    // UPDATE COMPLETE TRANSFER
    //
    // Updates both sides atomically.
    // ============================================================

    @RoomTransaction
    suspend fun updateTransfer(
        transferOut: Transaction,
        transferIn: Transaction
    ) {

        updateTransferTransaction(
            transactionId = transferOut.transactionId,
            userId = transferOut.userId,
            transferGroupId = transferOut.transferGroupId!!,
            title = transferOut.title,
            amount = transferOut.amount,
            accountId = transferOut.accountId!!,
            note = transferOut.note
        )

        updateTransferTransaction(
            transactionId = transferIn.transactionId,
            userId = transferIn.userId,
            transferGroupId = transferIn.transferGroupId!!,
            title = transferIn.title,
            amount = transferIn.amount,
            accountId = transferIn.accountId!!,
            note = transferIn.note
        )
    }
}

