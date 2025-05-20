package com.zasko.imageloads.fragment

import android.content.Context
import androidx.fragment.app.Fragment
import com.zasko.imageloads.base.BindLife
import java.util.concurrent.atomic.AtomicBoolean

open class MainLoadFragment : Fragment(), BindLife by BindLife() {
    
    companion object {
        var screenWidth = 0
    }


    private var isInitResume = false

    var isLoadMore = AtomicBoolean(false)


    override fun onAttach(context: Context) {
        super.onAttach(context)
        screenWidth = context.resources.displayMetrics.widthPixels
    }

    override fun onResume() {
        super.onResume()
        if (!isInitResume) {
            isInitResume = true
            initLoad()
        }
    }

    open fun initLoad() {

    }

}