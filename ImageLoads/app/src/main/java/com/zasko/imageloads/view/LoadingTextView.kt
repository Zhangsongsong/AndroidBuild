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

    private val bucketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3F51B5")
        style = Paint.Style.FILL
    }

    private var progress = 0.5f   // 水位 0~1
    private var waveOffset = 0f // 波浪水平偏移
    private var waveLength = 0f // 波长

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            // 让偏移量在一个完整波长内循环
            val value = it.animatedValue as Float
            waveOffset = value * waveLength * 2
            invalidate()
        }
    }

    init {
        // 在 View attach 后才启动动画（防止未 attach 提前 start）
        if (!isInEditMode) {
            animator.start()
        }
    }

    fun setProgress(ratio: Float) {
        progress = ratio.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val bucketRect = RectF(10f, 10f, width - 10f, height - 10f)

        // 桶的外框
        canvas.drawRoundRect(bucketRect, 30f, 30f, bucketPaint)

        // 波长取容器宽度一半
        waveLength = bucketRect.width() / 2

        // 保存图层，限制水波只画在桶里
        val saveCount = canvas.saveLayer(bucketRect, null)

        // 裁剪桶内部
        canvas.clipRect(bucketRect.left, bucketRect.top, bucketRect.right, bucketRect.bottom)

        // 水位高度
        val waterLevel = bucketRect.bottom - (bucketRect.height() * progress)

        // 波浪路径
        val path = Path()
        path.moveTo(bucketRect.left - waveOffset, waterLevel)

        val waveHeight = 40f
        var x = -waveLength * 2
        while (x <= bucketRect.width() * 2) {
            // 波峰
            path.quadTo(
                x + waveLength / 2, waterLevel - waveHeight,
                x + waveLength, waterLevel
            )
            // 波谷
            path.quadTo(
                x + waveLength * 1.5f, waterLevel + waveHeight,
                x + waveLength * 2, waterLevel
            )
            x += waveLength * 2
        }

        path.lineTo(bucketRect.right, bucketRect.bottom)
        path.lineTo(bucketRect.left, bucketRect.bottom)
        path.close()

        canvas.drawPath(path, waterPaint)

        canvas.restoreToCount(saveCount)
    }
}

