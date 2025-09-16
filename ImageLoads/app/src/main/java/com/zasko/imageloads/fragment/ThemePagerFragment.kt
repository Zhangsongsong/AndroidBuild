package com.zasko.imageloads.fragment

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.zasko.imageloads.base.BindLife
import com.zasko.imageloads.base.PagerLoadListener
import java.util.concurrent.atomic.AtomicBoolean

open class ThemePagerFragment : Fragment(), BindLife by BindLife(), PagerLoadListener {

    companion object {
        var screenWidth = 0

        const val KEY_DATA = "key_data"
    }


    private var isInitResume = false


    var isLoadEnd = AtomicBoolean(false)
    var isLoadingMore = AtomicBoolean(false)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (screenWidth <= 0) {
            screenWidth = context.resources.displayMetrics.widthPixels
        }
    }


    override fun onResume() {
        super.onResume()
        if (!isInitResume) {
            isInitResume = true
            initByResume()
        }
    }

    open fun initByResume() {

    }


    fun checkRunAfterLoadingMore(moreLockBack: () -> Unit = {}, block: () -> Unit = {}) {
        if (isLoadingMore.get()) {
            moreLockBack.invoke()
            return
        }
        block.invoke()
    }

    fun runInAct(block: (act: FragmentActivity) -> Unit) {
        activity?.let {
            block.invoke(it)
        }
    }


}