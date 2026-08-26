package com.example.walletwise.goal

data class Goal(
    val title: String,
    val targetAmount: Int,
    var currentAmount: Int = 0
) {
    val progressPercent: Int
        get() = if (targetAmount == 0) 0 else ((currentAmount.toFloat() / targetAmount) * 100).toInt()
}
