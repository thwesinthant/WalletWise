package com.example.walletwise.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.walletwise.R
import com.example.walletwise.entity.MonthPoint
import kotlin.math.max

/**
 * Line chart with a soft gradient fill underneath, one point per month.
 *
 * Unlike a plain View, this one reports its own *desired* width based on how
 * many points it has ([pointSpacingDp] each) instead of just filling its
 * parent. Wrap it in a HorizontalScrollView with layout_width="wrap_content"
 * and it will lay out wider than the screen and scroll horizontally, while a
 * single point (or a narrow parent) still just fills the available space.
 *
 * Points are drawn inset from the left/right edges by [horizontalInset] so
 * the first and last month labels (text-aligned CENTER on their point) have
 * room to render in full instead of being cut off by the canvas/scroll edge.
 */
class LineAreaChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var points: List<MonthPoint> = emptyList()
    private var lineColor: Int = Color.BLUE

    private val density = resources.displayMetrics.density
    private val pointSpacingDp = 56f
    private val bottomLabelHeight = 20f * density
    private val topPadding = 8f * density
    private val horizontalInset = 20f * density

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_muted)
        textSize = 11f * density
        textAlign = Paint.Align.CENTER
    }

    fun setData(points: List<MonthPoint>, lineColor: Int) {
        this.points = points
        this.lineColor = lineColor
        linePaint.color = lineColor
        dotPaint.color = lineColor
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val resolvedHeight = if (heightMode == MeasureSpec.EXACTLY) {
            MeasureSpec.getSize(heightMeasureSpec)
        } else {
            (140 * density).toInt()
        }

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        // One "slot" per point plus room on each side for the end labels,
        // so 12 months lay out wider than the screen without clipping.
        val desiredContentWidth =
            (pointSpacingDp * density * max(points.size - 1, 0)).toInt() + (horizontalInset * 2).toInt()

        val resolvedWidth = if (widthMode == MeasureSpec.EXACTLY) {
            parentWidth
        } else {
            max(desiredContentWidth, parentWidth)
        }

        setMeasuredDimension(resolvedWidth, resolvedHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildGradient(h)
    }

    private fun rebuildGradient(h: Int) {
        fillPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            Color.argb(64, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)),
            Color.argb(0, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) return

        rebuildGradient(height)

        val chartHeight = height - bottomLabelHeight - topPadding
        val minY = points.minOf { it.balance }
        val maxY = points.maxOf { it.balance }
        val range = (maxY - minY).takeIf { it != 0f } ?: 1f
        val usableWidth = (width - horizontalInset * 2).coerceAtLeast(1f)
        val stepX = if (points.size > 1) usableWidth / (points.size - 1) else usableWidth

        fun yFor(value: Float): Float {
            val normalized = (value - minY) / range
            return topPadding + chartHeight - (normalized * chartHeight)
        }

        val linePath = Path()
        val fillPath = Path()
        val xs = FloatArray(points.size)
        val ys = FloatArray(points.size)

        points.forEachIndexed { index, point ->
            val x = horizontalInset + stepX * index
            val y = yFor(point.balance)
            xs[index] = x
            ys[index] = y
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, topPadding + chartHeight)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(xs.last(), topPadding + chartHeight)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)

        // Highlight the last (current) point and label every month underneath.
        canvas.drawCircle(xs.last(), ys.last(), 4f * density, dotPaint)

        points.forEachIndexed { index, point ->
            canvas.drawText(point.label, xs[index], height - bottomLabelHeight * 0.15f, labelPaint)
        }
    }
}
