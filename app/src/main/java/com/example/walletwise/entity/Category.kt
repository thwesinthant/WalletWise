package com.example.walletwise.entity

data class Category(
    val label: String,
    val iconRes: Int,
    val tintColor: Int,
    val bgColor: Int = 0,
    val isAddAction: Boolean = false,

    // Room row ID.
    // 0L is used for the Add tile or a category not yet saved.
    val id: Long = 0L
)