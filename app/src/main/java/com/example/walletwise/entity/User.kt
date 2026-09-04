package com.example.walletwise.entity

import androidx.room.ColumnInfo
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
    @ColumnInfo(name = "user_id")
    val userId: Int = 0,

    @ColumnInfo(name = "full_name")
    val fullName: String,

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "password")
    val password: String,

    @ColumnInfo(name = "profile_image")
    val profileImage: String? = null,

    @ColumnInfo(name = "currency")
    val currency: String = "MMK",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)