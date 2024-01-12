package com.zasko.androidbuild.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class BottomNavView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

//    private val navItems = ArrayList<NavItem>()


    init {
        gravity = Gravity.CENTER
    }


//    fun setNavItems(list: List<NavItem>) {
//        clearNav()
//        navItems.clear()
//        navItems.addAll(list)
//        initNavItems()
//    }
//
//    private fun initNavItems() {
//        if (navItems.size <= 0) {
//            return
//        }
//
//    }


//    private fun clearNav() {
//        removeAllViews()
//    }

//    class NavItem {
//        constructor(title: String)
//    }
}

class NavItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private lateinit var titleTv: TextView

    init {
        titleTv = TextView(context)
        val titleParam = FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        titleParam.gravity = Gravity.CENTER
        titleTv.layoutParams = titleParam
    }

}