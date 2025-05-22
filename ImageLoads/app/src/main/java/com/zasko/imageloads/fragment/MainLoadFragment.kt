package com.zasko.imageloads.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        if (screenWidth <= 0) {
            screenWidth = context.resources.displayMetrics.widthPixels
        }
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