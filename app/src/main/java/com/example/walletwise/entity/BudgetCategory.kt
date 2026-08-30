package com.example.walletwise.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budget_categories",
    foreignKeys = [
        ForeignKey(
            entity = Budget::class,
            parentColumns = ["budgetId"],
            childColumns = ["budget_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["budget_id"]),
        Index(value = ["category_id"]),
        Index(
            value = ["budget_id", "category_id"],
            unique = true
        )
    ]
)
data class BudgetCategory(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "budget_category_id")
    val budgetCategoryId: Int = 0,

    @ColumnInfo(name = "budget_id")
    val budgetId: Int,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    @ColumnInfo(name = "limit_amount")
    val limitAmount: Double
)