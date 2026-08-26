package com.example.walletwise.entity

data class Category(
    val label: String,
    val iconRes: Int,
    val tintColor: Int,
    val bgColor: Int = 0,
    val isAddAction: Boolean = false,
    // Room row id. 0L for the "Add" tile and for a category that hasn't been saved yet.
    val id: Long = 0L
)
