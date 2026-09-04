package com.example.walletwise.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {

    override fun migrate(
        db: SupportSQLiteDatabase
    ) {

        // ============================================================
        // USERS
        // Old v5:
        // userId, fullName, email, password, profileImage,
        // currency, createdAt
        //
        // New v6:
        // user_id, full_name, email, password, profile_image,
        // currency, created_at
        // ============================================================

        db.execSQL(
            """
            ALTER TABLE users
            RENAME COLUMN userId TO user_id
            """.trimIndent()
        )

        db.execSQL(
            """
            ALTER TABLE users
            RENAME COLUMN fullName TO full_name
            """.trimIndent()
        )

        db.execSQL(
            """
            ALTER TABLE users
            RENAME COLUMN profileImage TO profile_image
            """.trimIndent()
        )

        db.execSQL(
            """
            ALTER TABLE users
            RENAME COLUMN createdAt TO created_at
            """.trimIndent()
        )


        // ============================================================
        // ACCOUNTS
        // ============================================================

        db.execSQL(
            """
            ALTER TABLE accounts
            RENAME COLUMN accountId TO account_id
            """.trimIndent()
        )

        db.execSQL(
            """
            ALTER TABLE accounts
            RENAME COLUMN userId TO user_id
            """.trimIndent()
        )

        db.execSQL(
            """
            ALTER TABLE accounts
            RENAME COLUMN openingBalance TO opening_balance
            """.trimIndent()
        )

        db.execSQL(
            """
            ALTER TABLE accounts
            RENAME COLUMN createdAt TO created_at
            """.trimIndent()
        )


        // ============================================================
        // CATEGORIES
        // ============================================================

        db.execSQL(
            """
            ALTER TABLE categories
            RENAME COLUMN id TO category_id
            """.trimIndent()
        )

        db.execSQL(
            """
            ALTER TABLE categories
            RENAME COLUMN userId TO user_id
            """.trimIndent()
        )

        db.execSQL(
            """
            ALTER TABLE categories
            RENAME COLUMN iconName TO icon_name
            """.trimIndent()
        )

        db.execSQL(
            """
            ALTER TABLE categories
            RENAME COLUMN tintColor TO tint_color
            """.trimIndent()
        )

        db.execSQL(
            """
            ALTER TABLE categories
            RENAME COLUMN bgColor TO bg_color
            """.trimIndent()
        )

        db.execSQL(
            """
            ALTER TABLE categories
            RENAME COLUMN sortOrder TO sort_order
            """.trimIndent()
        )


        // ============================================================
        // BUDGETS
        //
        // If budgets did not exist in v5, create them.
        //
        // If budgets already existed in v5 with budgetId,
        // rename budgetId -> budget_id.
        // ============================================================

        if (!tableExists(db, "budgets")) {

            db.execSQL(
                """
                CREATE TABLE budgets (
                    budget_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    amount REAL NOT NULL,
                    start_date INTEGER NOT NULL,
                    end_date INTEGER NOT NULL,
                    created_at INTEGER NOT NULL,

                    FOREIGN KEY(user_id)
                        REFERENCES users(user_id)
                        ON DELETE CASCADE
                )
                """.trimIndent()
            )

        } else {

            // If the old table has budgetId, rename it.
            if (columnExists(db, "budgets", "budgetId")) {

                db.execSQL(
                    """
                    ALTER TABLE budgets
                    RENAME COLUMN budgetId TO budget_id
                    """.trimIndent()
                )
            }

            // Handle the unlikely case where the old table had
            // userId instead of user_id.
            if (columnExists(db, "budgets", "userId")) {

                db.execSQL(
                    """
                    ALTER TABLE budgets
                    RENAME COLUMN userId TO user_id
                    """.trimIndent()
                )
            }

            if (columnExists(db, "budgets", "startDate")) {

                db.execSQL(
                    """
                    ALTER TABLE budgets
                    RENAME COLUMN startDate TO start_date
                    """.trimIndent()
                )
            }

            if (columnExists(db, "budgets", "endDate")) {

                db.execSQL(
                    """
                    ALTER TABLE budgets
                    RENAME COLUMN endDate TO end_date
                    """.trimIndent()
                )
            }

            if (columnExists(db, "budgets", "createdAt")) {

                db.execSQL(
                    """
                    ALTER TABLE budgets
                    RENAME COLUMN createdAt TO created_at
                    """.trimIndent()
                )
            }
        }


        // ============================================================
        // BUDGET INDEXES
        // ============================================================

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            index_budgets_user_id
            ON budgets(user_id)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            index_budgets_user_id_start_date
            ON budgets(user_id, start_date)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            index_budgets_user_id_end_date
            ON budgets(user_id, end_date)
            """.trimIndent()
        )


        // ============================================================
        // BUDGET CATEGORIES
        //
        // Your current BudgetCategory is already completely
        // snake_case.
        // ============================================================

        if (!tableExists(db, "budget_categories")) {

            db.execSQL(
                """
                CREATE TABLE budget_categories (
                    budget_category_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    budget_id INTEGER NOT NULL,
                    category_id INTEGER NOT NULL,
                    limit_amount REAL NOT NULL,

                    FOREIGN KEY(budget_id)
                        REFERENCES budgets(budget_id)
                        ON DELETE CASCADE,

                    FOREIGN KEY(category_id)
                        REFERENCES categories(category_id)
                        ON DELETE CASCADE
                )
                """.trimIndent()
            )
        }


        // ============================================================
        // BUDGET CATEGORY INDEXES
        // ============================================================

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            index_budget_categories_budget_id
            ON budget_categories(budget_id)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            index_budget_categories_category_id
            ON budget_categories(category_id)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS
            index_budget_categories_budget_id_category_id
            ON budget_categories(budget_id, category_id)
            """.trimIndent()
        )
    }


    // ================================================================
    // CHECK WHETHER A TABLE EXISTS
    // ================================================================

    private fun tableExists(
        database: SupportSQLiteDatabase,
        tableName: String
    ): Boolean {

        database.query(
            """
            SELECT name
            FROM sqlite_master
            WHERE type = 'table'
            AND name = ?
            """,
            arrayOf(tableName)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }


    // ================================================================
    // CHECK WHETHER A COLUMN EXISTS
    // ================================================================

    private fun columnExists(
        database: SupportSQLiteDatabase,
        tableName: String,
        columnName: String
    ): Boolean {

        database.query(
            "PRAGMA table_info($tableName)"
        ).use { cursor ->

            val nameIndex = cursor.getColumnIndex("name")

            while (cursor.moveToNext()) {

                if (cursor.getString(nameIndex) == columnName) {
                    return true
                }
            }
        }

        return false
    }
}