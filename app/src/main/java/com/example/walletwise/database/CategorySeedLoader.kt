package com.example.walletwise.database

import android.content.Context
import android.graphics.Color
import com.example.walletwise.entity.CategoryEntity
import org.json.JSONArray

object CategorySeedLoader {

    fun loadDefaultEntities(
        context: Context,
        userId: Int
    ): List<CategoryEntity> {

        val json =
            context.assets
                .open("categories_seed.json")
                .bufferedReader()
                .use {
                    it.readText()
                }

        val array =
            JSONArray(json)

        val entities =
            mutableListOf<CategoryEntity>()

        for (index in 0 until array.length()) {

            val obj =
                array.getJSONObject(index)

            entities.add(
                CategoryEntity(
                    userId = userId,

                    label =
                        obj.getString("label"),

                    // Store stable drawable NAME.
                    iconName =
                        obj.getString("icon"),

                    tintColor =
                        Color.parseColor(
                            obj.getString("tintColor")
                        ),

                    bgColor =
                        Color.parseColor(
                            obj.getString("bgColor")
                        ),

                    sortOrder = index
                )
            )
        }

        return entities
    }
}