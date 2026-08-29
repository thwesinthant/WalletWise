package com.example.walletwise.dao

import androidx.room.*
import com.example.walletwise.entity.Account
import com.example.walletwise.entity.AccountBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY accountId ASC")
    fun getAccountsForUser(userId: Int): Flow<List<Account>>

    /**
     * currentBalance = openingBalance + total income on this account
     *                  - total expense on this account
     */
    @Query(
        """
        SELECT a.accountId  AS accountId,
               a.name       AS name,
               a.openingBalance AS openingBalance,
               a.openingBalance
                 + COALESCE((
                     SELECT SUM(amount) FROM transactions t
                     WHERE t.account_id = a.accountId AND t.type = 'INCOME'
                   ), 0)
                 - COALESCE((
                     SELECT SUM(amount) FROM transactions t
                     WHERE t.account_id = a.accountId AND t.type = 'EXPENSE'
                   ), 0) AS currentBalance
        FROM accounts a
        WHERE a.userId = :userId
        """
    )
    fun getAccountBalances(userId: Int): Flow<List<AccountBalance>>
}