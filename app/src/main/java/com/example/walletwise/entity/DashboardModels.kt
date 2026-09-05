package com.example.walletwise.entity

import androidx.annotation.ColorInt

/**
 * Kept in this package (rather than com.example.walletwise.dashboard) so that
 * BarChartView / DonutChartView / LineAreaChartView, which were copied over
 * from the friend's project unchanged, keep working without editing their
 * imports.
 */

data class DayBar(val label: String, val income: Float, val expense: Float)

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

/** One point on the Balance Trend line — one per month, oldest to newest. */
data class MonthPoint(val label: String, val balance: Float)

/** One row in the Top Spending Categories card — this period's spend plus the swing vs the previous comparable period. */
data class CategoryTrendItem(
    val categoryLabel: String,
    val currentAmountLabel: String,
    val changeLabel: String,
    val isIncrease: Boolean,
    @ColorInt val color: Int
)

/** One row in the Biggest Transactions card. */
data class BiggestTransactionItem(
    val label: String,
    val dateLabel: String,
    val amountLabel: String,
    val isIncome: Boolean,
    @ColorInt val color: Int,
    @ColorInt val iconBg: Int
)

/** Everything the analytics dashboard needs to bind a full refresh. */
data class DashboardUiState(
    val periodLabel: String = "This Month",
    val totalBalance: String = "",
    val periodIncome: String = "",
    val periodExpense: String = "",
    val periodNet: String = "",
    val weekBars: List<DayBar> = emptyList(),
    val weekMaxValue: Float = 1f,
    val incomeBreakdownTotal: String = "",
    val incomeBreakdownItems: List<BreakdownItem> = emptyList(),
    val expenseBreakdownTotal: String = "",
    val expenseBreakdownItems: List<BreakdownItem> = emptyList(),
    val monthlyTrend: List<MonthPoint> = emptyList(),
    val trendCurrentValue: String = "",
    val trendChangeLabel: String = "",
    val trendChangeIsNegative: Boolean = false,
    val cashFlowIncome: String = "",
    val cashFlowIncomePercent: Int = 0,
    val cashFlowExpense: String = "",
    val cashFlowExpensePercent: Int = 0,
    val accounts: List<AccountItem> = emptyList(),
    val topCategoryTrends: List<CategoryTrendItem> = emptyList(),
    val biggestTransactions: List<BiggestTransactionItem> = emptyList()
)

/**
 * Which range the period tabs (This Month / This Year / Custom) filter to.
 * Custom carries its own picked range (epoch millis, start inclusive / end exclusive).
 */
sealed class DashboardPeriod {
    data object ThisMonth : DashboardPeriod()
    data object ThisYear : DashboardPeriod()
    data class Custom(val start: Long, val end: Long) : DashboardPeriod()
}