package com.example.walletwise.category

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SelectCategoryActivity : AppCompatActivity() {

    private lateinit var userDao: UserDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var adapter: CategoryAdapter

    /** Latest snapshot from Room, kept around so taps can be resolved back to a row. */
    private var currentEntities: List<CategoryEntity> = emptyList()

    /** Row id of the placeholder local user; resolved in onCreate before anything else runs. */
    private var currentUserId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_category)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val database = AppDatabase.getDatabase(this)
        userDao = database.userDao()
        categoryDao = database.categoryDao()

        val recyclerView = findViewById<RecyclerView>(R.id.categoryRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 4)
        adapter = CategoryAdapter(mutableListOf()) { selected ->
            if (selected.isAddAction) {
                showAddOrEditSheet(existing = null)
            } else {
                val entity = currentEntities.firstOrNull { it.id == selected.id }
                if (entity != null) showCategoryActionsSheet(entity)
            }
        }
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            // Categories have a FK to users. User.userId is autoGenerate, so there's no
            // fixed "id 0" to check against — look the placeholder user up by its fixed
            // email instead, and capture the real generated id from the insert.
            val existingUser = userDao.getUserByEmail(DEFAULT_USER_EMAIL)
            currentUserId = existingUser?.userId ?: userDao.insertUser(
                User(
                    fullName = "Local User",
                    email = DEFAULT_USER_EMAIL,
                    password = ""
                )
            ).toInt()

            if (categoryDao.count() == 0) {
                categoryDao.insertAll(
                    CategorySeedLoader.loadDefaultEntities(this@SelectCategoryActivity, currentUserId)
                )
            }
        }

        lifecycleScope.launch {
            categoryDao.observeAll().collect { entities ->
                currentEntities = entities
                val displayList = mutableListOf(
                    Category("Add", R.drawable.ic_plus, Color.TRANSPARENT, isAddAction = true)
                )
                displayList.addAll(entities.map { it.toCategory() })
                adapter.submitList(displayList)
            }
        }
    }

    /** Opens the add/edit dialog. Pass null to create a new category. */
    private fun showAddOrEditSheet(existing: CategoryEntity?) {
        AddCategoryBottomSheet(existing = existing?.toCategory()) { result ->
            lifecycleScope.launch {
                if (existing == null) {
                    val nextOrder = (categoryDao.minSortOrder() ?: 0) - 1
                    categoryDao.insert(
                        CategoryEntity(
                            userId = currentUserId,
                            label = result.label,
                            iconRes = result.iconRes,
                            tintColor = result.tintColor,
                            bgColor = result.bgColor,
                            sortOrder = nextOrder
                        )
                    )
                } else {
                    categoryDao.update(
                        existing.copy(
                            label = result.label,
                            iconRes = result.iconRes,
                            tintColor = result.tintColor,
                            bgColor = result.bgColor
                        )
                    )
                }
            }
        }.show(supportFragmentManager, "add_edit_category")
    }

    private fun showCategoryActionsSheet(entity: CategoryEntity) {
        CategoryActionsBottomSheet(
            categoryLabel = entity.label,
            onEdit = { showAddOrEditSheet(existing = entity) },
            onDelete = { confirmDelete(entity) }
        ).show(supportFragmentManager, "category_actions")
    }

    private fun confirmDelete(entity: CategoryEntity) {
        AlertDialog.Builder(this, R.style.DeleteCategoryDialogTheme)
            .setTitle("Delete category?")
            .setMessage("\"${entity.label}\" will be removed. This can't be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                lifecycleScope.launch { categoryDao.delete(entity) }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun CategoryEntity.toCategory(): Category =
        Category(
            label = label,
            iconRes = iconRes,
            tintColor = tintColor,
            bgColor = bgColor,
            isAddAction = false,
            id = id
        )

    companion object {
        private const val DEFAULT_USER_EMAIL = "local@device"
    }
}