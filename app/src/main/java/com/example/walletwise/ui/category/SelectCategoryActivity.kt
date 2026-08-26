package com.example.walletwise.ui.category

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.walletwise.R
import com.google.android.material.appbar.MaterialToolbar

class SelectCategoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_category)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val categories = listOf(
            Category("Add", R.drawable.ic_plus, Color.TRANSPARENT, isAddAction = true),
            Category("Groceries", R.drawable.ic_category_groceries, getColor(R.color.category_groceries), getColor(R.color.category_groceries_bg)),
            Category("Travel", R.drawable.ic_category_travel, getColor(R.color.category_travel), getColor(R.color.category_travel_bg)),
            Category("Car", R.drawable.ic_category_car, getColor(R.color.category_car), getColor(R.color.category_car_bg)),
            Category("Home", R.drawable.ic_category_home, getColor(R.color.category_home), getColor(R.color.category_home_bg)),
            Category("Insurances", R.drawable.ic_category_insurances, getColor(R.color.category_insurances), getColor(R.color.category_insurances_bg)),
            Category("Education", R.drawable.ic_category_education, getColor(R.color.category_education), getColor(R.color.category_education_bg)),
            Category("Marketing", R.drawable.ic_category_marketing, getColor(R.color.category_marketing), getColor(R.color.category_marketing_bg)),
            Category("shopping", R.drawable.ic_category_shopping, getColor(R.color.category_shopping), getColor(R.color.category_shopping_bg)),
            Category("Internet", R.drawable.ic_category_internet, getColor(R.color.category_internet), getColor(R.color.category_internet_bg)),
            Category("Water", R.drawable.ic_category_water, getColor(R.color.category_water), getColor(R.color.category_water_bg)),
            Category("Rent", R.drawable.ic_category_rent, getColor(R.color.category_rent), getColor(R.color.category_rent_bg)),
            Category("Gym", R.drawable.ic_category_gym, getColor(R.color.category_gym), getColor(R.color.category_gym_bg)),
            Category("Subscription", R.drawable.ic_category_subscription, getColor(R.color.category_subscription), getColor(R.color.category_subscription_bg)),
            Category("Vacation", R.drawable.ic_category_vacation, getColor(R.color.category_vacation), getColor(R.color.category_vacation_bg)),
            Category("Other", R.drawable.ic_category_other, getColor(R.color.category_other), getColor(R.color.category_other_bg))
        )
        val recyclerView = findViewById<RecyclerView>(R.id.categoryRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 4)
        val adapter = CategoryAdapter(categories.toMutableList()) { selected ->
            if (selected.isAddAction) {
                AddCategoryBottomSheet { newCategory ->
                    (recyclerView.adapter as? CategoryAdapter)?.addCategory(newCategory)
                }.show(supportFragmentManager, "add_category")
            } else {
                // TODO: return selected.label to whoever opened this screen
            }
        }
        recyclerView.adapter = adapter
    }
}