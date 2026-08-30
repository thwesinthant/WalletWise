package com.example.walletwise.entity

import androidx.room.Embedded
import androidx.room.Relation

data class BudgetWithCategories(

    @Embedded
    val budget: Budget,

    @Relation(
        parentColumn = "budgetId",
        entityColumn = "budget_id"
    )
    val categories: List<BudgetCategory>
)