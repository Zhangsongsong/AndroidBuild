package com.zasko.imageloads.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.zasko.imageloads.R
import com.zasko.imageloads.components.LogComponent

class LoadingTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val textView: TextView = TextView(context).apply {
        textSize = 30f
        typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        setTextColor(context.getColor(R.color.color_00c8ff))

    }

    init {
        addView(textView, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        })
    }

    fun getTextView(): TextView {
        return textView
    }

}

class WaterBucketView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val TAG = "WaterBucketView"
    }

    private val bucketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    // 前层水
    private val frontWavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3F51B5")
        style = Paint.Style.FILL
        alpha = 180
    }

    // 后层水（稍浅）
    private val backWavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5C6BC0")
        style = Paint.Style.FILL
        alpha = 120
    }

    private var progress = 0.0f          // 当前水位
    private var waveOffset1 = 0f         // 前层波浪偏移
    private var waveOffset2 = 0f         // 后层波浪偏移
    private var waveLength = 0f          // 基础波长

    private var progressAnimator: ValueAnimator? = null

    // 两个偏移动画，速度不同
    private val animator1 = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            waveOffset1 = (it.animatedValue as Float) * waveLength
            invalidate()
        }
    }

    private val animator2 = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 4000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            waveOffset2 = (it.animatedValue as Float) * waveLength
            invalidate()
        }
    }

    fun setProgress(target: Float) {
        LogComponent.printD(TAG, "setProgress target:${target}")
        val end = target.coerceIn(0f, 1f)
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(progress, end).apply {
            duration = (Math.abs(end - progress) * 2000).toLong().coerceAtLeast(800)
            interpolator = LinearInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun getProgress(): Float {
        return progress
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator1.isStarted) animator1.start()
        if (!animator2.isStarted) animator2.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator1.cancel()
        animator2.cancel()
        progressAnimator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val bucketRect = RectF(10f, 10f, width - 10f, height - 10f)

        // 桶外框
        canvas.drawRoundRect(bucketRect, 30f, 30f, bucketPaint)

        waveLength = bucketRect.width()

        val saveCount = canvas.saveLayer(bucketRect, null)

        // ✅ 用圆角矩形做裁剪
        val clipPath = Path().apply {
            addRoundRect(bucketRect, 30f, 30f, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        val waterLevel = bucketRect.bottom - (bucketRect.height() * progress)

        // 画后层波浪
        drawWave(canvas, bucketRect, waterLevel, waveOffset2, height * 0.015f, backWavePaint)

        // 画前层波浪
        drawWave(canvas, bucketRect, waterLevel, waveOffset1, height * 0.02f, frontWavePaint)

        canvas.restoreToCount(saveCount)
    }


    private fun drawWave(
        canvas: Canvas, bucketRect: RectF, waterLevel: Float, offset: Float, amplitude: Float, paint: Paint
    ) {
        val path = Path()
        path.moveTo(bucketRect.left, waterLevel)

        var x = bucketRect.left
        while (x <= bucketRect.right) {
            val y = (amplitude * Math.sin((2 * Math.PI / waveLength) * (x + offset))).toFloat()
            path.lineTo(x, waterLevel + y)
            x += 10
        }

        path.lineTo(bucketRect.right, bucketRect.bottom)
        path.lineTo(bucketRect.left, bucketRect.bottom)
        path.close()

        canvas.drawPath(path, paint)
    }
}


