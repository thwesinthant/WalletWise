package com.example.walletwise.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.walletwise.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert
    suspend fun insertUser(user: User): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(users: List<User>)

    @Query(
        """
        SELECT * FROM users
        WHERE user_id = :userId
        LIMIT 1
        """
    )
    fun getUserById(userId: Int): Flow<User?>

    /*
     * Used when a screen needs the user immediately,
     * for example to get the user's selected currency.
     */
    @Query(
        """
        SELECT * FROM users
        WHERE user_id = :userId
        LIMIT 1
        """
    )
    suspend fun getUserByIdOnce(userId: Int): User?

    @Query(
        """
        SELECT * FROM users
        WHERE email = :email
        LIMIT 1
        """
    )
    suspend fun getUserByEmail(email: String): User?

    @Query(
        """
        SELECT * FROM users
        WHERE email = :email
        AND password = :password
        LIMIT 1
        """
    )
    suspend fun login(
        email: String,
        password: String
    ): User?

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM users
            WHERE email = :email
        )
        """
    )
    suspend fun emailExists(email: String): Boolean

    @Query(
        """
        UPDATE users
        SET password = :newPasswordHash
        WHERE email = :email
        """
    )
    suspend fun updatePassword(
        email: String,
        newPasswordHash: String
    )

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    @Update
    suspend fun updateUser(user: User)
}