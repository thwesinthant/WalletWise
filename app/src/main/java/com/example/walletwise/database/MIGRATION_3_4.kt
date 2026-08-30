package com.example.walletwise.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {

    override fun migrate(
        db: SupportSQLiteDatabase
    ) {

        db.execSQL(
            """
            ALTER TABLE `transactions`
            ADD COLUMN `transfer_group_id` TEXT
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_transactions_transfer_group_id`
            ON `transactions` (`transfer_group_id`)
            """.trimIndent()
        )
    }
}