package com.example.walletwise.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
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
data class CategoryEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "category_id")
    val id: Long = 0L,

    @ColumnInfo(name = "user_id")
    val userId: Int,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "icon_name")
    val iconName: String,

    @ColumnInfo(name = "tint_color")
    val tintColor: Int,

    @ColumnInfo(name = "bg_color")
    val bgColor: Int,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int
)