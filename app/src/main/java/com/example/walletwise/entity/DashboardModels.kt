package com.example.walletwise.entity

import androidx.annotation.ColorInt

data class DayBar(
    val label: String,
    val income: Float,
    val expense: Float
)

data class BreakdownItem(
    val label: String,
    val amount: String,
    val percent: Int,
    @ColorInt val color: Int
)

data class AccountItem(
    val name: String,
    val balance: String,
    val percent: Int,
    @ColorInt val color: Int,
    @ColorInt val iconBg: Int
)

/**
 * One point on the Balance Trend line.
 *
 * The list is ordered from oldest to newest.
 */
data class MonthPoint(
    val label: String,
    val balance: Float
)

data class CategoryTrendItem(
    val categoryLabel: String,
    val currentAmountLabel: String,
    val changeLabel: String,
    val isIncrease: Boolean,
    @ColorInt val color: Int
)

data class BiggestTransactionItem(
    val label: String,
    val dateLabel: String,
    val amountLabel: String,
    val isIncome: Boolean,
    @ColorInt val color: Int,
    @ColorInt val iconBg: Int
)

data class DashboardUiState(
    val periodLabel: String = "This Month",

    // Current total money across all accounts.
    val totalBalance: String = "",

    // Selected-period cash flow.
    val periodIncome: String = "",
    val periodExpense: String = "",
    val periodNet: String = "",

    // Income vs Expense chart.
    val periodBars: List<DayBar> = emptyList(),
    val periodMaxValue: Float = 1f,

    // Income breakdown.
    val incomeBreakdownTotal: String = "",
    val incomeBreakdownItems: List<BreakdownItem> = emptyList(),

    // Expense breakdown.
    val expenseBreakdownTotal: String = "",
    val expenseBreakdownItems: List<BreakdownItem> = emptyList(),

    // Balance trend.
    val monthlyTrend: List<MonthPoint> = emptyList(),
    val trendCurrentValue: String = "",
    val trendChangeLabel: String = "",
    val trendChangeIsNegative: Boolean = false,

    // Accounts.
    val accounts: List<AccountItem> = emptyList(),

    // Category comparison.
    val topCategoryTrends: List<CategoryTrendItem> = emptyList(),

    // Biggest transactions.
    val biggestTransactions: List<BiggestTransactionItem> = emptyList()
)

sealed class DashboardPeriod {

    data object ThisMonth : DashboardPeriod()

    data object ThisYear : DashboardPeriod()

    data class Custom(
        val start: Long,
        val end: Long
    ) : DashboardPeriod()
}