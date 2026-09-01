package com.example.walletwise.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.walletwise.entity.Notification
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(
        notification: Notification
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(
        notifications: List<Notification>
    )

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
    // BUDGET EXCEEDED NOTIFICATION CHECK
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
    // CATEGORY BUDGET EXCEEDED NOTIFICATION CHECK
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
}