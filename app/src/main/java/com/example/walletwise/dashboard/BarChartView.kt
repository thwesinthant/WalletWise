package com.example.walletwise.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.walletwise.R
import androidx.core.content.ContextCompat
import com.example.walletwise.entity.DayBar

/**
 * Grouped income/expense bar chart. Everything (grid lines, bars, y-axis and
 * x-axis labels) is drawn in [onDraw], mirroring the Canvas-based chart that
 * used to live in the Compose version of this screen.
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var data: List<DayBar> = emptyList()
    private var incomeColor: Int = Color.GREEN
    private var expenseColor: Int = Color.BLUE
    private var maxValue: Float = 5000f

    private val density = resources.displayMetrics.density
    private val leftAxisWidth = 28f * density
    private val bottomLabelHeight = 24f * density

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.divider)
        strokeWidth = 1f * density
    }
    private val incomeBarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val expenseBarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val yLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_muted)
        textSize = 11f * density
        textAlign = Paint.Align.LEFT
    }
    private val xLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_muted)
        textSize = 12f * density
        textAlign = Paint.Align.CENTER
    }

    fun setData(days: List<DayBar>, incomeColor: Int, expenseColor: Int, maxValue: Float = 5000f) {
        this.data = days
        this.incomeColor = incomeColor
        this.expenseColor = expenseColor
        this.maxValue = maxValue
        incomeBarPaint.color = incomeColor
        expenseBarPaint.color = expenseColor
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val chartWidth = width - leftAxisWidth
        val chartHeight = height - bottomLabelHeight

        // Grid lines + y-axis labels, scaled to whatever maxValue actually is.
        val yLabels = (0..5).map { formatAxisLabel(maxValue * (5 - it) / 5f) }
        val stepY = chartHeight / (yLabels.size - 1)
        for (i in yLabels.indices) {
            val y = stepY * i
            canvas.drawLine(leftAxisWidth, y, width.toFloat(), y, gridPaint)
            canvas.drawText(yLabels[i], 0f, y + (yLabelPaint.textSize / 3f), yLabelPaint)
        }

        // Bars
        val groupWidth = chartWidth / data.size
        val barWidth = groupWidth * 0.22f
        val gap = barWidth * 0.35f
        val cornerRadius = 6f * density
        val rect = RectF()

        data.forEachIndexed { index, day ->
            val groupStart = leftAxisWidth + groupWidth * index
            val centerX = groupStart + groupWidth / 2f

            val incomeHeight = (day.income / maxValue) * chartHeight
            val expenseHeight = (day.expense / maxValue) * chartHeight

            val incomeLeft = centerX - gap / 2f - barWidth
            val expenseLeft = centerX + gap / 2f

            rect.set(incomeLeft, chartHeight - incomeHeight, incomeLeft + barWidth, chartHeight)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, incomeBarPaint)

            rect.set(expenseLeft, chartHeight - expenseHeight, expenseLeft + barWidth, chartHeight)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, expenseBarPaint)

            canvas.drawText(day.label, centerX, chartHeight + bottomLabelHeight * 0.7f, xLabelPaint)
        }
    }

    private fun formatAxisLabel(value: Float): String = when {
        value <= 0f -> "0"
        value >= 1000f -> "${(value / 1000f).let { if (it == it.toInt().toFloat()) it.toInt().toString() else "%.1f".format(it) }}k"
        else -> value.toInt().toString()
    }
}
