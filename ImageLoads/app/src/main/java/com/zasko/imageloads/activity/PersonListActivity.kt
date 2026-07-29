package com.zasko.imageloads.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.zasko.imageloads.base.BaseComposeActivity
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.ImageListScreen
import com.zasko.imageloads.data.DataUseFrom
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.ui.xiuren.activity.XiuRenWebViewActivity
import com.zasko.imageloads.utils.Constants
import com.zasko.imageloads.utils.switchThread
import com.zasko.imageloads.ui.xiuren.XiuRenViewModel
import io.reactivex.rxjava3.core.Single

class PersonListActivity : BaseComposeActivity() {
    companion object {
        private const val KEY_DATA = "key_theme"
        private const val LOAD_MAX_SIZE = 20

        fun start(context: Context, data: MainThemeSelectInfo) {
            context.startActivity(Intent(context, PersonListActivity::class.java).apply {
                putExtra(KEY_DATA, data)
            })
        }
    }

    private lateinit var viewModel: XiuRenViewModel
    private lateinit var dataInfo: MainThemeSelectInfo
    private val images = mutableStateListOf<ImageLoadsInfo>()

    private var loadStartIndex = 0
    private var isRefreshing by mutableStateOf(false)
    private var isLoadingMoreState by mutableStateOf(false)
    private var isLoadEndState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataInfo = readThemeInfo()
        viewModel = ViewModelProvider(this)[XiuRenViewModel::class.java].apply {
            initBindLife(this@PersonListActivity)
        }

        setContent {
            ImageLoadsTheme {
                ImageListScreen(
                    title = dataInfo.title,
                    images = images,
                    isRefreshing = isRefreshing,
                    isLoadingMore = isLoadingMoreState,
                    onBack = ::finish,
                    onOpenWeb = ::openWebView,
                    onLoadMore = ::loadMoreData,
                    onImageClick = ::openDetail,
                )
            }
        }

        loadNewData()
    }

    private fun readThemeInfo(): MainThemeSelectInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(KEY_DATA, MainThemeSelectInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(KEY_DATA) as? MainThemeSelectInfo
        } ?: MainThemeSelectInfo()
    }

    private fun loadNewData() {
        if (isLoadingMoreState) {
            isRefreshing = false
            return
        }
        loadStartIndex = 0
        isLoadEndState = false
        isRefreshing = true
        getData(start = loadStartIndex)
            .switchThread()
            .doOnSuccess { list ->
                loadStartIndex += LOAD_MAX_SIZE
                images.clear()
                images.addAll(list)
                isLoadEndState = list.isEmpty()
            }
            .doFinally { isRefreshing = false }
            .bindLife()
    }

    private fun loadMoreData() {
        if (isRefreshing || isLoadingMoreState || isLoadEndState) {
            return
        }
        isLoadingMoreState = true
        getData(start = loadStartIndex)
            .switchThread()
            .doOnSuccess { list ->
                if (list.isEmpty()) {
                    isLoadEndState = true
                } else {
                    loadStartIndex += LOAD_MAX_SIZE
                    images.addAll(list)
                }
            }
            .doFinally { isLoadingMoreState = false }
            .bindLife()
    }

    private fun getData(start: Int): Single<List<ImageLoadsInfo>> {
        return when (dataInfo.dataUseFrom) {
            DataUseFrom.PRIVATE_FILE.value -> viewModel.getLocalData(start = start)
            else -> viewModel.getNetworkData(start = start)
        }
    }

    private fun openDetail(itemInfo: ImageLoadsInfo) {
        PersonDetailActivity.start(
            activity = this,
            data = itemInfo.apply {
                fromType = Constants.THEME_TYPE_XIUREN
            },
        )
    }

    private fun openWebView() {
        XiuRenWebViewActivity.start(context = this)
    }
}
