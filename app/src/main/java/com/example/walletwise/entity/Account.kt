package com.example.walletwise.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"])
    ]
)
data class Account(
    @PrimaryKey(autoGenerate = true)
    val accountId: Int = 0,

    val userId: Int,

    val name: String,

    val openingBalance: Double = 0.0,

    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Query result shape for computed balances (opening + net transactions).
 * Not a table — used as a @Query return type in AccountDao.
 */
data class AccountBalance(
    val accountId: Int,
    val name: String,
    val openingBalance: Double,
    val currentBalance: Double
)