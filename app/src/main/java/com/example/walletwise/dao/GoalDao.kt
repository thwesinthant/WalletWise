package com.example.walletwise.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

import com.example.walletwise.entity.Goal

import kotlinx.coroutines.flow.Flow


@Dao
interface GoalDao {


    // ============================================================
    // INSERT
    // ============================================================

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertGoal(
        goal: Goal
    ): Long


    // ============================================================
    // UPDATE
    // ============================================================

    @Update
    suspend fun updateGoal(
        goal: Goal
    )


    // ============================================================
    // DELETE
    // ============================================================

    @Delete
    suspend fun deleteGoal(
        goal: Goal
    )


    // ============================================================
    // OBSERVE USER GOALS
    // ============================================================

    @Query(
        """
        SELECT *
        FROM goals
        WHERE user_id = :userId
        ORDER BY created_at DESC
        """
    )
    fun getGoalsByUser(
        userId: Int
    ): Flow<List<Goal>>


    // ============================================================
    // GET GOAL BY ID
    // ============================================================

    @Query(
        """
        SELECT *
        FROM goals
        WHERE goal_id = :goalId
        LIMIT 1
        """
    )
    suspend fun getGoalById(
        goalId: Int
    ): Goal?


    // ============================================================
    // GET GOAL FOR SPECIFIC USER
    // ============================================================

    @Query(
        """
        SELECT *
        FROM goals
        WHERE goal_id = :goalId
        AND user_id = :userId
        LIMIT 1
        """
    )
    suspend fun getGoalByIdForUser(
        goalId: Int,
        userId: Int
    ): Goal?


    // ============================================================
    // UPDATE CURRENT AMOUNT
    // ============================================================

    @Query(
        """
        UPDATE goals
        SET current_amount = :newAmount
        WHERE goal_id = :goalId
        AND user_id = :userId
        """
    )
    suspend fun updateCurrentAmount(
        goalId: Int,
        userId: Int,
        newAmount: Double
    )


    // ============================================================
    // DELETE BY USER
    // ============================================================

    @Query(
        """
        DELETE FROM goals
        WHERE goal_id = :goalId
        AND user_id = :userId
        """
    )
    suspend fun deleteGoalById(
        goalId: Int,
        userId: Int
    )
}