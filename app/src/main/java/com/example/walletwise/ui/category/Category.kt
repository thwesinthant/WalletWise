package com.example.walletwise.ui.category

data class Category(
    val label: String,
    val iconRes: Int,
    val tintColor: Int,
    val bgColor: Int = 0,
    val isAddAction: Boolean = false
)