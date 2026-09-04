package com.example.walletwise.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.walletwise.entity.Notification
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    // ============================================================
    // INSERT NOTIFICATION
    // ============================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(
        notification: Notification
    )

    // ============================================================
    // INSERT NOTIFICATION AND RETURN ID
    // ============================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationAndGetId(
        notification: Notification
    ): Long

    // ============================================================
    // INSERT MULTIPLE
    // ============================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(
        notifications: List<Notification>
    )

    // ============================================================
    // GET USER NOTIFICATIONS
    // ============================================================

    @Query(
        """
        SELECT *
        FROM notifications
        WHERE user_id = :userId
        ORDER BY CAST(created_at AS INTEGER) DESC
        """
    )
    fun getNotificationsByUser(
        userId: Int
    ): Flow<List<Notification>>

    // ============================================================
    // GET ONE NOTIFICATION
    // ============================================================

    @Query(
        """
        SELECT *
        FROM notifications
        WHERE notification_id = :notificationId
        LIMIT 1
        """
    )
    suspend fun getNotificationById(
        notificationId: Int
    ): Notification?

    // ============================================================
    // MARK AS READ
    // ============================================================

    @Query(
        """
        UPDATE notifications
        SET is_read = 1
        WHERE notification_id = :id
        """
    )
    suspend fun markAsRead(
        id: Int
    )

    // ============================================================
    // MARK ALL AS READ
    // ============================================================

    @Query(
        """
        UPDATE notifications
        SET is_read = 1
        WHERE user_id = :userId
        AND is_read = 0
        """
    )
    suspend fun markAllAsRead(
        userId: Int
    )

    // ============================================================
    // DELETE SINGLE
    // ============================================================

    @Query(
        """
        DELETE FROM notifications
        WHERE notification_id = :notificationId
        """
    )
    suspend fun deleteNotification(
        notificationId: Int
    )

    // ============================================================
    // CLEAR ALL
    // ============================================================

    @Query(
        """
        DELETE FROM notifications
        WHERE user_id = :userId
        """
    )
    suspend fun clearAllNotifications(
        userId: Int
    )

    // ============================================================
    // UNREAD COUNT
    // ============================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM notifications
        WHERE user_id = :userId
        AND is_read = 0
        """
    )
    fun getUnreadNotificationCount(
        userId: Int
    ): Flow<Int>

    // ============================================================
    // OVERALL BUDGET EXCEEDED CHECK
    // ============================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM notifications
        WHERE user_id = :userId
        AND type = 'BUDGET_EXCEEDED'
        AND reference_type = 'BUDGET'
        AND reference_id = :budgetId
        """
    )
    suspend fun countBudgetExceededNotification(
        userId: Int,
        budgetId: Int
    ): Int

    // ============================================================
    // CATEGORY BUDGET EXCEEDED CHECK
    // ============================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM notifications
        WHERE user_id = :userId
        AND type = 'CATEGORY_BUDGET_EXCEEDED'
        AND reference_type = 'BUDGET_CATEGORY'
        AND reference_id = :budgetCategoryId
        """
    )
    suspend fun countCategoryBudgetExceededNotification(
        userId: Int,
        budgetCategoryId: Int
    ): Int

    // ============================================================
    // GOAL COMPLETED CHECK
    // ============================================================

    @Query(
        """
        SELECT COUNT(*)
        FROM notifications
        WHERE user_id = :userId
        AND type = 'GOAL_COMPLETED'
        AND reference_type = 'GOAL'
        AND reference_id = :goalId
        """
    )
    suspend fun countGoalCompletedNotification(
        userId: Int,
        goalId: Int
    ): Int
}

