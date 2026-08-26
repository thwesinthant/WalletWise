package com.example.walletwise.database

import com.example.walletwise.entity.Notification
import com.example.walletwise.entity.User

object SeedData {

    fun getDefaultNotifications(): List<Notification> {
        return listOf(
            Notification(
                notificationId = 1,
                userId = 1,
                title = "Budget alert!",
                message = "You've used 85% of your Dining budget for this month.",
                type = "BUDGET_ALERT",
                timeAgo = "23 min",
                isRead = false
            ),
            Notification(
                notificationId = 2,
                userId = 1,
                title = "Bill due tomorrow",
                message = "Your Netflix subscription of 15.99 dollars is due tomorrow.",
                type = "BILL_DUE",
                timeAgo = "2 hr",
                isRead = false
            ),
            Notification(
                notificationId = 3,
                userId = 1,
                title = "Your weekly summary is ready",
                message = "You spent 342 dollars this week, 12% less than last week.",
                type = "SUMMARY",
                timeAgo = "1 dy",
                isRead = true
            ),
            Notification(
                notificationId = 4,
                userId = 1,
                title = "Savings goal reminder",
                message = "Add funds to your Japan Trip goal to stay on track.",
                type = "GOAL_REMINDER",
                timeAgo = "1 wk",
                isRead = false
            ),
            Notification(
                notificationId = 5,
                userId = 1,
                title = "Large transaction detected",
                message = "A 120 dollar charge was made at Amazon today.",
                type = "TRANSACTION",
                timeAgo = "2 wk",
                isRead = true
            )
        )
    }
    // Notification လုံးဝမထည့်ဘဲ List အလွတ်ထားခြင်း
    //fun getDefaultNotifications(): List<Notification> {
        //return emptyList()
    //}
}