package com.zasko.accessibility.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.zasko.accessibility.R
import com.zasko.accessibility.databinding.ViewSwitchAutoBinding
import com.zasko.accessibility.utils.onClick

class AutoSwitchView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val TAG = AutoSwitchView::class.java.simpleName
    private var binding: ViewSwitchAutoBinding


    init {
        val view = View.inflate(context, R.layout.view_switch_auto, this)
        binding = ViewSwitchAutoBinding.bind(view)

        val attrs = context.obtainStyledAttributes(attrs, R.styleable.AutoSwitchView)

        setTitle(attrs.getString(R.styleable.AutoSwitchView_titleText) ?: "")
        setDes(attrs.getString(R.styleable.AutoSwitchView_desText) ?: "")
        setSwitch(attrs.getBoolean(R.styleable.AutoSwitchView_switchCheck, false))
        attrs.recycle()


        binding.contentView.onClick {
            binding.switchView.isChecked = !binding.switchView.isChecked
        }


    }


    fun setTitle(value: String) {
        binding.titleTv.text = value
    }

    fun setDes(value: String) {
        binding.desTv.text = value
    }

    fun setSwitch(isCheck: Boolean) {
        binding.switchView.isChecked = isCheck
    }


}