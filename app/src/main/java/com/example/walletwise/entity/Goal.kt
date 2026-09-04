package com.example.walletwise.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goals",
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
data class Goal(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "goal_id")
    val goalId: Int = 0,

    @ColumnInfo(name = "user_id")
    val userId: Int,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "target_amount")
    val targetAmount: Double,

    @ColumnInfo(name = "current_amount")
    val currentAmount: Double = 0.0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {

    val progressPercent: Int
        get() {
            if (targetAmount <= 0.0) return 0

            return (
                    (currentAmount / targetAmount * 100).toInt()
                    ).coerceIn(0, 100)
        }
}