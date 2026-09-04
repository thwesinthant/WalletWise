package com.example.walletwise.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.walletwise.entity.BreakdownItem
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/**
 * Ring/donut chart made of one rounded stroked arc per item, matching the
 * Compose Canvas drawArc() version (each slice has a small gap before the next).
 *
 * The ring itself never changes shape. Touching a slice (as soon as the
 * finger/pointer lands, no need to lift or click) just overlays that
 * slice's name and percentage as plain text in the center hole; touching
 * the same slice again, or touching outside the ring, clears it.
 */
class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var items: List<BreakdownItem> = emptyList()
    private var strokeWidthPx: Float = 18f * resources.displayMetrics.density
    private var selectedIndex: Int? = null

    private val density = resources.displayMetrics.density

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val arcRect = RectF()

    private val centerLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 11f * density
        color = android.graphics.Color.parseColor("#2E2E3A")
        typeface = Typeface.DEFAULT_BOLD
    }
    private val centerPercentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 13f * density
        typeface = Typeface.DEFAULT_BOLD
    }

    init {
        isClickable = true
        isFocusable = true
        // Without this, the platform draws its own highlight ring (blue on
        // most themes) over the view whenever it gains focus from a tap -
        // that's the blue circle showing up over the chart. We draw our own
        // selection feedback (the center text), so the system's default
        // highlight is redundant and gets turned off.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            defaultFocusHighlightEnabled = false
        }
    }

    fun setData(items: List<BreakdownItem>, strokeWidthDp: Float? = null) {
        this.items = items
        selectedIndex = null
        if (strokeWidthDp != null) {
            strokeWidthPx = strokeWidthDp * resources.displayMetrics.density
        }
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // The dashboard lives inside a NestedScrollView. Without this, the
        // slightest finger movement during a tap gets read as a scroll and
        // the parent steals the gesture (we get ACTION_CANCEL instead of
        // ACTION_UP), so the tap never registers. Claim the gesture as soon
        // as it starts inside us, and let go again once it's done.
        when (event.action) {
            MotionEvent.ACTION_DOWN -> parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                parent?.requestDisallowInterceptTouchEvent(false)
        }

        // React the moment the finger/pointer lands (ACTION_DOWN), not on
        // release - so it shows on press/hover without needing a full tap.
        if (event.action == MotionEvent.ACTION_DOWN && items.isNotEmpty()) {
            selectSliceAt(event.x, event.y, toggle = true)
        }
        if (event.action == MotionEvent.ACTION_UP) {
            // Still needed so accessibility services and click listeners
            // work; the visual update already happened on ACTION_DOWN above.
            performClick()
        }
        return true
    }

    /**
     * @param toggle when true (a press), landing on the already-selected
     * slice again clears it. Hover doesn't toggle - it just tracks whichever
     * slice the pointer is currently over, so repeated hover-move events
     * over the same slice don't flicker it on and off.
     */
    private fun selectSliceAt(x: Float, y: Float, toggle: Boolean) {
        val cx = width / 2f
        val cy = height / 2f
        val dx = x - cx
        val dy = y - cy
        val distance = hypot(dx, dy)
        val outerRadius = min(width, height) / 2f

        if (distance <= outerRadius) {
            // Angle measured the same way the arcs are drawn: 0 degrees at
            // 12 o'clock (i.e. drawArc's -90deg start), clockwise.
            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
            if (angle < 0f) angle += 360f

            var cumulative = 0f
            var hitIndex: Int? = null
            for ((index, item) in items.withIndex()) {
                val sweep = 360f * (item.percent / 100f)
                if (angle >= cumulative && angle < cumulative + sweep) {
                    hitIndex = index
                    break
                }
                cumulative += sweep
            }

            val newIndex = if (toggle && hitIndex != null && hitIndex == selectedIndex) null else hitIndex
            if (newIndex != selectedIndex) {
                selectedIndex = newIndex
                invalidate()
            }
        } else if (selectedIndex != null) {
            selectedIndex = null
            invalidate()
        }
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        // Mouse pointer moving over the chart with no button held - a
        // separate event stream from onTouchEvent's finger-press handling.
        when (event.action) {
            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE ->
                if (items.isNotEmpty()) selectSliceAt(event.x, event.y, toggle = false)
            MotionEvent.ACTION_HOVER_EXIT -> {
                selectedIndex = null
                invalidate()
            }
        }
        return super.onHoverEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (items.isEmpty()) return

        // Ring is drawn exactly as before - selection never changes its
        // shape, position, or size.
        arcPaint.strokeWidth = strokeWidthPx
        val half = strokeWidthPx / 2f
        arcRect.set(half, half, width - half, height - half)

        var startAngle = -90f
        items.forEach { item ->
            val sweep = 360f * (item.percent / 100f)
            arcPaint.color = item.color
            canvas.drawArc(arcRect, startAngle, sweep - 3f, false, arcPaint)
            startAngle += sweep
        }

        val index = selectedIndex ?: return
        val item = items.getOrNull(index) ?: return
        val cx = width / 2f
        val cy = height / 2f

        // Selection just overlays plain text in the center hole - name on
        // top, percentage below, colored to match the slice.
        centerPercentPaint.color = item.color
        val labelMetrics = centerLabelPaint.fontMetrics
        val percentMetrics = centerPercentPaint.fontMetrics
        val gap = 1f * density

        val labelBaselineY = cy - gap / 2f - (labelMetrics.descent)
        val percentBaselineY = cy + gap / 2f - percentMetrics.ascent

        canvas.drawText(item.label, cx, labelBaselineY, centerLabelPaint)
        canvas.drawText("${item.percent}%", cx, percentBaselineY, centerPercentPaint)
    }
}
