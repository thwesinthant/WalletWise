package com.example.walletwise.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {

    override fun migrate(
        db: SupportSQLiteDatabase
    ) {

        // =========================================================
        // ACCOUNTS
        // =========================================================

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `accounts` (
                `accountId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `userId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `openingBalance` REAL NOT NULL,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`userId`)
                    REFERENCES `users`(`userId`)
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_accounts_userId`
            ON `accounts` (`userId`)
            """.trimIndent()
        )


        // =========================================================
        // BUDGETS
        // =========================================================

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `budgets` (
                `budgetId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `user_id` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `start_date` INTEGER NOT NULL,
                `end_date` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                FOREIGN KEY(`user_id`)
                    REFERENCES `users`(`userId`)
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_budgets_user_id`
            ON `budgets` (`user_id`)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_budgets_user_id_start_date`
            ON `budgets` (`user_id`, `start_date`)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_budgets_user_id_end_date`
            ON `budgets` (`user_id`, `end_date`)
            """.trimIndent()
        )


        // =========================================================
        // BUDGET CATEGORIES
        // =========================================================

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `budget_categories` (
                `budget_category_id`
                    INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,

                `budget_id`
                    INTEGER NOT NULL,

                `category_id`
                    INTEGER NOT NULL,

                `limit_amount`
                    REAL NOT NULL,

                FOREIGN KEY(`budget_id`)
                    REFERENCES `budgets`(`budgetId`)
                    ON DELETE CASCADE,

                FOREIGN KEY(`category_id`)
                    REFERENCES `categories`(`id`)
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_budget_categories_budget_id`
            ON `budget_categories` (`budget_id`)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_budget_categories_category_id`
            ON `budget_categories` (`category_id`)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS
            `index_budget_categories_budget_id_category_id`
            ON `budget_categories`
            (`budget_id`, `category_id`)
            """.trimIndent()
        )


        // =========================================================
        // TRANSACTIONS
        //
        // Old table:
        // category TEXT
        // payment_method TEXT
        //
        // New table:
        // category_id INTEGER
        // account_id INTEGER
        // =========================================================

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS
            `transactions_new` (

                `transaction_id`
                    INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,

                `user_id`
                    INTEGER NOT NULL,

                `title`
                    TEXT NOT NULL,

                `amount`
                    REAL NOT NULL,

                `type`
                    TEXT NOT NULL,

                `category_id`
                    INTEGER,

                `account_id`
                    INTEGER,

                `note`
                    TEXT,

                `created_at`
                    INTEGER NOT NULL,

                FOREIGN KEY(`user_id`)
                    REFERENCES `users`(`userId`)
                    ON DELETE CASCADE,

                FOREIGN KEY(`account_id`)
                    REFERENCES `accounts`(`accountId`)
                    ON DELETE SET NULL,

                FOREIGN KEY(`category_id`)
                    REFERENCES `categories`(`id`)
                    ON DELETE SET NULL
            )
            """.trimIndent()
        )


        // ---------------------------------------------------------
        // COPY EXISTING TRANSACTIONS
        //
        // Existing category/payment information cannot safely be
        // converted automatically into IDs without additional mapping.
        //
        // Therefore:
        // category_id = NULL
        // account_id = NULL
        // ---------------------------------------------------------

        db.execSQL(
            """
            INSERT INTO `transactions_new` (
                `transaction_id`,
                `user_id`,
                `title`,
                `amount`,
                `type`,
                `category_id`,
                `account_id`,
                `note`,
                `created_at`
            )
            SELECT
                `transaction_id`,
                `user_id`,
                `title`,
                `amount`,
                `type`,
                NULL,
                NULL,
                `note`,
                `created_at`
            FROM `transactions`
            """.trimIndent()
        )


        // ---------------------------------------------------------
        // REPLACE OLD TABLE
        // ---------------------------------------------------------

        db.execSQL(
            "DROP TABLE `transactions`"
        )

        db.execSQL(
            """
            ALTER TABLE `transactions_new`
            RENAME TO `transactions`
            """.trimIndent()
        )


        // =========================================================
        // TRANSACTION INDEXES
        // =========================================================

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_transactions_user_id`
            ON `transactions` (`user_id`)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_transactions_account_id`
            ON `transactions` (`account_id`)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_transactions_category_id`
            ON `transactions` (`category_id`)
            """.trimIndent()
        )
    }
}