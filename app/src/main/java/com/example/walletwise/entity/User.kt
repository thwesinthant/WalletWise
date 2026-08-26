package com.example.walletwise.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true)
    ]
)
data class User(

    @PrimaryKey(autoGenerate = true)
    val userId: Int = 0,
    val fullName: String,
    val email: String,
    val password: String,
    val profileImage: String? = null,
    val currency: String = "MMK",
    val createdAt: Long = System.currentTimeMillis()
)