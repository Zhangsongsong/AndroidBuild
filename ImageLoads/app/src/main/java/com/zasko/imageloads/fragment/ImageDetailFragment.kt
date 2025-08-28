package com.zasko.imageloads.fragment

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.zasko.imageloads.R
import com.zasko.imageloads.adapter.DetailImagesAdapter
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.MainLoadsInfo
import com.zasko.imageloads.databinding.FragmentDetailImageBinding
import com.zasko.imageloads.manager.ImageLoadsManager
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.PermissionUtil
import com.zasko.imageloads.utils.loadImageWithInside
import com.zasko.imageloads.utils.onClick
import com.zasko.imageloads.utils.switchThread
import io.reactivex.rxjava3.core.Single
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ImageDetailFragment : MainLoadFragment() {


    companion object {
        const val KEY_DATA = "key_data"
        private const val TAG = "ImageDetailFragment"
    }

    private lateinit var mainLoadsInfo: MainLoadsInfo

    private lateinit var binding: FragmentDetailImageBinding
    private lateinit var mAdapter: DetailImagesAdapter

    private var loadMoreIndex = 1

    private val isDownloading = AtomicBoolean(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mainLoadsInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(KEY_DATA, MainLoadsInfo::class.java)!!
            } else {
                it.getSerializable(KEY_DATA) as MainLoadsInfo
            }
            LogComponent.printD(tag = TAG, message = "mainLoadInfo:${mainLoadsInfo} ${Environment.getExternalStorageDirectory()}")
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDetailImageBinding.inflate(inflater)
        initView()
        getDetail()
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentCons) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        return binding.root
    }

    private fun initView() {
        binding.backIv.onClick {
            activity?.finish()
        }
        binding.downloadIv.onClick {
            FileUtil.createExternalDir()
            PermissionUtil.getReadAndWriteExternal(context = requireActivity())
            if (!File(FileUtil.getDownloadPath()).exists() || isDownloading.get()) {
                return@onClick
            }
            if ((mAdapter.getData().size) > 0) {
                val file = File("${FileUtil.getDownloadPath()}/${binding.nameTv.text}")
                if (!file.exists()) {
                    file.mkdirs()
                }
                LogComponent.printD(tag = TAG, message = "initView file:${file.absolutePath}")
            }
        }

        mAdapter = DetailImagesAdapter(loadMore = {
            if (!binding.bufferLoadingView.isVisible) {
                binding.bufferLoadingView.startAni()
            }
        })

        binding.coverIv.loadImageWithInside(url = mainLoadsInfo.url)
        binding.pictureRecycler.layoutManager = GridLayoutManager(context, 2)
        binding.pictureRecycler.adapter = mAdapter
    }

    private fun getDetail() {
        ImageLoadsManager.getXiuRenDetail(url = mainLoadsInfo.href).switchThread().doOnSuccess { info ->
            binding.loadingBar.isVisible = false
            binding.nameTv.text = info.name
            binding.descTv.text = info.desc
            binding.timeTv.text = info.time
            mAdapter.setData(list = info.pictures ?: emptyList())
            updateHasLoad()
//            loadMore()
        }.bindLife()
    }

    private fun loadMore() {
        if (isLoadEnd.get()) {
            return
        }
        if (isLoadingMore.get()) {
            return
        }
        isLoadingMore.set(true)
        LogComponent.printD(tag = TAG, message = "loadMore ${mainLoadsInfo.href} loadMoreIndex:${loadMoreIndex}")
        loadMoreIndex++
        Single.just(true).delay(5, TimeUnit.SECONDS).flatMap {
            ImageLoadsManager.getXiuRenDetailMore(url = toPageUrl(page = loadMoreIndex)).switchThread().doOnSuccess { list ->
                //如果是重复数据，直接判定end
                if (list.isEmpty() || mAdapter.getData().lastOrNull()?.url == list.lastOrNull()?.url) {
                    isLoadEnd.set(true)
                } else {
                    mAdapter.addData(list)
                }
                updateHasLoad()
            }
        }.doFinally {
            isLoadingMore.set(false)
            if (!isLoadEnd.get()) {
                loadMore()
            }
        }.bindLife()
    }

    @SuppressLint("SetTextI18n")
    private fun updateHasLoad() {
        binding.hasLoadCountTv.text = "${ContextCompat.getString(context, R.string.has_load)}${mAdapter?.getData()?.size}"
    }

    private fun toPageUrl(page: Int = 1): String {
        return "${mainLoadsInfo.href}?page=${page}"
    }
}