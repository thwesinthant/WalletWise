package com.example.walletwise.dashboard

import android.graphics.Color
import com.example.walletwise.entity.AccountBalance
import com.example.walletwise.entity.AccountItem
import com.example.walletwise.entity.BreakdownItem
import com.example.walletwise.entity.Budget
import com.example.walletwise.entity.BudgetCategory
import com.example.walletwise.entity.BudgetProgressItem
import com.example.walletwise.entity.CategoryEntity
import com.example.walletwise.entity.DashboardPeriod
import com.example.walletwise.entity.DashboardUiState
import com.example.walletwise.entity.DayBar
import com.example.walletwise.entity.MonthPoint
import com.example.walletwise.entity.Transaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.collections.flatMap
import kotlin.math.abs
import kotlin.math.roundToInt

// =====================================================================
// TRANSACTION TYPE CONSTANTS
//
// Same literal strings TransactionDao / AccountDao already filter on
// ('INCOME', 'EXPENSE', 'TRANSFER_IN', 'TRANSFER_OUT') — kept here as
// named constants just so this file doesn't repeat raw string literals.
// =====================================================================

private const val TYPE_INCOME = "INCOME"
private const val TYPE_EXPENSE = "EXPENSE"
private const val TYPE_TRANSFER_IN = "TRANSFER_IN"
private const val TYPE_TRANSFER_OUT = "TRANSFER_OUT"

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/** Cycled through for accounts, which (unlike WalletWise's Category) have no color column of their own. */
private val ACCOUNT_COLOR_PALETTE = listOf(
    "#8B7CF6", "#2F6FED", "#2ED47A", "#FFA53E", "#FF5A8A", "#1FA463"
).map { Color.parseColor(it) }

private val monthLabelFormat = SimpleDateFormat("MMM", Locale.getDefault())
private val dayLabelFormat = SimpleDateFormat("EEE", Locale.getDefault())
private val budgetDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())


// =====================================================================
// BUILD UI STATE
//
// Everything the analytics dashboard needs, computed from whatever the
// existing DAOs already return — no separate database, no new tables.
// =====================================================================

fun buildDashboardUiState(
    accounts: List<AccountBalance>,
    categories: List<CategoryEntity>,
    transactions: List<Transaction>,
    budgets: List<Budget>,
    budgetCategories: List<BudgetCategory>,
    period: DashboardPeriod,
    currency: String
): DashboardUiState {

    val now = System.currentTimeMillis()

    val totalBalance = accounts.sumOf { it.currentBalance }
    val totalAbsBalance = accounts.sumOf { abs(it.currentBalance) }

    val (periodStart, periodEnd) = rangeFor(period)
    val periodTx = transactions.filter { it.createdAt in periodStart until periodEnd }
    val periodIncome = periodTx.filter { it.type == TYPE_INCOME }.sumOf { it.amount }
    val periodExpense = periodTx.filter { it.type == TYPE_EXPENSE }.sumOf { it.amount }

    val (last30Start, last30End) = last30DaysRange(now)
    val last30Tx = transactions.filter { it.createdAt in last30Start until last30End }
    val last30Income = last30Tx.filter { it.type == TYPE_INCOME }.sumOf { it.amount }
    val last30Expense = last30Tx.filter { it.type == TYPE_EXPENSE }.sumOf { it.amount }

    val bars = weekBars(transactions, now)
    val trend = monthlyTrend(transactions, accounts, now)

    val activeBudget = budgets.firstOrNull { now in it.startDate until it.endDate }
    val budgetSection = buildBudgetProgress(activeBudget, budgetCategories, transactions, categories, currency)

    return DashboardUiState(
        periodLabel = labelFor(period),
        totalBalance = formatCurrency(totalBalance, currency),
        periodIncome = formatCurrency(periodIncome, currency),
        periodExpense = formatCurrency(periodExpense, currency),
        periodNet = formatCurrency(periodIncome - periodExpense, currency),
        weekBars = bars,
        weekMaxValue = bars.flatMap { listOf(it.income, it.expense) }.maxOrNull()?.let { niceMax(it) } ?: 1000f,
        incomeBreakdownTotal = formatCurrency(periodIncome, currency),
        incomeBreakdownItems = breakdown(periodTx, categories, TYPE_INCOME, currency),
        expenseBreakdownTotal = formatCurrency(periodExpense, currency),
        expenseBreakdownItems = breakdown(periodTx, categories, TYPE_EXPENSE, currency),
        monthlyTrend = trend.points,
        trendCurrentValue = formatCurrency(trend.points.lastOrNull()?.balance?.toDouble() ?: totalBalance, currency),
        trendChangeLabel = formatSignedPercent(trend.changePercent),
        trendChangeIsNegative = trend.changePercent < 0,
        cashFlowIncome = formatCurrency(last30Income, currency),
        cashFlowIncomePercent = percentOf(last30Income, last30Income + last30Expense),
        cashFlowExpense = formatCurrency(last30Expense, currency),
        cashFlowExpensePercent = percentOf(last30Expense, last30Income + last30Expense),
        accounts = accounts.mapIndexed { index, row ->
            val color = ACCOUNT_COLOR_PALETTE[index % ACCOUNT_COLOR_PALETTE.size]
            AccountItem(
                name = row.name,
                balance = formatCurrency(row.currentBalance, currency),
                percent = percentOf(abs(row.currentBalance), totalAbsBalance),
                color = color,
                iconBg = withAlpha(color, 38)
            )
        },
        hasActiveBudget = budgetSection != null,
        budgetName = budgetSection?.name ?: "",
        budgetPeriodLabel = budgetSection?.periodLabel ?: "",
        budgetSpentLabel = budgetSection?.spentLabel ?: "",
        budgetLimitLabel = budgetSection?.limitLabel ?: "",
        budgetOverallPercent = budgetSection?.overallPercent ?: 0,
        budgetIsOverBudget = budgetSection?.isOverBudget ?: false,
        budgetItems = budgetSection?.items ?: emptyList()
    )
}


// =====================================================================
// WEEKLY BAR CHART (last 7 days)
// =====================================================================

private fun weekBars(transactions: List<Transaction>, now: Long): List<DayBar> {
    val cal = Calendar.getInstance()
    return (6 downTo 0).map { offset ->
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, -offset)
        val dayStart = startOfDay(cal.timeInMillis)
        val dayEnd = dayStart + DAY_MILLIS
        val label = dayLabelFormat.format(cal.time)
        val dayTx = transactions.filter { it.createdAt in dayStart until dayEnd }
        val income = dayTx.filter { it.type == TYPE_INCOME }.sumOf { it.amount }.toFloat()
        val expense = dayTx.filter { it.type == TYPE_EXPENSE }.sumOf { it.amount }.toFloat()
        DayBar(label, income, expense)
    }
}


// =====================================================================
// 12-MONTH BALANCE TREND
// =====================================================================

private data class TrendResult(val points: List<MonthPoint>, val changePercent: Double)

private fun monthlyTrend(
    transactions: List<Transaction>,
    accounts: List<AccountBalance>,
    now: Long
): TrendResult {

    val openingTotal = accounts.sumOf { it.openingBalance }
    val sorted = transactions.sortedBy { it.createdAt }

    fun netUpTo(timestamp: Long): Double {
        var net = 0.0
        for (tx in sorted) {
            if (tx.createdAt > timestamp) break
            net += when (tx.type) {
                TYPE_INCOME, TYPE_TRANSFER_IN -> tx.amount
                TYPE_EXPENSE, TYPE_TRANSFER_OUT -> -tx.amount
                else -> 0.0
            }
        }
        return net
    }

    val cal = Calendar.getInstance()
    val points = mutableListOf<MonthPoint>()
    for (monthsAgo in 11 downTo 1) {
        cal.timeInMillis = now
        cal.add(Calendar.MONTH, -monthsAgo)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val monthEndBalance = openingTotal + netUpTo(cal.timeInMillis)
        points += MonthPoint(monthLabelFormat.format(cal.time), monthEndBalance.toFloat())
    }
    // Final point is "right now", not just this month's end.
    val currentBalance = openingTotal + netUpTo(now)
    points += MonthPoint("Today", currentBalance.toFloat())

    val first = points.firstOrNull()?.balance?.toDouble() ?: 0.0
    val last = points.lastOrNull()?.balance?.toDouble() ?: 0.0
    val change = if (first != 0.0) ((last - first) / abs(first)) * 100 else 0.0
    return TrendResult(points, change)
}


// =====================================================================
// CATEGORY BREAKDOWN (donut + legend)
//
// WalletWise's CategoryEntity has no INCOME/EXPENSE "type" of its own
// (unlike the friend's schema) — a category's direction is whatever the
// transaction using it was recorded as, so we group straight off
// Transaction.type + categoryId.
// =====================================================================

private fun breakdown(
    periodTx: List<Transaction>,
    categories: List<CategoryEntity>,
    txType: String,
    currency: String
): List<BreakdownItem> {

    val categoryById = categories.associateBy { it.id }
    val totals = periodTx
        .filter { it.type == txType && it.categoryId != null }
        .groupBy { it.categoryId }
        .mapNotNull { (categoryId, txs) ->
            val category = categoryById[categoryId] ?: return@mapNotNull null
            category to txs.sumOf { it.amount }
        }
        .sortedByDescending { it.second }

    val grandTotal = totals.sumOf { it.second }
    return totals.map { (category, amount) ->
        BreakdownItem(
            label = category.label,
            amount = formatCurrency(amount, currency),
            percent = percentOf(amount, grandTotal),
            color = category.tintColor
        )
    }
}


// =====================================================================
// BUDGET VS ACTUAL (Budget Progress card)
//
// Uses whichever Budget row has startDate <= now < endDate, same rule
// as BudgetDao.getActiveBudget. Spending is measured against the
// budget's OWN date range, not the period tabs — a budget always
// tracks its own window regardless of which analytics tab is selected.
// =====================================================================

private data class BudgetSection(
    val name: String,
    val periodLabel: String,
    val spentLabel: String,
    val limitLabel: String,
    val overallPercent: Int,
    val isOverBudget: Boolean,
    val items: List<BudgetProgressItem>
)

private fun buildBudgetProgress(
    activeBudget: Budget?,
    budgetCategories: List<BudgetCategory>,
    transactions: List<Transaction>,
    categories: List<CategoryEntity>,
    currency: String
): BudgetSection? {

    if (activeBudget == null) return null

    val categoryById = categories.associateBy { it.id }

    val spentByCategory = transactions
        .filter {
            it.type == TYPE_EXPENSE &&
                    it.categoryId != null &&
                    it.createdAt in activeBudget.startDate until activeBudget.endDate
        }
        .groupBy { it.categoryId }
        .mapValues { (_, txs) -> txs.sumOf { it.amount } }

    val categoryLimits = budgetCategories.filter { it.budgetId == activeBudget.budgetId }

    val items = categoryLimits
        .mapNotNull { limitRow ->
            val category = categoryById[limitRow.categoryId] ?: return@mapNotNull null
            val spent = spentByCategory[limitRow.categoryId] ?: 0.0
            val percent = if (limitRow.limitAmount > 0)
                ((spent / limitRow.limitAmount) * 100).roundToInt()
            else 0

            BudgetProgressItem(
                categoryLabel = category.label,
                spentLabel = formatCurrency(spent, currency),
                limitLabel = formatCurrency(limitRow.limitAmount, currency),
                percent = percent,
                isOverBudget = spent > limitRow.limitAmount,
                color = category.tintColor
            )
        }
        .sortedByDescending { it.percent }

    val totalLimit = categoryLimits.sumOf { it.limitAmount }
    val totalSpent = categoryLimits.sumOf { spentByCategory[it.categoryId] ?: 0.0 }
    val overallPercent = if (totalLimit > 0) ((totalSpent / totalLimit) * 100).roundToInt() else 0

    return BudgetSection(
        name = activeBudget.name,
        periodLabel = "${budgetDateFormat.format(Date(activeBudget.startDate))} - " +
                budgetDateFormat.format(Date(activeBudget.endDate - 1)),
        spentLabel = formatCurrency(totalSpent, currency),
        limitLabel = formatCurrency(totalLimit, currency),
        overallPercent = overallPercent,
        isOverBudget = totalSpent > totalLimit,
        items = items
    )
}


// =====================================================================
// DATE RANGE HELPERS
// =====================================================================

private fun rangeFor(period: DashboardPeriod): Pair<Long, Long> {
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()
    cal.timeInMillis = now
    return when (period) {
        DashboardPeriod.ThisMonth -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            startOfDay(cal.timeInMillis) to now + 1
        }
        DashboardPeriod.ThisYear -> {
            cal.set(Calendar.DAY_OF_YEAR, 1)
            startOfDay(cal.timeInMillis) to now + 1
        }
        is DashboardPeriod.Custom -> period.start to period.end
    }
}

/** Fixed last-30-days window for the Cash Flow card — independent of the tab selection. */
private fun last30DaysRange(now: Long): Pair<Long, Long> =
    (now - 30L * DAY_MILLIS) to now + 1

private fun labelFor(period: DashboardPeriod): String = when (period) {
    DashboardPeriod.ThisMonth -> "This Month"
    DashboardPeriod.ThisYear -> "This Year"
    is DashboardPeriod.Custom -> {
        val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
        "${fmt.format(Date(period.start))} - ${fmt.format(Date(period.end - 1))}"
    }
}

private fun startOfDay(timestamp: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun niceMax(value: Float): Float {
    if (value <= 0f) return 1000f
    var step = 1000f
    while (step < value) step *= 1.5f
    return step
}

private fun percentOf(part: Double, whole: Double): Int {
    if (whole <= 0.0) return 0
    return ((part / whole) * 100).roundToInt().coerceIn(0, 100)
}

private fun formatSignedPercent(percent: Double): String {
    val rounded = percent.roundToInt()
    val sign = if (rounded > 0) "+" else ""
    return "$sign$rounded%"
}

private fun withAlpha(color: Int, alpha: Int): Int =
    Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

/** Matches DashboardActivity.formatCurrency's own style, so both screens read the same way. */
private fun formatCurrency(amount: Double, currency: String): String =
    "$currency ${String.format(Locale.getDefault(), "%,.2f", amount)}"
