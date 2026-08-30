package com.example.walletwise.category

import android.content.Context
import com.example.walletwise.entity.Category
import com.example.walletwise.entity.CategoryEntity

const val DEFAULT_USER_ID: Int = 1

fun CategoryEntity.toCategory(
    context: Context
): Category {

    return Category(
        label = label,

        iconRes =
            context.getCategoryIconRes(
                iconName
            ),

        tintColor = tintColor,

        bgColor = bgColor,

        isAddAction = false,

        id = id
    )
}

fun Category.toEntity(
    context: Context,
    sortOrder: Int = 0,
    userId: Int = DEFAULT_USER_ID
): CategoryEntity {

    val iconName =
        context.resources.getResourceEntryName(
            iconRes
        )

    return CategoryEntity(
        id = id,

        userId = userId,

        label = label,

        iconName = iconName,

        tintColor = tintColor,

        bgColor = bgColor,

        sortOrder = sortOrder
    )
}