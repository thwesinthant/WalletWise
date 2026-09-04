package com.example.walletwise.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.walletwise.R

/**
 * Simple pill-shaped progress indicator: a light track with a colored fill
 * proportional to [progress]. Equivalent to the Compose ProgressBar() composable
 * (two stacked, clipped, rounded-corner Boxes).
 */
class ProgressBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var progress: Float = 0f
    private var trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.progress_track)
    }
    private var fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
    }
    private val rect = RectF()

    fun setProgress(progress: Float, color: Int) {
        this.progress = progress.coerceIn(0f, 1f)
        fillPaint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = height / 2f

        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, trackPaint)

        val fillWidth = width * progress
        if (fillWidth > 0f) {
            rect.set(0f, 0f, fillWidth, height.toFloat())
            canvas.drawRoundRect(rect, radius, radius, fillPaint)
        }
    }
}
