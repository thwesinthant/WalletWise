package com.example.walletwise.category

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.walletwise.CategoryAdapter
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

    private lateinit var userDao: UserDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var adapter: CategoryAdapter

    private var currentEntities: List<CategoryEntity> = emptyList()

    private var currentUserId: Int = 0

    /*
     * TRUE  = user is selecting a category for transaction
     * FALSE = user is managing categories
     */
    private var selectMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_select_category
        )

        // ========================================================
        // GET MODE
        // ========================================================

        selectMode = intent.getBooleanExtra(
            EXTRA_SELECT_MODE,
            false
        )

        // ========================================================
        // TOOLBAR
        // ========================================================

        val toolbar =
            findViewById<MaterialToolbar>(
                R.id.toolbar
            )

        toolbar.title =
            if (selectMode) {
                "Select category"
            } else {
                "Manage categories"
            }

        toolbar.setNavigationOnClickListener {
            finish()
        }

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
        // RECYCLERVIEW
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
                mutableListOf()
            ) { selected ->

                handleCategoryClick(selected)
            }

        recyclerView.adapter =
            adapter

        // ========================================================
        // LOAD USER + CATEGORIES
        // ========================================================

        lifecycleScope.launch {

            /*
             * IMPORTANT:
             * Prefer the USER_ID passed from the previous Activity.
             */
            val passedUserId =
                intent.getIntExtra(
                    EXTRA_USER_ID,
                    -1
                )

            if (passedUserId != -1) {

                currentUserId =
                    passedUserId

            } else {

                /*
                 * Fallback for your existing Category Management
                 * implementation.
                 */
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
            // SEED CATEGORIES IF EMPTY
            // ====================================================

            if (
                categoryDao.countByUserId(
                    currentUserId
                ) == 0
            ) {

                val defaultCategories =
                    CategorySeedLoader
                        .loadDefaultEntities(
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

                    /*
                     * MANAGE MODE:
                     * Show Add button.
                     */
                    if (!selectMode) {

                        displayList.add(
                            Category(
                                label = "Add",
                                iconRes =
                                    R.drawable.ic_plus,
                                tintColor =
                                    Color.TRANSPARENT,
                                isAddAction =
                                    true
                            )
                        )
                    }

                    /*
                     * Show all categories.
                     */
                    displayList.addAll(
                        entities.map {
                            it.toCategory()
                        }
                    )

                    adapter.submitList(
                        displayList
                    )
                }
        }
    }

    // ============================================================
    // HANDLE CATEGORY CLICK
    // ============================================================

    private fun handleCategoryClick(
        selected: Category
    ) {

        /*
         * MODE 1
         * SELECT CATEGORY
         */
        if (selectMode) {

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

            return
        }

        /*
         * MODE 2
         * MANAGE CATEGORY
         */

        if (selected.isAddAction) {

            showAddOrEditSheet(
                existing = null
            )

            return
        }

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
                existing?.toCategory()
        ) { result ->

            lifecycleScope.launch {

                if (existing == null) {

                    val nextOrder =
                        (
                                categoryDao.minSortOrder(
                                    currentUserId
                                ) ?: 0
                                ) - 1

                    categoryDao.insert(
                        CategoryEntity(
                            userId =
                                currentUserId,

                            label =
                                result.label,

                            iconRes =
                                result.iconRes,

                            tintColor =
                                result.tintColor,

                            bgColor =
                                result.bgColor,

                            sortOrder =
                                nextOrder
                        )
                    )

                } else {

                    categoryDao.update(
                        existing.copy(

                            label =
                                result.label,

                            iconRes =
                                result.iconRes,

                            tintColor =
                                result.tintColor,

                            bgColor =
                                result.bgColor
                        )
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
    // ROOM ENTITY → UI CATEGORY
    // ============================================================

    private fun CategoryEntity.toCategory(): Category {

        return Category(

            label =
                label,

            iconRes =
                iconRes,

            tintColor =
                tintColor,

            bgColor =
                bgColor,

            isAddAction =
                false,

            id =
                id
        )
    }

    companion object {

        const val EXTRA_SELECT_MODE =
            "EXTRA_SELECT_MODE"

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