package com.example.walletwise.category

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.walletwise.R
import com.example.walletwise.dao.CategoryDao
import com.example.walletwise.dao.UserDao
import com.example.walletwise.database.AppDatabase
import com.example.walletwise.database.CategorySeedLoader
import com.example.walletwise.entity.Category
import com.example.walletwise.entity.CategoryEntity
import com.example.walletwise.entity.User
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class SelectCategoryActivity : AppCompatActivity() {

    // ============================================================
    // DATABASE
    // ============================================================

    private lateinit var userDao: UserDao
    private lateinit var categoryDao: CategoryDao

    // ============================================================
    // ADAPTER
    // ============================================================

    private lateinit var adapter: CategoryAdapter

    // ============================================================
    // CATEGORY STATE
    // ============================================================

    private var currentEntities: List<CategoryEntity> =
        emptyList()

    private var currentUserId: Int = -1

    // ============================================================
    // ON CREATE
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_select_category
        )

        // ========================================================
        // GET USER ID
        // ========================================================

        currentUserId =
            intent.getIntExtra(
                EXTRA_USER_ID,
                -1
            )

        // ========================================================
        // DATABASE
        // ========================================================

        val database =
            AppDatabase.getDatabase(this)

        userDao =
            database.userDao()

        categoryDao =
            database.categoryDao()

        // ========================================================
        // TOOLBAR
        // ========================================================

        val toolbar =
            findViewById<MaterialToolbar>(
                R.id.toolbar
            )

        toolbar.title =
            "Categories"

        toolbar.setNavigationOnClickListener {
            finish()
        }

        // ========================================================
        // RECYCLER VIEW
        // ========================================================

        val recyclerView =
            findViewById<RecyclerView>(
                R.id.categoryRecyclerView
            )

        recyclerView.layoutManager =
            GridLayoutManager(
                this,
                4
            )

        adapter =
            CategoryAdapter(
                mutableListOf(),

                // =================================================
                // NORMAL TAP
                // =================================================
                onClick = { selected ->

                    handleCategoryClick(
                        selected
                    )
                },

                // =================================================
                // LONG PRESS
                // =================================================
                onLongClick = { selected ->

                    handleCategoryLongClick(
                        selected
                    )
                }
            )

        recyclerView.adapter =
            adapter

        // ========================================================
        // LOAD USER + CATEGORIES
        // ========================================================

        lifecycleScope.launch {

            // ====================================================
            // FALLBACK LOCAL USER
            // ====================================================

            if (currentUserId == -1) {

                val existingUser =
                    userDao.getUserByEmail(
                        DEFAULT_USER_EMAIL
                    )

                currentUserId =
                    existingUser?.userId
                        ?: userDao.insertUser(
                            User(
                                fullName = "Local User",
                                email = DEFAULT_USER_EMAIL,
                                password = ""
                            )
                        ).toInt()
            }

            // ====================================================
            // SEED DEFAULT CATEGORIES
            // ====================================================

            if (
                categoryDao.countByUserId(
                    currentUserId
                ) == 0
            ) {

                val defaultCategories =
                    CategorySeedLoader.loadDefaultEntities(
                        this@SelectCategoryActivity,
                        currentUserId
                    )

                categoryDao.insertAll(
                    defaultCategories
                )
            }

            // ====================================================
            // OBSERVE CATEGORIES
            // ====================================================

            categoryDao
                .observeAll(
                    currentUserId
                )
                .collect { entities ->

                    currentEntities =
                        entities

                    val displayList =
                        mutableListOf<Category>()

                    // =================================================
                    // ADD CATEGORY TILE
                    // =================================================

                    displayList.add(
                        Category(
                            label = "Add",
                            iconRes = R.drawable.ic_plus,
                            tintColor = Color.TRANSPARENT,
                            isAddAction = true
                        )
                    )

                    // =================================================
                    // NORMAL CATEGORIES
                    // =================================================

                    displayList.addAll(
                        entities.map { entity ->

                            entity.toCategory(
                                this@SelectCategoryActivity
                            )
                        }
                    )

                    adapter.submitList(
                        displayList
                    )
                }
        }
    }

    // ============================================================
    // HANDLE NORMAL CLICK
    // ============================================================

    private fun handleCategoryClick(
        selected: Category
    ) {

        // ========================================================
        // ADD CATEGORY
        // ========================================================

        if (selected.isAddAction) {

            showAddOrEditSheet(
                existing = null
            )

            return
        }

        // ========================================================
        // SELECT CATEGORY
        // ========================================================

        val resultIntent =
            Intent()

        resultIntent.putExtra(
            RESULT_CATEGORY_ID,
            selected.id
        )

        resultIntent.putExtra(
            RESULT_CATEGORY_LABEL,
            selected.label
        )

        resultIntent.putExtra(
            RESULT_CATEGORY_ICON,
            selected.iconRes
        )

        setResult(
            RESULT_OK,
            resultIntent
        )

        finish()
    }

    // ============================================================
    // HANDLE LONG PRESS
    // ============================================================

    private fun handleCategoryLongClick(
        selected: Category
    ) {

        // ========================================================
        // DO NOTHING FOR ADD TILE
        // ========================================================

        if (selected.isAddAction) {
            return
        }

        // ========================================================
        // FIND ENTITY
        // ========================================================

        val entity =
            currentEntities.firstOrNull {

                it.id == selected.id

            }

        if (entity != null) {

            showCategoryActionsSheet(
                entity
            )
        }
    }

    // ============================================================
    // ADD / EDIT CATEGORY
    // ============================================================

    private fun showAddOrEditSheet(
        existing: CategoryEntity?
    ) {

        AddCategoryBottomSheet(

            existing =
                existing?.toCategory(
                    this@SelectCategoryActivity
                )

        ) { result ->

            lifecycleScope.launch {

                // =================================================
                // ADD
                // =================================================

                if (existing == null) {

                    val nextOrder =
                        (
                                categoryDao.minSortOrder(
                                    currentUserId
                                ) ?: 0
                                ) - 1

                    categoryDao.insert(

                        result.toEntity(

                            context =
                                this@SelectCategoryActivity,

                            sortOrder =
                                nextOrder,

                            userId =
                                currentUserId
                        )
                    )

                }

                // =================================================
                // EDIT
                // =================================================

                else {

                    val updatedEntity =
                        result.toEntity(

                            context =
                                this@SelectCategoryActivity,

                            sortOrder =
                                existing.sortOrder,

                            userId =
                                existing.userId

                        ).copy(

                            id =
                                existing.id
                        )

                    categoryDao.update(
                        updatedEntity
                    )
                }
            }
        }.show(
            supportFragmentManager,
            "add_edit_category"
        )
    }

    // ============================================================
    // CATEGORY ACTIONS
    // ============================================================

    private fun showCategoryActionsSheet(
        entity: CategoryEntity
    ) {

        CategoryActionsBottomSheet(

            categoryLabel =
                entity.label,

            onEdit = {

                showAddOrEditSheet(
                    existing = entity
                )
            },

            onDelete = {

                confirmDelete(
                    entity
                )
            }

        ).show(
            supportFragmentManager,
            "category_actions"
        )
    }

    // ============================================================
    // DELETE CATEGORY
    // ============================================================

    private fun confirmDelete(
        entity: CategoryEntity
    ) {

        AlertDialog.Builder(
            this,
            R.style.DeleteCategoryDialogTheme
        )
            .setTitle(
                "Delete category?"
            )
            .setMessage(
                "\"${entity.label}\" will be removed. This can't be undone."
            )
            .setPositiveButton(
                "Delete"
            ) { dialog, _ ->

                lifecycleScope.launch {

                    categoryDao.delete(
                        entity
                    )
                }

                dialog.dismiss()
            }
            .setNegativeButton(
                "Cancel"
            ) { dialog, _ ->

                dialog.dismiss()
            }
            .show()
    }

    // ============================================================
    // CONSTANTS
    // ============================================================

    companion object {

        const val EXTRA_USER_ID =
            "EXTRA_USER_ID"

        const val RESULT_CATEGORY_ID =
            "RESULT_CATEGORY_ID"

        const val RESULT_CATEGORY_LABEL =
            "RESULT_CATEGORY_LABEL"

        const val RESULT_CATEGORY_ICON =
            "RESULT_CATEGORY_ICON"

        private const val DEFAULT_USER_EMAIL =
            "local@device"
    }
}