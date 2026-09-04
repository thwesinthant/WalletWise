package com.example.walletwise.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.walletwise.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // ============================================================
    // OBSERVE ALL CATEGORIES FOR USER
    // ============================================================

    @Query(
        """
    SELECT *
    FROM categories
    WHERE user_id = :userId
    ORDER BY sort_order ASC, category_id ASC
    """
    )
    fun observeAll(
        userId: Int
    ): Flow<List<CategoryEntity>>

    // ============================================================
    // COUNT USER CATEGORIES
    // ============================================================

    @Query(
        """
    SELECT COUNT(*)
    FROM categories
    WHERE user_id = :userId
    """
    )
    suspend fun countByUserId(
        userId: Int
    ): Int

    // ============================================================
    // GET MINIMUM SORT ORDER
    // ============================================================

    @Query(
        """
    SELECT MIN(sort_order)
    FROM categories
    WHERE user_id = :userId
    """
    )
    suspend fun minSortOrder(
        userId: Int
    ): Int?

    // ============================================================
    // GET CATEGORY BY ID
    // IMPORTANT:
    // Also checks userId so one user cannot access another
    // user's category.
    // ============================================================

    @Query(
        """
    SELECT *
    FROM categories
    WHERE category_id = :categoryId
    AND user_id = :userId
    LIMIT 1
    """
    )
    suspend fun getCategoryById(
        categoryId: Long,
        userId: Int
    ): CategoryEntity?

    // ============================================================
    // INSERT MULTIPLE CATEGORIES
    // ============================================================

    @Insert
    suspend fun insertAll(
        categories: List<CategoryEntity>
    )

    // ============================================================
    // INSERT ONE CATEGORY
    // ============================================================

    @Insert
    suspend fun insert(
        category: CategoryEntity
    ): Long

    // ============================================================
    // UPDATE CATEGORY
    // ============================================================

    @Update
    suspend fun update(
        category: CategoryEntity
    )

    // ============================================================
    // DELETE CATEGORY
    // ============================================================

    @Delete
    suspend fun delete(
        category: CategoryEntity
    )
}