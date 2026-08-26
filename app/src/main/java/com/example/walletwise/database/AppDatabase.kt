package com.example.walletwise.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.walletwise.dao.NotificationDao
import com.example.walletwise.dao.UserDao
import com.example.walletwise.entity.Notification
import com.example.walletwise.entity.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Notification::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun notificationDao(): NotificationDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "walletwise"
                )
                    .addCallback(AppDatabaseCallback())
                    .build()

                INSTANCE = instance

                instance
            }
        }

        private class AppDatabaseCallback : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                INSTANCE?.let { database ->

                    CoroutineScope(Dispatchers.IO).launch {

                        database.notificationDao().insertAll(
                            SeedData.getDefaultNotifications()
                        )
                    }
                }
            }
        }
    }
}