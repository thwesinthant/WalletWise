package com.example.walletwise.util

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.TextView
import com.example.walletwise.R
import com.example.walletwise.budget.AddBudgetActivity
import com.example.walletwise.budget.BudgetActivity
import com.example.walletwise.category.SelectCategoryActivity
import com.example.walletwise.dashboard.DashboardActivity
import com.example.walletwise.transactions.AddTransactionActivity
import com.example.walletwise.goal.GoalActivity
import com.example.walletwise.profile.ProfileActivity
import com.example.walletwise.transactions.TransactionActivity

enum class NavTab {
    HOME,
    TRANSACTION,
    BUDGETS,
    GOALS,
    SETTINGS
}

object BottomNavHelper {

    fun setup(
        activity: Activity,
        root: View,
        current: NavTab,
        userId: Int,
        onBeforeNavigate: (NavTab) -> Unit = {}
    ) {

        val tabs = mapOf(
            NavTab.HOME to Triple(
                R.id.navHome,
                R.id.iconHome,
                R.id.labelHome
            ),

            NavTab.TRANSACTION to Triple(
                R.id.navTransaction,
                R.id.iconTransaction,
                R.id.labelTransaction
            ),

            NavTab.BUDGETS to Triple(
                R.id.navBudgets,
                R.id.iconBudgets,
                R.id.labelBudgets
            ),

            NavTab.GOALS to Triple(
                R.id.navGoals,
                R.id.iconGoals,
                R.id.labelGoals
            ),

            NavTab.SETTINGS to Triple(
                R.id.navSettings,
                R.id.iconSettings,
                R.id.labelSettings
            )
        )

        tabs.forEach { (tab, ids) ->

            val (containerId, iconId, labelId) = ids

            val container =
                root.findViewById<View>(containerId)

            val icon =
                root.findViewById<TextView>(iconId)

            val label =
                root.findViewById<TextView>(labelId)

            // Highlight current tab
            if (tab == current) {

                icon.setBackgroundResource(
                    R.drawable.bg_nav_active
                )

                label.setTextColor(
                    activity.getColor(
                        R.color.primary_500
                    )
                )

            } else {

                icon.background = null

                label.setTextColor(
                    activity.getColor(
                        R.color.neutral_500
                    )
                )
            }

            container.setOnClickListener {

                if (tab == current) {
                    return@setOnClickListener
                }

                // Make sure we have a valid user
                if (userId == -1) {
                    return@setOnClickListener
                }

                onBeforeNavigate(tab)

                val targetClass = when (tab) {

                    NavTab.HOME ->
                        DashboardActivity::class.java

                    NavTab.TRANSACTION ->
                        TransactionActivity::class.java

                    NavTab.BUDGETS ->
                        SelectCategoryActivity::class.java

                    NavTab.GOALS ->
                        BudgetActivity::class.java

                    NavTab.SETTINGS ->
                        ProfileActivity::class.java
                }

                val intent =
                    Intent(
                        activity,
                        targetClass
                    )


                // =====================================================
                // CATEGORY MANAGEMENT
                // =====================================================

                if (tab == NavTab.BUDGETS) {

                    /*
                     * Open SelectCategoryActivity in MANAGE mode.
                     */
                    intent.putExtra(
                        SelectCategoryActivity.EXTRA_SELECT_MODE,
                        false
                    )

                    /*
                     * SelectCategoryActivity uses EXTRA_USER_ID,
                     * not "USER_ID".
                     */
                    intent.putExtra(
                        SelectCategoryActivity.EXTRA_USER_ID,
                        userId
                    )

                } else {

                    /*
                     * Other activities use "USER_ID".
                     */
                    intent.putExtra(
                        "USER_ID",
                        userId
                    )
                }


                if (tab == NavTab.HOME) {

                    intent.flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                }

                activity.startActivity(intent)
            }
        }
    }
}