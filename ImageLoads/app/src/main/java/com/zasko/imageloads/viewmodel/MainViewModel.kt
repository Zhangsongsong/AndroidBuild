package com.zasko.imageloads.viewmodel

import android.content.Context
import com.zasko.imageloads.R
import com.zasko.imageloads.adapter.MainLoadsAdapter
import com.zasko.imageloads.base.BaseViewModel
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.manager.ImageLoadsManager
import com.zasko.imageloads.utils.switchThread
import java.util.concurrent.atomic.AtomicBoolean

class MainViewModel : BaseViewModel() {
    companion object {

        private const val TAG = "MainViewModel"
        val DRAWER_ITEMS = mutableListOf(R.string.xiuren, R.string.heisi)
    }

    private var currentId: Int = -1

    private val isLoadMore = AtomicBoolean(false)

    fun drawerItemClick(context: Context, id: Int = -1, adapter: MainLoadsAdapter? = null) {
        LogComponent.printD(tag = TAG, message = "drawerItemClick id:${id} currentId:${currentId} adapter:${adapter}")
        if (id == currentId) {
            return
        }
        currentId = id
        adapter?.removeData()
        when (id) {
            R.string.xiuren -> {
                ImageLoadsManager.getXiuRenData(context = context).switchThread().doOnSuccess {
                    adapter?.setData(it)
                }.bindLife()
            }

            R.string.heisi -> {

            }
        }
    }

    fun loadMore(context: Context, adapter: MainLoadsAdapter? = null) {
        if (isLoadMore.get()) {
            return
        }
        isLoadMore.set(true)
        when (currentId) {
            R.string.xiuren -> {
                ImageLoadsManager.getXiuRenMoreData(context = context).switchThread().doOnSuccess {
                    adapter?.addData(it)
                }.doFinally { isLoadMore.set(false) }.bindLife()
            }
        }
    }
}

