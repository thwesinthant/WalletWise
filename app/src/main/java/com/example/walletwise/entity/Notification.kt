package com.example.walletwise.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"])
    ]
)
data class Notification(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "notification_id")
    val notificationId: Int = 0,

    @ColumnInfo(name = "user_id")
    val userId: Int,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "reference_type")
    val referenceType: String? = null,

    @ColumnInfo(name = "reference_id")
    val referenceId: Int? = null,

    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,

    @ColumnInfo(name = "time_ago")
    val timeAgo: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: String = System.currentTimeMillis().toString()
)