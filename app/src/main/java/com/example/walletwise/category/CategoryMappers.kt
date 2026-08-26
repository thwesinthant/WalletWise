package com.example.walletwise.category

import com.example.walletwise.entity.Category
import com.example.walletwise.entity.CategoryEntity


const val DEFAULT_USER_ID: Int = 1

/**
 * Converts a Room CategoryEntitiy into a UI Category.
 */
fun CategoryEntity.toCategory(): Category {
    return Category(
        label = label,
        iconRes = iconRes,
        tintColor = tintColor,
        bgColor = bgColor,
        isAddAction = false,
        id = id
    )
}

/**
 * Converts a UI Category into a Room CategoryEntitiy.
 */
fun Category.toEntity(
    sortOrder: Int = 0,
    userId: Int = DEFAULT_USER_ID
): CategoryEntity {
    return CategoryEntity(
        id = id,
        userId = userId,
        label = label,
        iconRes = iconRes,
        tintColor = tintColor,
        bgColor = bgColor,
        sortOrder = sortOrder
    )
}