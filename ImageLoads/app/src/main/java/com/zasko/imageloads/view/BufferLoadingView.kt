package com.zasko.imageloads.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import androidx.core.view.isVisible
import com.zasko.imageloads.R

class BufferLoadingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    private val scaleAni = ScaleAnimation(0.3f, 1f, 1f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
        repeatCount = -1
    }
    private val alphaAni = AlphaAnimation(1f, 0.2f).apply {
        repeatCount = -1
    }

    private val aniSet = AnimationSet(true).apply {
        addAnimation(scaleAni)
        addAnimation(alphaAni)
        duration = 500
    }

    init {
        setBackgroundColor(context.getColor(R.color.teal_200))
    }


    fun startAni(isVis: Boolean = true) {
        if (!this.isVisible) {
            this.isVisible = isVis
        }
        this.clearAnimation()
        this.startAnimation(aniSet)
    }

    fun stopAni(unVis: Boolean = false) {
        this.clearAnimation()
        if (this.isVisible) {
            this.isVisible = unVis
        }
    }

}