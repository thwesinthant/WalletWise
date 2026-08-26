package com.example.walletwise.database

import android.content.Context
import android.graphics.Color
import com.example.walletwise.entity.CategoryEntity
import org.json.JSONArray

object CategorySeedLoader {

    private const val DEFAULT_USER_ID = 0

    fun loadDefaultEntities(
        context: Context,
        userId: Int = DEFAULT_USER_ID
    ): List<CategoryEntity> {

        val json = context.assets
            .open("categories_seed.json")
            .bufferedReader()
            .use { it.readText() }

        val array = JSONArray(json)
        val entities = mutableListOf<CategoryEntity>()

        for (index in 0 until array.length()) {

            val obj = array.getJSONObject(index)

            val iconName = obj.getString("icon")

            val iconRes = context.resources.getIdentifier(
                iconName,
                "drawable",
                context.packageName
            )

            // Skip if drawable doesn't exist
            if (iconRes == 0) {
                continue
            }

            entities.add(
                CategoryEntity(
                    userId = userId,
                    label = obj.getString("label"),
                    iconRes = iconRes,
                    tintColor = Color.parseColor(
                        obj.getString("tintColor")
                    ),
                    bgColor = Color.parseColor(
                        obj.getString("bgColor")
                    ),
                    sortOrder = index
                )
            )
        }

        return entities
    }
}