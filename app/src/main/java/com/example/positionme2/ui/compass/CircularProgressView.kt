package com.example.positionme2.ui.compass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Custom view for displaying a circular progress indicator for compass calibration
 */
class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint().apply {
        color = Color.LTGRAY
        alpha = 76 // 30% opacity
        style = Paint.Style.STROKE
        strokeWidth = 24f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 24f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val rectF = RectF()

    // Current progress from 0.0 to 1.0
    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateProgressColor()
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val size = minOf(width, height)

        // Calculate padding to center the circle
        val strokeWidth = backgroundPaint.strokeWidth
        val left = (width - size) / 2 + strokeWidth / 2
        val top = (height - size) / 2 + strokeWidth / 2
        val right = left + size - strokeWidth
        val bottom = top + size - strokeWidth

        rectF.set(left, top, right, bottom)

        // Draw background circle
        canvas.drawArc(rectF, 0f, 360f, false, backgroundPaint)

        // Draw progress arc
        canvas.drawArc(rectF, -90f, 360f * progress, false, progressPaint)
    }

    private fun updateProgressColor() {
        progressPaint.color = when {
            progress < 0.4f -> Color.RED
            progress < 0.8f -> Color.parseColor("#FFA500") // Orange
            else -> Color.GREEN
        }
    }
}
