package com.zasko.androidbuild.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.zasko.androidbuild.BaseActivity
import com.zasko.androidbuild.databinding.ActivitySpringAnimationBinding
import com.zasko.androidbuild.utils.onClick
import com.zasko.androidbuild.views.dp

class SpringAnimationActivity : BaseActivity() {


    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, SpringAnimationActivity::class.java))
        }
    }

    private lateinit var binding: ActivitySpringAnimationBinding

    private var currentY = 100f.dp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySpringAnimationBinding.inflate(layoutInflater)
        setContentView(binding.root)



        binding.startAniBtn.onClick {

            if (currentY > 0) {
                SpringAnimation(binding.springView, DynamicAnimation.TRANSLATION_Y, (-100f).dp).apply {
                    spring.dampingRatio = SpringForce.DAMPING_RATIO_HIGH_BOUNCY
                }

            } else {
                SpringAnimation(binding.springView, DynamicAnimation.TRANSLATION_Y, 100f.dp).apply {
                    spring.dampingRatio = SpringForce.DAMPING_RATIO_HIGH_BOUNCY
                }

            }.start()
            currentY = -currentY
        }


    }
}