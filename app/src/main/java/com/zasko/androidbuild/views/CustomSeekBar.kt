package com.zasko.androidbuild.views

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.zasko.androidbuild.R

class CustomSeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val TAG = "CustomSeekBar"
    }


    private val MAX = 100
    private val DEFAULT_MIN_HEIGHT = 2.dp.toFloat()
    private val DEFAULT_MAX_HEIGHT = 20.dp.toFloat()


    private val bgPaint = Paint()   //背景
    private val barPaint = Paint()  //进度
    private val roundPaint = Paint()    //圆


    private var currentProgress = 0f

    private var mRadius = 8f


    //最大最小高度
    private var defaultMinHeight = 0f
    private var defaultMaxHeight = 0f

    //实际绘制高度
    private var drawProgressHeight = 0

    //绘制圆高
    private var roundHeight = DEFAULT_MAX_HEIGHT * 1.5f

    private var mProgressBarListener: ProgressBarListener? = null

    private var isFromUser = false

    private var isMinBottom = false

    init {
        val attr = context.obtainStyledAttributes(attrs, R.styleable.CustomSeekBar)
        defaultMinHeight = attr.getDimension(R.styleable.CustomSeekBar_minHeight, DEFAULT_MIN_HEIGHT)
        defaultMaxHeight = attr.getDimension(R.styleable.CustomSeekBar_maxHeight, DEFAULT_MAX_HEIGHT)
        roundHeight = attr.getDimension(R.styleable.CustomSeekBar_roundHeight, DEFAULT_MAX_HEIGHT * 1.5f)
        isMinBottom = attr.getBoolean(R.styleable.CustomSeekBar_is_min_bottom, false)

        //默认绘制最小的先
        drawProgressHeight = defaultMinHeight.toInt()

        bgPaint.isAntiAlias = true
        bgPaint.color = attr.getColor(R.styleable.CustomSeekBar_bgColor, Color.GRAY)

        barPaint.isAntiAlias = true
        barPaint.color = attr.getColor(R.styleable.CustomSeekBar_progressColor, Color.GREEN)

        roundPaint.isAntiAlias = true
        roundPaint.color = barPaint.color
        roundPaint.style = Paint.Style.FILL_AND_STROKE
        attr.recycle()
    }

    fun setProgress(progress: Float) {
//        IKLog.d("$TAG setProgress:${progress} isFromUser:${isFromUser}")
        if (isFromUser) {
            return
        }
        currentProgress = progress
        invalidate()
    }

    fun getProgress(): Float {
        return currentProgress
    }

    fun setProgressBarListener(listener: ProgressBarListener) {
        mProgressBarListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas?.let {
            val height = measuredHeight * 1.0f
            val width = measuredWidth * 1.0f

            val barWidth = width / MAX * currentProgress

            if (isFromUser) {
                val startHeight = height / 2 - drawProgressHeight / 2    //开始高度
                it.drawRoundRect(0f, startHeight, width, startHeight + drawProgressHeight, mRadius, mRadius, bgPaint)
                //进度条
                it.drawRoundRect(0f, startHeight, barWidth, startHeight + drawProgressHeight, mRadius, mRadius, barPaint)

                it.drawCircle(barWidth, height / 2, roundHeight / 2, roundPaint)

            } else {

                val startHeight = if (isMinBottom) {
                    height - drawProgressHeight
                } else {
                    height / 2 - drawProgressHeight / 2
                }
                it.drawRoundRect(0f, startHeight, width, startHeight + drawProgressHeight, mRadius, mRadius, bgPaint)
                //进度条
                it.drawRoundRect(0f, startHeight, barWidth, startHeight + drawProgressHeight, mRadius, mRadius, barPaint)
            }

        }
    }

    private var downX: Float = 0f
    private var lastProgress = 0f


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    isFromUser = true
                    downX = it.x
                    lastProgress = currentProgress
                    drawProgressHeight = defaultMaxHeight.toInt()

                    mProgressBarListener?.onStartTrackingTouch()

                    invalidate()
//                    IKLog.d("$TAG ACTION_DOWN:${downX}")
                }

                MotionEvent.ACTION_MOVE -> {
                    if (event.x != downX) {
                        val x = event.x - downX
                        currentProgress = lastProgress + x / measuredWidth * MAX
                        if (currentProgress < 0) {
                            currentProgress = 0f
                        } else if (currentProgress > MAX) {
                            currentProgress = MAX * 1.0f
                        }
//                        IKLog.d("$TAG ACTION_MOVE currentProgress:${currentProgress.toInt()}")
                        mProgressBarListener?.onProgressChanged(currentProgress.toInt(), isFromUser)
                        invalidate()
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isFromUser = false
                    drawProgressHeight = defaultMinHeight.toInt()
                    mProgressBarListener?.onStopTrackingTouch()
                    invalidate()
                }

            }
        }
        return true
    }

    interface ProgressBarListener {
        fun onProgressChanged(progress: Int, fromUser: Boolean) {

        }

        fun onStartTrackingTouch() {

        }

        fun onStopTrackingTouch() {

        }
    }
}

val Int.dp: Int
    get() = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics
    ).toInt()
