package com.example.walletwise.util

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.TextView
import com.example.walletwise.R
import com.example.walletwise.budget.BudgetActivity
import com.example.walletwise.dashboard.DashboardActivity
import com.example.walletwise.expense.AddTransactionActivity
import com.example.walletwise.goal.GoalActivity
import com.example.walletwise.profile.ProfileActivity

enum class NavTab { HOME, TRANSACTION, BUDGETS, GOALS, SETTINGS }

object BottomNavHelper {

    fun setup(
        activity: Activity,
        root: View,
        current: NavTab,
        userId: Int,
        onBeforeNavigate: (NavTab) -> Unit = {}   // <- your "backend action" hook
    ) {
        val tabs = mapOf(
            NavTab.HOME to Triple(R.id.navHome, R.id.iconHome, R.id.labelHome),
            NavTab.TRANSACTION to Triple(R.id.navTransaction, R.id.iconTransaction, R.id.labelTransaction),
            NavTab.BUDGETS to Triple(R.id.navBudgets, R.id.iconBudgets, R.id.labelBudgets),
            NavTab.GOALS to Triple(R.id.navGoals, R.id.iconGoals, R.id.labelGoals),
            NavTab.SETTINGS to Triple(R.id.navSettings, R.id.iconSettings, R.id.labelSettings)
        )

        tabs.forEach { (tab, ids) ->
            val (containerId, iconId, labelId) = ids
            val container = root.findViewById<View>(containerId)
            val icon = root.findViewById<TextView>(iconId)
            val label = root.findViewById<TextView>(labelId)

            // Highlight active tab
            if (tab == current) {
                icon.setBackgroundResource(R.drawable.bg_nav_active)
                label.setTextColor(activity.getColor(R.color.primary_500))
            } else {
                icon.background = null
                label.setTextColor(activity.getColor(R.color.neutral_500))
            }

            container.setOnClickListener {
                if (tab == current) return@setOnClickListener  // already on this screen

                onBeforeNavigate(tab)  // e.g. log analytics, mark something read, save draft, etc.

                val targetClass = when (tab) {
                    NavTab.HOME -> DashboardActivity::class.java
                    NavTab.SETTINGS -> ProfileActivity::class.java
                    NavTab.TRANSACTION -> AddTransactionActivity::class.java
                    NavTab.BUDGETS -> BudgetActivity::class.java
                    NavTab.GOALS -> GoalActivity::class.java
                }

                targetClass.let {
                    val intent = Intent(activity, it)
                    intent.putExtra("USER_ID", userId)
                    if (tab == NavTab.HOME) {
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    activity.startActivity(intent)
                }
            }
        }
    }
}