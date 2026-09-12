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
    suspend fun insertNotificationAndGetId(
        notification: Notification
    ): Long



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
        SELECT *
        FROM notifications
        WHERE notification_id = :notificationId
        LIMIT 1
        """
    )
    suspend fun getNotificationById(
        notificationId: Int
    ): Notification?


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
        UPDATE notifications
        SET is_read = 1
        WHERE user_id = :userId
        AND is_read = 0
        """
    )
    suspend fun markAllAsRead(
        userId: Int
    )


    @Query(
        """
        DELETE FROM notifications
        WHERE notification_id = :notificationId
        """
    )
    suspend fun deleteNotification(
        notificationId: Int
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

