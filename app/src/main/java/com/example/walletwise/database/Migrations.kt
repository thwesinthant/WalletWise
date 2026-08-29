package com.example.walletwise.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `userId` INTEGER NOT NULL,
                `label` TEXT NOT NULL,
                `iconRes` INTEGER NOT NULL,
                `tintColor` INTEGER NOT NULL,
                `bgColor` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                FOREIGN KEY(`userId`) REFERENCES `users`(`userId`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_userId` ON `categories` (`userId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transactions` (
                `transaction_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `user_id` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `type` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `payment_method` TEXT NOT NULL,
                `note` TEXT,
                `created_at` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}