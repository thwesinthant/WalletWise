package com.example.walletwise.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
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
data class Account(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "account_id")
    val accountId: Int = 0,

    @ColumnInfo(name = "user_id")
    val userId: Int,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "opening_balance")
    val openingBalance: Double = 0.0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)


data class AccountBalance(

    @ColumnInfo(name = "account_id")
    val accountId: Int,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "opening_balance")
    val openingBalance: Double,

    @ColumnInfo(name = "current_balance")
    val currentBalance: Double
)