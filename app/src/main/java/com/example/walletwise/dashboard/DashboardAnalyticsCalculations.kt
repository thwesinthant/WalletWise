package com.example.walletwise.dashboard

import android.graphics.Color
import com.example.walletwise.entity.AccountBalance
import com.example.walletwise.entity.AccountItem
import com.example.walletwise.entity.BiggestTransactionItem
import com.example.walletwise.entity.BreakdownItem
import com.example.walletwise.entity.CategoryEntity
import com.example.walletwise.entity.CategoryTrendItem
import com.example.walletwise.entity.DashboardPeriod
import com.example.walletwise.entity.DashboardUiState
import com.example.walletwise.entity.DayBar
import com.example.walletwise.entity.MonthPoint
import com.example.walletwise.entity.Transaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TYPE_INCOME = "INCOME"
private const val TYPE_EXPENSE = "EXPENSE"
private const val TYPE_TRANSFER_IN = "TRANSFER_IN"
private const val TYPE_TRANSFER_OUT = "TRANSFER_OUT"

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

private val ACCOUNT_COLOR_PALETTE = listOf(
    "#8B7CF6",
    "#2F6FED",
    "#2ED47A",
    "#FFA53E",
    "#FF5A8A",
    "#1FA463"
).map { Color.parseColor(it) }

private val monthLabelFormat =
    SimpleDateFormat("MMM", Locale.getDefault())

private val shortDateFormat =
    SimpleDateFormat("MMM d", Locale.getDefault())


fun buildDashboardUiState(
    accounts: List<AccountBalance>,
    categories: List<CategoryEntity>,
    transactions: List<Transaction>,
    period: DashboardPeriod,
    currency: String
): DashboardUiState {

    val now = System.currentTimeMillis()

    // =========================================================
    // CURRENT ACCOUNT BALANCE
    // =========================================================

    val totalBalance =
        accounts.sumOf { it.currentBalance }

    val totalPositiveBalance =
        accounts.sumOf {
            it.currentBalance.coerceAtLeast(0.0)
        }


    // =========================================================
    // SELECTED PERIOD
    // =========================================================

    val (periodStart, periodEnd) =
        rangeFor(period)

    val periodTx =
        transactions.filter {
            it.createdAt >= periodStart &&
                    it.createdAt < periodEnd
        }


    // =========================================================
    // PERIOD INCOME / EXPENSE
    //
    // IMPORTANT:
    // Transfers are intentionally excluded.
    // =========================================================

    val periodIncome =
        periodTx
            .filter { it.type == TYPE_INCOME }
            .sumOf { it.amount }

    val periodExpense =
        periodTx
            .filter { it.type == TYPE_EXPENSE }
            .sumOf { it.amount }

    val periodNet =
        periodIncome - periodExpense


    // =========================================================
    // PREVIOUS COMPARABLE PERIOD
    // =========================================================

    val (previousStart, previousEnd) =
        previousComparablePeriod(
            period = period,
            periodStart = periodStart,
            periodEnd = periodEnd
        )

    val previousTx =
        transactions.filter {
            it.createdAt >= previousStart &&
                    it.createdAt < previousEnd
        }


    // =========================================================
    // PERIOD CHART
    // =========================================================

    val periodBars =
        periodBars(
            periodTx = periodTx,
            period = period,
            periodStart = periodStart,
            periodEnd = periodEnd
        )

    val periodMaxValue =
        periodBars
            .flatMap {
                listOf(
                    it.income,
                    it.expense
                )
            }
            .maxOrNull()
            ?.let { niceMax(it) }
            ?: 1000f


    // =========================================================
    // BALANCE TREND
    // =========================================================

    val trend =
        monthlyTrend(
            transactions = transactions,
            accounts = accounts,
            now = now
        )


    // =========================================================
    // CATEGORY ANALYTICS
    // =========================================================

    val categoryTrends =
        buildCategoryTrends(
            currentTx = periodTx,
            previousTx = previousTx,
            categories = categories,
            currency = currency
        )


    // =========================================================
    // BIGGEST TRANSACTIONS
    // =========================================================

    val biggestTransactions =
        buildBiggestTransactions(
            periodTx = periodTx,
            categories = categories,
            currency = currency
        )


    // =========================================================
    // FINAL UI STATE
    // =========================================================

    return DashboardUiState(

        periodLabel = labelFor(period),

        totalBalance =
            formatCurrency(
                totalBalance,
                currency
            ),

        periodIncome =
            formatCurrency(
                periodIncome,
                currency
            ),

        periodExpense =
            formatCurrency(
                periodExpense,
                currency
            ),

        periodNet =
            formatCurrency(
                periodNet,
                currency
            ),

        periodBars = periodBars,

        periodMaxValue = periodMaxValue,

        incomeBreakdownTotal =
            formatCurrency(
                periodIncome,
                currency
            ),

        incomeBreakdownItems =
            breakdown(
                periodTx = periodTx,
                categories = categories,
                txType = TYPE_INCOME,
                currency = currency
            ),

        expenseBreakdownTotal =
            formatCurrency(
                periodExpense,
                currency
            ),

        expenseBreakdownItems =
            breakdown(
                periodTx = periodTx,
                categories = categories,
                txType = TYPE_EXPENSE,
                currency = currency
            ),

        monthlyTrend =
            trend.points,

        trendCurrentValue =
            formatCurrency(
                trend.points.lastOrNull()?.balance?.toDouble()
                    ?: totalBalance,
                currency
            ),

        trendChangeLabel =
            formatSignedPercent(
                trend.changePercent
            ),

        trendChangeIsNegative =
            trend.changePercent < 0,

        accounts =
            accounts.mapIndexed { index, row ->

                val color =
                    ACCOUNT_COLOR_PALETTE[
                        index % ACCOUNT_COLOR_PALETTE.size
                    ]

                AccountItem(
                    name = row.name,

                    balance =
                        formatCurrency(
                            row.currentBalance,
                            currency
                        ),

                    percent =
                        percentOf(
                            row.currentBalance.coerceAtLeast(0.0),
                            totalPositiveBalance
                        ),

                    color = color,

                    iconBg =
                        withAlpha(
                            color,
                            38
                        )
                )
            },

        topCategoryTrends =
            categoryTrends,

        biggestTransactions =
            biggestTransactions
    )
}


/* =============================================================
   INCOME VS EXPENSE PERIOD BARS
   ============================================================= */

private fun periodBars(
    periodTx: List<Transaction>,
    period: DashboardPeriod,
    periodStart: Long,
    periodEnd: Long
): List<DayBar> {

    return when (period) {

        // -----------------------------------------------------
        // THIS MONTH
        // -----------------------------------------------------
        DashboardPeriod.ThisMonth -> {

            dailyBars(
                transactions = periodTx,
                start = periodStart,
                end = periodEnd
            )
        }


        // -----------------------------------------------------
        // THIS YEAR
        // -----------------------------------------------------
        DashboardPeriod.ThisYear -> {

            monthlyBars(
                transactions = periodTx,
                start = periodStart,
                end = periodEnd
            )
        }


        // -----------------------------------------------------
        // CUSTOM
        // -----------------------------------------------------
        is DashboardPeriod.Custom -> {

            val durationDays =
                (periodEnd - periodStart)
                    .toDouble() / DAY_MILLIS

            if (durationDays <= 31.0) {

                dailyBars(
                    transactions = periodTx,
                    start = periodStart,
                    end = periodEnd
                )

            } else {

                monthlyBars(
                    transactions = periodTx,
                    start = periodStart,
                    end = periodEnd
                )
            }
        }
    }
}


/* =============================================================
   DAILY BAR DATA
   ============================================================= */

private fun dailyBars(
    transactions: List<Transaction>,
    start: Long,
    end: Long
): List<DayBar> {

    val result =
        mutableListOf<DayBar>()

    var currentStart =
        startOfDay(start)

    while (currentStart < end) {

        val nextDay =
            Calendar.getInstance().apply {
                timeInMillis = currentStart
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis

        val currentEnd =
            minOf(
                nextDay,
                end
            )

        val dayTransactions =
            transactions.filter {
                it.createdAt >= currentStart &&
                        it.createdAt < currentEnd
            }

        val income =
            dayTransactions
                .filter {
                    it.type == TYPE_INCOME
                }
                .sumOf {
                    it.amount
                }
                .toFloat()

        val expense =
            dayTransactions
                .filter {
                    it.type == TYPE_EXPENSE
                }
                .sumOf {
                    it.amount
                }
                .toFloat()

        val label =
            SimpleDateFormat(
                "d",
                Locale.getDefault()
            ).format(
                Date(currentStart)
            )

        result += DayBar(
            label = label,
            income = income,
            expense = expense
        )

        currentStart =
            currentEnd
    }

    return result
}


/* =============================================================
   MONTHLY BAR DATA
   ============================================================= */

private fun monthlyBars(
    transactions: List<Transaction>,
    start: Long,
    end: Long
): List<DayBar> {

    val result =
        mutableListOf<DayBar>()

    var monthStart =
        startOfMonth(start)

    while (monthStart < end) {

        val nextMonth =
            Calendar.getInstance().apply {
                timeInMillis = monthStart
                add(Calendar.MONTH, 1)
            }.timeInMillis

        val monthEnd =
            minOf(
                nextMonth,
                end
            )

        val monthTransactions =
            transactions.filter {
                it.createdAt >= monthStart &&
                        it.createdAt < monthEnd
            }

        val income =
            monthTransactions
                .filter {
                    it.type == TYPE_INCOME
                }
                .sumOf {
                    it.amount
                }
                .toFloat()

        val expense =
            monthTransactions
                .filter {
                    it.type == TYPE_EXPENSE
                }
                .sumOf {
                    it.amount
                }
                .toFloat()

        result += DayBar(
            label =
                monthLabelFormat.format(
                    Date(monthStart)
                ),

            income = income,
            expense = expense
        )

        monthStart =
            monthEnd
    }

    return result
}


/* =============================================================
   BALANCE TREND
   ============================================================= */

private data class TrendResult(
    val points: List<MonthPoint>,
    val changePercent: Double
)

private fun monthlyTrend(
    transactions: List<Transaction>,
    accounts: List<AccountBalance>,
    now: Long
): TrendResult {

    /*
     * This follows your current Account model:
     *
     * current balance =
     * opening balance
     * + income
     * + transfer in
     * - expense
     * - transfer out
     *
     * NOTE:
     * Because the current account-edit flow can modify
     * openingBalance, historical points are only as accurate
     * as the current openingBalance value.
     *
     * We are keeping this compatible with your existing
     * database design instead of introducing a new table.
     */

    val openingTotal =
        accounts.sumOf {
            it.openingBalance
        }

    val sortedTransactions =
        transactions.sortedBy {
            it.createdAt
        }


    fun netUpTo(
        timestamp: Long
    ): Double {

        var net = 0.0

        for (tx in sortedTransactions) {

            if (tx.createdAt > timestamp) {
                break
            }

            net += when (tx.type) {

                TYPE_INCOME,
                TYPE_TRANSFER_IN ->
                    tx.amount

                TYPE_EXPENSE,
                TYPE_TRANSFER_OUT ->
                    -tx.amount

                else ->
                    0.0
            }
        }

        return net
    }


    val calendar =
        Calendar.getInstance()

    val points =
        mutableListOf<MonthPoint>()


    // Previous 11 months.
    for (monthsAgo in 11 downTo 1) {

        calendar.timeInMillis =
            now

        calendar.add(
            Calendar.MONTH,
            -monthsAgo
        )

        calendar.set(
            Calendar.DAY_OF_MONTH,
            calendar.getActualMaximum(
                Calendar.DAY_OF_MONTH
            )
        )

        calendar.set(
            Calendar.HOUR_OF_DAY,
            23
        )

        calendar.set(
            Calendar.MINUTE,
            59
        )

        calendar.set(
            Calendar.SECOND,
            59
        )

        calendar.set(
            Calendar.MILLISECOND,
            999
        )

        val timestamp =
            calendar.timeInMillis

        points += MonthPoint(

            label =
                monthLabelFormat.format(
                    calendar.time
                ),

            balance =
                (
                        openingTotal +
                                netUpTo(timestamp)
                        ).toFloat()
        )
    }


    // Current point.
    val currentBalance =
        openingTotal +
                netUpTo(now)

    points += MonthPoint(
        label = "Today",
        balance = currentBalance.toFloat()
    )


    val first =
        points
            .firstOrNull()
            ?.balance
            ?.toDouble()
            ?: 0.0

    val last =
        points
            .lastOrNull()
            ?.balance
            ?.toDouble()
            ?: 0.0


    val change =
        if (first != 0.0) {

            (
                    (last - first) /
                            abs(first)
                    ) * 100.0

        } else {

            0.0
        }


    return TrendResult(
        points = points,
        changePercent = change
    )
}


/* =============================================================
   BREAKDOWN
   ============================================================= */

private fun breakdown(
    periodTx: List<Transaction>,
    categories: List<CategoryEntity>,
    txType: String,
    currency: String
): List<BreakdownItem> {

    val categoryById =
        categories.associateBy {
            it.id
        }


    val grouped =
        periodTx
            .filter {
                it.type == txType
            }
            .groupBy {
                it.categoryId
            }


    val totals =
        grouped.map { (categoryId, txs) ->

            val amount =
                txs.sumOf {
                    it.amount
                }

            val category =
                categoryId?.let {
                    categoryById[it]
                }

            val label =
                category?.label
                    ?: "Uncategorized"

            val color =
                category?.tintColor
                    ?: Color.parseColor(
                        "#A0A6B0"
                    )

            BreakdownData(
                label = label,
                amount = amount,
                color = color
            )
        }
            .sortedByDescending {
                it.amount
            }


    val grandTotal =
        totals.sumOf {
            it.amount
        }


    return totals.map { item ->

        BreakdownItem(

            label = item.label,

            amount =
                formatCurrency(
                    item.amount,
                    currency
                ),

            percent =
                percentOf(
                    item.amount,
                    grandTotal
                ),

            color = item.color
        )
    }
}


private data class BreakdownData(
    val label: String,
    val amount: Double,
    val color: Int
)


/* =============================================================
   TOP CATEGORY TRENDS
   ============================================================= */

private fun buildCategoryTrends(
    currentTx: List<Transaction>,
    previousTx: List<Transaction>,
    categories: List<CategoryEntity>,
    currency: String,
    limit: Int = 5
): List<CategoryTrendItem> {

    val categoryById =
        categories.associateBy {
            it.id
        }


    fun totalsByCategory(
        transactions: List<Transaction>
    ): Map<Long?, Double> {

        return transactions
            .filter {
                it.type == TYPE_EXPENSE &&
                        it.categoryId != null
            }
            .groupBy {
                it.categoryId
            }
            .mapValues { (_, group) ->
                group.sumOf {
                    it.amount
                }
            }
    }


    val currentTotals =
        totalsByCategory(
            currentTx
        )

    val previousTotals =
        totalsByCategory(
            previousTx
        )


    return currentTotals
        .entries
        .sortedByDescending {
            it.value
        }
        .take(limit)
        .mapNotNull { (categoryId, currentAmount) ->

            val category =
                categoryById[categoryId]
                    ?: return@mapNotNull null

            val previousAmount =
                previousTotals[categoryId]
                    ?: 0.0

            val isNew =
                previousAmount <= 0.0


            val changePercent =
                if (isNew) {
                    null
                } else {
                    (
                            (currentAmount - previousAmount) /
                                    previousAmount
                            ) * 100.0
                }


            CategoryTrendItem(

                categoryLabel =
                    category.label,

                currentAmountLabel =
                    formatCurrency(
                        currentAmount,
                        currency
                    ),

                changeLabel =
                    if (isNew) {
                        "New"
                    } else {
                        formatSignedPercent(
                            changePercent!!
                        )
                    },

                isIncrease =
                    isNew ||
                            (
                                    changePercent != null &&
                                            changePercent > 0
                                    ),

                color =
                    category.tintColor
            )
        }
}


/* =============================================================
   BIGGEST TRANSACTIONS
   ============================================================= */

private fun buildBiggestTransactions(
    periodTx: List<Transaction>,
    categories: List<CategoryEntity>,
    currency: String,
    limit: Int = 5
): List<BiggestTransactionItem> {

    val categoryById =
        categories.associateBy {
            it.id
        }


    val incomeColor =
        Color.parseColor(
            "#2ED47A"
        )

    val expenseColor =
        Color.parseColor(
            "#FF5A8A"
        )


    return periodTx

        // Transfers are intentionally excluded.
        .filter {
            it.type == TYPE_INCOME ||
                    it.type == TYPE_EXPENSE
        }

        .sortedByDescending {
            it.amount
        }

        .take(limit)

        .map { tx ->

            val isIncome =
                tx.type == TYPE_INCOME

            val category =
                tx.categoryId?.let {
                    categoryById[it]
                }


            val itemColor =
                category?.tintColor
                    ?: if (isIncome) {
                        incomeColor
                    } else {
                        expenseColor
                    }


            BiggestTransactionItem(

                label =
                    category?.label
                        ?: if (isIncome) {
                            "Income"
                        } else {
                            "Uncategorized"
                        },

                dateLabel =
                    shortDateFormat.format(
                        Date(tx.createdAt)
                    ),

                amountLabel =
                    "${
                        if (isIncome) {
                            "+"
                        } else {
                            "-"
                        }
                    } ${
                        formatCurrency(
                            tx.amount,
                            currency
                        )
                    }",

                isIncome =
                    isIncome,

                color =
                    itemColor,

                iconBg =
                    withAlpha(
                        itemColor,
                        38
                    )
            )
        }
}


/* =============================================================
   PERIOD RANGE
   ============================================================= */

private fun rangeFor(
    period: DashboardPeriod
): Pair<Long, Long> {

    val now =
        System.currentTimeMillis()

    val calendar =
        Calendar.getInstance()

    calendar.timeInMillis =
        now


    return when (period) {

        DashboardPeriod.ThisMonth -> {

            calendar.set(
                Calendar.DAY_OF_MONTH,
                1
            )

            startOfDay(
                calendar.timeInMillis
            ) to (now + 1)
        }


        DashboardPeriod.ThisYear -> {

            calendar.set(
                Calendar.DAY_OF_YEAR,
                1
            )

            startOfDay(
                calendar.timeInMillis
            ) to (now + 1)
        }


        is DashboardPeriod.Custom -> {

            period.start to period.end
        }
    }
}


/* =============================================================
   PREVIOUS COMPARABLE PERIOD
   ============================================================= */

private fun previousComparablePeriod(
    period: DashboardPeriod,
    periodStart: Long,
    periodEnd: Long
): Pair<Long, Long> {

    val duration =
        (
                periodEnd - periodStart
                ).coerceAtLeast(1L)


    val calendar =
        Calendar.getInstance()


    return when (period) {

        DashboardPeriod.ThisMonth -> {

            calendar.timeInMillis =
                periodStart

            calendar.add(
                Calendar.MONTH,
                -1
            )

            val previousStart =
                startOfDay(
                    calendar.timeInMillis
                )

            previousStart to
                    previousStart + duration
        }


        DashboardPeriod.ThisYear -> {

            calendar.timeInMillis =
                periodStart

            calendar.add(
                Calendar.YEAR,
                -1
            )

            val previousStart =
                startOfDay(
                    calendar.timeInMillis
                )

            previousStart to
                    previousStart + duration
        }


        is DashboardPeriod.Custom -> {

            (
                    periodStart - duration
                    ) to periodStart
        }
    }
}


/* =============================================================
   PERIOD LABEL
   ============================================================= */

private fun labelFor(
    period: DashboardPeriod
): String {

    return when (period) {

        DashboardPeriod.ThisMonth ->
            "This Month"

        DashboardPeriod.ThisYear ->
            "This Year"

        is DashboardPeriod.Custom -> {

            val format =
                SimpleDateFormat(
                    "MMM d",
                    Locale.getDefault()
                )

            "${
                format.format(
                    Date(period.start)
                )
            } - ${
                format.format(
                    Date(period.end - 1)
                )
            }"
        }
    }
}


/* =============================================================
   START OF DAY
   ============================================================= */

private fun startOfDay(
    timestamp: Long
): Long {

    val calendar =
        Calendar.getInstance()

    calendar.timeInMillis =
        timestamp

    calendar.set(
        Calendar.HOUR_OF_DAY,
        0
    )

    calendar.set(
        Calendar.MINUTE,
        0
    )

    calendar.set(
        Calendar.SECOND,
        0
    )

    calendar.set(
        Calendar.MILLISECOND,
        0
    )

    return calendar.timeInMillis
}


/* =============================================================
   START OF MONTH
   ============================================================= */

private fun startOfMonth(
    timestamp: Long
): Long {

    val calendar =
        Calendar.getInstance()

    calendar.timeInMillis =
        timestamp

    calendar.set(
        Calendar.DAY_OF_MONTH,
        1
    )

    calendar.set(
        Calendar.HOUR_OF_DAY,
        0
    )

    calendar.set(
        Calendar.MINUTE,
        0
    )

    calendar.set(
        Calendar.SECOND,
        0
    )

    calendar.set(
        Calendar.MILLISECOND,
        0
    )

    return calendar.timeInMillis
}


/* =============================================================
   MAX VALUE FOR BAR CHART
   ============================================================= */

private fun niceMax(
    value: Float
): Float {

    if (value <= 0f) {
        return 1000f
    }

    var step =
        1000f

    while (step < value) {
        step *= 1.5f
    }

    return step
}


/* =============================================================
   PERCENTAGE
   ============================================================= */

private fun percentOf(
    part: Double,
    whole: Double
): Int {

    if (whole <= 0.0) {
        return 0
    }

    return (
            (part / whole) * 100.0
            )
        .roundToInt()
        .coerceIn(
            0,
            100
        )
}


/* =============================================================
   SIGNED PERCENT
   ============================================================= */

private fun formatSignedPercent(
    percent: Double
): String {

    val rounded =
        percent.roundToInt()

    val sign =
        if (rounded > 0) {
            "+"
        } else {
            ""
        }

    return "$sign$rounded%"
}


/* =============================================================
   COLOR ALPHA
   ============================================================= */

private fun withAlpha(
    color: Int,
    alpha: Int
): Int {

    return Color.argb(
        alpha,
        Color.red(color),
        Color.green(color),
        Color.blue(color)
    )
}


/* =============================================================
   CURRENCY FORMAT
   ============================================================= */

private fun formatCurrency(
    amount: Double,
    currency: String
): String {

    return "$currency ${
        String.format(
            Locale.getDefault(),
            "%,.2f",
            amount
        )
    }"
}