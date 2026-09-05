package com.example.walletwise.dashboard

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.util.Pair as AndroidXPair
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.walletwise.R

import com.example.walletwise.database.AppDatabase
import com.example.walletwise.entity.AccountItem
import com.example.walletwise.entity.BreakdownItem
import com.example.walletwise.entity.DashboardPeriod
import com.example.walletwise.entity.DashboardUiState
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Analytics / reports screen (charts on top of accounts, categories and
 * transactions). Same shape as DashboardActivity: an Activity that pulls
 * straight from the existing Room DAOs via combine() + lifecycleScope —
 * no ViewModel, no Repository class, no second database.
 *
 * Launch with an Intent extra "USER_ID", same convention as the rest of
 * the app's Activities.
 */
class DashboardAnalyticsActivity : AppCompatActivity() {

    // =========================================================
    // USER / DATABASE
    // =========================================================

    private var currentUserId: Int = -1

    private var userCurrency: String = "MMK"

    private lateinit var database: AppDatabase


    // =========================================================
    // PERIOD SELECTION
    // =========================================================

    private val selectedPeriodFlow = MutableStateFlow<DashboardPeriod>(DashboardPeriod.ThisMonth)

    private var selectedPeriod: DashboardPeriod = DashboardPeriod.ThisMonth

    private var hasAutoScrolledTrend = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setupBottomInsets()

        // =========================================================
        // USER ID
        // =========================================================

        currentUserId = intent.getIntExtra("USER_ID", -1)

        if (currentUserId == -1) {
            finish()
            return
        }

        database = AppDatabase.getDatabase(applicationContext)

        setupTopBar()
        setupPeriodTabs()
        loadUserCurrencyThenObserve()
    }

    private fun setupBottomInsets() {

        val scrollView = findViewById<androidx.core.widget.NestedScrollView>(
            R.id.dashboardScroll
        )

        val content = findViewById<LinearLayout>(
            R.id.dashboardContent
        )

        val baseBottomPadding = content.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { _, insets ->

            val navigationBarBottom =
                insets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()
                ).bottom

            content.setPadding(
                content.paddingLeft,
                content.paddingTop,
                content.paddingRight,
                baseBottomPadding + navigationBarBottom
            )

            insets
        }

        ViewCompat.requestApplyInsets(scrollView)
    }

    // ---- Currency ---------------------------------------------------------

    private fun loadUserCurrencyThenObserve() {
        lifecycleScope.launch {
            val user = database.userDao().getUserByIdOnce(currentUserId)
            userCurrency = user?.currency?.ifBlank { "MMK" } ?: "MMK"
            observeDashboard()
        }
    }

    // ---- Top bar ---------------------------------------------------------

    private fun setupTopBar() {
        val topBarRoot = findViewById<View>(R.id.topBar)

        val basePaddingStart = topBarRoot.paddingStart
        val basePaddingTop = topBarRoot.paddingTop
        val basePaddingEnd = topBarRoot.paddingEnd
        val basePaddingBottom = topBarRoot.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(topBarRoot) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                basePaddingStart + bars.left,
                basePaddingTop + bars.top,
                basePaddingEnd + bars.right,
                basePaddingBottom
            )
            insets
        }

        topBarRoot.findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // ---- Period tabs ---------------------------------------------------

    private fun setupPeriodTabs() {
        val tabsRoot = findViewById<View>(R.id.periodTabs)
        val tabThisMonth = tabsRoot.findViewById<TextView>(R.id.tabThisMonth)
        val tabThisYear = tabsRoot.findViewById<TextView>(R.id.tabThisYear)
        val tabCustom = tabsRoot.findViewById<TextView>(R.id.tabCustom)
        val tabs = listOf(tabThisMonth, tabThisYear, tabCustom)

        fun styleTabs() {
            val selectedTab = when (selectedPeriod) {
                DashboardPeriod.ThisMonth -> tabThisMonth
                DashboardPeriod.ThisYear -> tabThisYear
                is DashboardPeriod.Custom -> tabCustom
            }
            tabs.forEach { tab ->
                val isSelected = tab === selectedTab
                tab.setBackgroundResource(if (isSelected) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected)
                tab.setTextColor(
                    ContextCompat.getColor(this, if (isSelected) R.color.blue_primary else R.color.text_secondary)
                )
                tab.typeface = ResourcesCompat.getFont(this, if (isSelected) R.font.dosis_bold else R.font.dosis_medium)
            }
        }

        fun choosePeriod(period: DashboardPeriod) {
            selectedPeriod = period
            styleTabs()
            selectedPeriodFlow.value = period
        }

        tabThisMonth.setOnClickListener { choosePeriod(DashboardPeriod.ThisMonth) }
        tabThisYear.setOnClickListener { choosePeriod(DashboardPeriod.ThisYear) }
        tabCustom.setOnClickListener { showCustomRangePicker { start, end -> choosePeriod(DashboardPeriod.Custom(start, end)) } }

        styleTabs()
    }

    /** Lets the person pick an actual start/end date instead of "Custom" being a fixed window in disguise. */
    private fun showCustomRangePicker(onRangePicked: (start: Long, end: Long) -> Unit) {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select date range")
            .setTheme(R.style.ThemeOverlay_App_DatePicker)
            .setSelection(
                AndroidXPair(
                    MaterialDatePicker.thisMonthInUtcMilliseconds(),
                    MaterialDatePicker.todayInUtcMilliseconds()
                )
            )
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            // The picker works in UTC midnight millis; re-anchor to local-day
            // boundaries so filtering against createdAt (stored in local time) is correct.
            val startCal = Calendar.getInstance().apply {
                timeInMillis = selection.first
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                timeInMillis = selection.second
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            onRangePicked(startCal.timeInMillis, endCal.timeInMillis)
        }
        picker.show(supportFragmentManager, "custom_range_picker")
    }

    // ---- Reactive binding: straight off the existing DAOs -------------------

    private fun observeDashboard() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                val accountsFlow = database.accountDao().getAccountBalances(currentUserId)
                val categoriesFlow = database.categoryDao().observeAll(currentUserId)
                val transactionsFlow = database.transactionDao().getAllTransactions(currentUserId)

                combine(
                    accountsFlow,
                    categoriesFlow,
                    transactionsFlow,
                    selectedPeriodFlow
                ) { accounts, categories, transactions, period ->
                    buildDashboardUiState(
                        accounts = accounts,
                        categories = categories,
                        transactions = transactions,
                        period = period,
                        currency = userCurrency
                    )
                }.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: DashboardUiState) {
        bindBalanceOverview(state)
        bindIncomeVsExpense(state)
        bindBreakdownCard(
            cardId = R.id.incomeBreakdownCard,
            title = "Income Breakdown",
            total = state.incomeBreakdownTotal,
            items = state.incomeBreakdownItems
        )
        bindBreakdownCard(
            cardId = R.id.expenseBreakdownCard,
            title = "Expense Breakdown",
            total = state.expenseBreakdownTotal,
            items = state.expenseBreakdownItems
        )
        bindBalanceTrend(state)
        bindTopCategoriesCard(state)
        bindBiggestTransactionsCard(state)
        bindAccounts(state)
    }

    // ---- Balance overview ------------------------------------------------

    private fun bindBalanceOverview(state: DashboardUiState) {
        val cardRoot = findViewById<View>(R.id.balanceOverviewCard)
        cardRoot.findViewById<TextView>(R.id.tvTotalBalance).text = state.totalBalance

        configureAmountChip(
            chipRoot = cardRoot.findViewById(R.id.chipIncome),
            label = "Income",
            amount = state.periodIncome,
            bgDrawableRes = R.drawable.bg_chip_income,
            iconRes = R.drawable.ic_arrow_upward,
            iconTintColorRes = R.color.income_green
        )

        configureAmountChip(
            chipRoot = cardRoot.findViewById(R.id.chipExpense),
            label = "Expense",
            amount = state.periodExpense,
            bgDrawableRes = R.drawable.bg_chip_expense,
            iconRes = R.drawable.ic_arrow_downward,
            iconTintColorRes = R.color.expense_red
        )
    }

    private fun configureAmountChip(
        chipRoot: View,
        label: String,
        amount: String,
        bgDrawableRes: Int,
        iconRes: Int,
        iconTintColorRes: Int
    ) {
        chipRoot.setBackgroundResource(bgDrawableRes)
        chipRoot.findViewById<TextView>(R.id.tvChipLabel).text = label
        chipRoot.findViewById<TextView>(R.id.tvChipAmount).text = amount
        val icon = chipRoot.findViewById<ImageView>(R.id.ivChipIcon)
        icon.setImageResource(iconRes)
        icon.setColorFilter(ContextCompat.getColor(this, iconTintColorRes))
    }

    // ---- Income vs expense -------------------------------------------------

    private fun bindIncomeVsExpense(state: DashboardUiState) {
        val cardRoot = findViewById<View>(R.id.incomeVsExpenseCard)
        cardRoot.findViewById<TextView>(R.id.tvPeriodBadge).text = state.periodLabel
        cardRoot.findViewById<TextView>(R.id.tvIncomeExpenseTotal).text = state.periodNet
        cardRoot.findViewById<BarChartView>(R.id.barChart).setData(
            days = state.weekBars,
            incomeColor = ContextCompat.getColor(this, R.color.income_green),
            expenseColor = ContextCompat.getColor(this, R.color.expense_blue),
            maxValue = state.weekMaxValue
        )
    }

    // ---- Breakdown cards (income + expense donut/legend) -------------------

    private fun bindBreakdownCard(cardId: Int, title: String, total: String, items: List<BreakdownItem>) {
        val cardRoot = findViewById<View>(cardId)
        cardRoot.findViewById<TextView>(R.id.tvBreakdownTitle).text = title
        cardRoot.findViewById<TextView>(R.id.tvBreakdownTotal).text = total
        cardRoot.findViewById<DonutChartView>(R.id.donutChart).setData(items, strokeWidthDp = 18f)

        val legendContainer = cardRoot.findViewById<LinearLayout>(R.id.legendContainer)
        legendContainer.removeAllViews()
        items.forEachIndexed { index, item ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_legend_row, legendContainer, false)
            (row.findViewById<View>(R.id.dotView).background.mutate() as GradientDrawable).setColor(item.color)
            row.findViewById<TextView>(R.id.tvLegendLabel).text = item.label
            row.findViewById<TextView>(R.id.tvLegendAmount).text = item.amount
            row.findViewById<TextView>(R.id.tvLegendPercent).text = "${item.percent}%"
            row.findViewById<View>(R.id.dividerView).visibility =
                if (index == items.lastIndex) View.GONE else View.VISIBLE
            legendContainer.addView(row)
        }
    }

    // ---- Balance trend (12 months, horizontal scroll) ------------------------

    private fun bindBalanceTrend(state: DashboardUiState) {
        val cardRoot = findViewById<View>(R.id.balanceTrendCard)
        cardRoot.findViewById<TextView>(R.id.tvCurrentValue).text = state.trendCurrentValue

        val tvChangeLabel = cardRoot.findViewById<TextView>(R.id.tvChangeLabel)
        tvChangeLabel.text = state.trendChangeLabel
        tvChangeLabel.setTextColor(
            ContextCompat.getColor(this, if (state.trendChangeIsNegative) R.color.expense_red else R.color.income_green)
        )

        cardRoot.findViewById<LineAreaChartView>(R.id.lineChart).setData(
            points = state.monthlyTrend,
            lineColor = ContextCompat.getColor(this, R.color.blue_primary)
        )

        // First time data loads, snap the scroll to the right so "Today" (the
        // most recent month) is what's visible without the user scrolling.
        if (!hasAutoScrolledTrend && state.monthlyTrend.isNotEmpty()) {
            hasAutoScrolledTrend = true
            val scrollView = cardRoot.findViewById<HorizontalScrollView>(R.id.trendScroll)
            scrollView.post { scrollView.fullScroll(View.FOCUS_RIGHT) }
        }
    }

    // ---- Top spending categories (period-over-period) ---------------------

    private fun bindTopCategoriesCard(state: DashboardUiState) {
        val cardRoot = findViewById<View>(R.id.topCategoriesCard)
        val container = cardRoot.findViewById<LinearLayout>(R.id.topCategoriesContainer)
        val emptyState = cardRoot.findViewById<TextView>(R.id.tvTopCategoriesEmptyState)

        container.removeAllViews()

        if (state.topCategoryTrends.isEmpty()) {
            container.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            return
        }

        container.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        state.topCategoryTrends.forEach { item ->
            val row = LayoutInflater.from(this)
                .inflate(R.layout.item_category_trend_row, container, false)

            val changeColor = ContextCompat.getColor(
                this,
                if (item.isIncrease) R.color.expense_red else R.color.income_green
            )

            (row.findViewById<View>(R.id.dotView).background.mutate() as GradientDrawable).setColor(item.color)
            row.findViewById<TextView>(R.id.tvCategoryTrendLabel).text = item.categoryLabel
            row.findViewById<TextView>(R.id.tvCategoryTrendChange).apply {
                text = item.changeLabel
                setTextColor(changeColor)
            }
            row.findViewById<TextView>(R.id.tvCategoryTrendAmount).text =
                "${item.currentAmountLabel} this period"

            container.addView(row)
        }
    }

    // ---- Biggest transactions ---------------------------------------------

    private fun bindBiggestTransactionsCard(state: DashboardUiState) {
        val cardRoot = findViewById<View>(R.id.biggestTransactionsCard)
        val container = cardRoot.findViewById<LinearLayout>(R.id.biggestTransactionsContainer)
        val emptyState = cardRoot.findViewById<TextView>(R.id.tvBiggestTransactionsEmptyState)

        container.removeAllViews()

        if (state.biggestTransactions.isEmpty()) {
            container.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            return
        }

        container.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        state.biggestTransactions.forEach { item ->
            val row = LayoutInflater.from(this)
                .inflate(R.layout.item_biggest_transaction_row, container, false)

            val amountColor = ContextCompat.getColor(
                this,
                if (item.isIncome) R.color.income_green else R.color.expense_red
            )

            val iconBg = row.findViewById<FrameLayout>(R.id.ivBigTxIconBg)
            (iconBg.background.mutate() as GradientDrawable).setColor(item.iconBg)

            row.findViewById<ImageView>(R.id.ivBigTxIcon).apply {
                setImageResource(if (item.isIncome) R.drawable.ic_arrow_upward else R.drawable.ic_arrow_downward)
                setColorFilter(item.color)
            }
            row.findViewById<TextView>(R.id.tvBigTxLabel).text = item.label
            row.findViewById<TextView>(R.id.tvBigTxDate).text = item.dateLabel
            row.findViewById<TextView>(R.id.tvBigTxAmount).apply {
                text = item.amountLabel
                setTextColor(amountColor)
            }

            container.addView(row)
        }
    }

    // ---- Accounts ------------------------------------------------------

    private fun bindAccounts(state: DashboardUiState) {
        val cardRoot = findViewById<View>(R.id.accountsCard)
        val container = cardRoot.findViewById<LinearLayout>(R.id.accountsContainer)
        container.removeAllViews()

        state.accounts.forEach { account: AccountItem ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_account_row, container, false)

            val iconBg = row.findViewById<FrameLayout>(R.id.ivIconBg)
            (iconBg.background.mutate() as GradientDrawable).setColor(account.iconBg)

            row.findViewById<ImageView>(R.id.ivIcon).setColorFilter(account.color)
            row.findViewById<TextView>(R.id.tvAccountName).text = account.name
            row.findViewById<TextView>(R.id.tvAccountBalance).text = account.balance

            row.findViewById<ProgressBarView>(R.id.progressBar).setProgress(account.percent / 100f, account.color)
            row.findViewById<TextView>(R.id.tvAccountPercent).apply {
                text = "${account.percent}%"
                setTextColor(account.color)
            }

            container.addView(row)
        }
    }
}
