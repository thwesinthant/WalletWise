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

    @Query("""
        SELECT * 
        FROM categories 
        WHERE userId = :userId
        ORDER BY sortOrder ASC, id ASC
    """)
    fun observeAll(userId: Int): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("""
        SELECT COUNT(*) 
        FROM categories 
        WHERE userId = :userId
    """)
    suspend fun countByUserId(userId: Int): Int

    @Query("""
        SELECT MIN(sortOrder) 
        FROM categories
        WHERE userId = :userId
    """)
    suspend fun minSortOrder(userId: Int): Int?

    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)
}