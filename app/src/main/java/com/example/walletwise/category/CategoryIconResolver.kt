package com.example.walletwise.category

import android.content.Context

fun Context.getCategoryIconRes(
    iconName: String
): Int {

    val resourceId =
        resources.getIdentifier(
            iconName,
            "drawable",
            packageName
        )

    // Prevent crashes if a drawable name is invalid.
    return if (resourceId != 0) {
        resourceId
    } else {
        0
    }
}