package com.example.walletwise.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.walletwise.dao.AccountDao
import com.example.walletwise.dao.BudgetDao
import com.example.walletwise.dao.CategoryDao
import com.example.walletwise.dao.GoalDao
import com.example.walletwise.dao.NotificationDao
import com.example.walletwise.dao.TransactionDao
import com.example.walletwise.dao.UserDao
import com.example.walletwise.entity.Account
import com.example.walletwise.entity.Budget
import com.example.walletwise.entity.Goal
import com.example.walletwise.entity.BudgetCategory
import com.example.walletwise.entity.CategoryEntity
import com.example.walletwise.entity.Notification
import com.example.walletwise.entity.Transaction
import com.example.walletwise.entity.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Notification::class,
        CategoryEntity::class,
        Transaction::class,
        Account::class,
        Budget::class,
        BudgetCategory::class,
        Goal::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun accountDao(): AccountDao

    abstract fun notificationDao(): NotificationDao

    abstract fun categoryDao(): CategoryDao

    abstract fun transactionDao(): TransactionDao

    abstract fun budgetDao(): BudgetDao

    abstract fun goalDao(): GoalDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(
            context: Context
        ): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "walletwise"
                    )
                        .addMigrations(
                            MIGRATION_1_2,
                            MIGRATION_2_3,
                            MIGRATION_3_4,
                            MIGRATION_5_6
                        )
                        .fallbackToDestructiveMigration()
                        .build()

                INSTANCE =
                    instance

                instance
            }
        }

    }
}