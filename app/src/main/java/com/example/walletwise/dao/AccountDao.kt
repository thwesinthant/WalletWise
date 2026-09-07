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
        WHERE user_id = :userId
        ORDER BY account_id ASC
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
        WHERE account_id = :accountId
        AND user_id = :userId
        LIMIT 1
        """
    )
    suspend fun getAccountById(
        accountId: Int,
        userId: Int
    ): Account?


    // =========================================================
    // GET ALL ACCOUNT BALANCES
    //
    // Current Balance =
    //
    // opening balance
    // + income
    // + transfer in
    // - expense
    // - transfer out
    // =========================================================

    @Query(
        """
        SELECT
            a.account_id AS account_id,
            a.name AS name,
            a.opening_balance AS opening_balance,

            (
                a.opening_balance

                + COALESCE((
                    SELECT SUM(t.amount)
                    FROM transactions t
                    WHERE t.account_id = a.account_id
                    AND t.user_id = a.user_id
                    AND t.type = 'INCOME'
                ), 0)

                + COALESCE((
                    SELECT SUM(t.amount)
                    FROM transactions t
                    WHERE t.account_id = a.account_id
                    AND t.user_id = a.user_id
                    AND t.type = 'TRANSFER_IN'
                ), 0)

                - COALESCE((
                    SELECT SUM(t.amount)
                    FROM transactions t
                    WHERE t.account_id = a.account_id
                    AND t.user_id = a.user_id
                    AND t.type = 'EXPENSE'
                ), 0)

                - COALESCE((
                    SELECT SUM(t.amount)
                    FROM transactions t
                    WHERE t.account_id = a.account_id
                    AND t.user_id = a.user_id
                    AND t.type = 'TRANSFER_OUT'
                ), 0)

            ) AS current_balance

        FROM accounts a

        WHERE a.user_id = :userId

        ORDER BY a.account_id ASC
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
            a.account_id AS account_id,
            a.name AS name,
            a.opening_balance AS opening_balance,

            (
                a.opening_balance

                + COALESCE((
                    SELECT SUM(t.amount)
                    FROM transactions t
                    WHERE t.account_id = a.account_id
                    AND t.user_id = a.user_id
                    AND t.type = 'INCOME'
                ), 0)

                + COALESCE((
                    SELECT SUM(t.amount)
                    FROM transactions t
                    WHERE t.account_id = a.account_id
                    AND t.user_id = a.user_id
                    AND t.type = 'TRANSFER_IN'
                ), 0)

                - COALESCE((
                    SELECT SUM(t.amount)
                    FROM transactions t
                    WHERE t.account_id = a.account_id
                    AND t.user_id = a.user_id
                    AND t.type = 'EXPENSE'
                ), 0)

                - COALESCE((
                    SELECT SUM(t.amount)
                    FROM transactions t
                    WHERE t.account_id = a.account_id
                    AND t.user_id = a.user_id
                    AND t.type = 'TRANSFER_OUT'
                ), 0)

            ) AS current_balance

        FROM accounts a

        WHERE a.account_id = :accountId
        AND a.user_id = :userId

        LIMIT 1
        """
    )
    suspend fun getAccountBalanceById(
        accountId: Int,
        userId: Int
    ): AccountBalance?


    // =========================================================
    // GET TRANSACTION EFFECT
    //
    // Everything that happened after opening balance.
    //
    // transactionEffect =
    //
    // income
    // + transfer in
    // - expense
    // - transfer out
    //
    // When editing the account's current balance:
    //
    // newOpeningBalance =
    // desiredCurrentBalance - transactionEffect
    // =========================================================

    @Query(
        """
        SELECT

            COALESCE((
                SELECT SUM(t.amount)
                FROM transactions t
                WHERE t.account_id = :accountId
                AND t.user_id = :userId
                AND t.type = 'INCOME'
            ), 0)

            +

            COALESCE((
                SELECT SUM(t.amount)
                FROM transactions t
                WHERE t.account_id = :accountId
                AND t.user_id = :userId
                AND t.type = 'TRANSFER_IN'
            ), 0)

            -

            COALESCE((
                SELECT SUM(t.amount)
                FROM transactions t
                WHERE t.account_id = :accountId
                AND t.user_id = :userId
                AND t.type = 'EXPENSE'
            ), 0)

            -

            COALESCE((
                SELECT SUM(t.amount)
                FROM transactions t
                WHERE t.account_id = :accountId
                AND t.user_id = :userId
                AND t.type = 'TRANSFER_OUT'
            ), 0)
        """
    )
    suspend fun getTransactionEffect(
        accountId: Int,
        userId: Int
    ): Double
}