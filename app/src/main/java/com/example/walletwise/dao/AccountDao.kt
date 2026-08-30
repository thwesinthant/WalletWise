package com.example.walletwise.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.walletwise.entity.Account
import com.example.walletwise.entity.AccountBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    // =========================================================
    // INSERT
    // =========================================================

    @Insert
    suspend fun insert(
        account: Account
    ): Long


    // =========================================================
    // UPDATE
    // =========================================================

    @Update
    suspend fun update(
        account: Account
    )


    // =========================================================
    // DELETE
    // =========================================================

    @Delete
    suspend fun delete(
        account: Account
    )


    // =========================================================
    // GET ACCOUNTS FOR USER
    // =========================================================

    @Query(
        """
        SELECT *
        FROM accounts
        WHERE userId = :userId
        ORDER BY accountId ASC
        """
    )
    fun getAccountsForUser(
        userId: Int
    ): Flow<List<Account>>


    // =========================================================
    // GET ACCOUNT BY ID
    // =========================================================

    @Query(
        """
        SELECT *
        FROM accounts
        WHERE accountId = :accountId
        AND userId = :userId
        LIMIT 1
        """
    )
    suspend fun getAccountById(
        accountId: Int,
        userId: Int
    ): Account?


    // =========================================================
    // GET ACCOUNT BALANCES
    //
    // Balance =
    //
    // openingBalance
    // + income
    // + transfer in
    // - expense
    // - transfer out
    // =========================================================

    @Query(
        """
        SELECT
            a.accountId AS accountId,
            a.name AS name,
            a.openingBalance AS openingBalance,

            a.openingBalance

            + COALESCE((
                SELECT SUM(amount)
                FROM transactions t
                WHERE t.account_id = a.accountId
                AND t.type = 'INCOME'
            ), 0)

            + COALESCE((
                SELECT SUM(amount)
                FROM transactions t
                WHERE t.account_id = a.accountId
                AND t.type = 'TRANSFER_IN'
            ), 0)

            - COALESCE((
                SELECT SUM(amount)
                FROM transactions t
                WHERE t.account_id = a.accountId
                AND t.type = 'EXPENSE'
            ), 0)

            - COALESCE((
                SELECT SUM(amount)
                FROM transactions t
                WHERE t.account_id = a.accountId
                AND t.type = 'TRANSFER_OUT'
            ), 0)

            AS currentBalance

        FROM accounts a

        WHERE a.userId = :userId

        ORDER BY a.accountId ASC
        """
    )
    fun getAccountBalances(
        userId: Int
    ): Flow<List<AccountBalance>>


    // =========================================================
    // GET SINGLE ACCOUNT BALANCE
    // =========================================================

    @Query(
        """
        SELECT
            a.accountId AS accountId,
            a.name AS name,
            a.openingBalance AS openingBalance,

            a.openingBalance

            + COALESCE((
                SELECT SUM(amount)
                FROM transactions t
                WHERE t.account_id = a.accountId
                AND t.type = 'INCOME'
            ), 0)

            + COALESCE((
                SELECT SUM(amount)
                FROM transactions t
                WHERE t.account_id = a.accountId
                AND t.type = 'TRANSFER_IN'
            ), 0)

            - COALESCE((
                SELECT SUM(amount)
                FROM transactions t
                WHERE t.account_id = a.accountId
                AND t.type = 'EXPENSE'
            ), 0)

            - COALESCE((
                SELECT SUM(amount)
                FROM transactions t
                WHERE t.account_id = a.accountId
                AND t.type = 'TRANSFER_OUT'
            ), 0)

            AS currentBalance

        FROM accounts a

        WHERE a.accountId = :accountId
        AND a.userId = :userId

        LIMIT 1
        """
    )
    suspend fun getAccountBalanceById(
        accountId: Int,
        userId: Int
    ): AccountBalance?
}