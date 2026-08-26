package com.example.walletwise.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.walletwise.entity.Notification
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    // Notification တစ်ခုချင်းစီ ထည့်ရန်
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)

    // SeedData မှ Notification အများအပြား (List) တစ်ခါတည်း ထည့်ရန် (Error ပျောက်စေမည့် အပိုင်း)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<Notification>)

    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY CAST(created_at AS INTEGER) DESC")
    fun getNotificationsByUser(userId: Int): Flow<List<Notification>>

    @Query("UPDATE notifications SET is_read = 1 WHERE notification_id = :id")
    suspend fun markAsRead(id: Int)

    @Query("DELETE FROM notifications WHERE user_id = :userId")
    suspend fun clearAllNotifications(userId: Int)
}