package com.zasko.imageloads.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.zasko.imageloads.R
import com.zasko.imageloads.utils.loadImage

class ImageMarkView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var coverIv: ImageView = ImageView(context)

    private var markView: View

    init {
        addView(coverIv, LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        markView = View(context).apply {
            alpha = 0.0f

            setBackgroundColor(context.getColor(R.color.color_222125))
        }
        addView(markView, LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
            gravity = Gravity.BOTTOM
        })
    }

    fun getCoverView(): ImageView {
        return coverIv
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val lp = markView.layoutParams as LayoutParams
        lp.height = (h * 0.8f).toInt()
        markView.layoutParams = lp
    }


}

fun ImageMarkView.loadImage(url: String) {
    this.getCoverView().loadImage(url)
}