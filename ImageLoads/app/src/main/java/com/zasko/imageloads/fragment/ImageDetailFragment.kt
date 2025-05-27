package com.zasko.imageloads.fragment

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
import java.io.File

class ImageDetailFragment : MainLoadFragment() {


    companion object {
        const val KEY_DATA = "key_data"
        private const val TAG = "ImageDetailFragment"
    }

    private lateinit var mainLoadsInfo: MainLoadsInfo

    private lateinit var binding: FragmentDetailImageBinding
    private var mAdapter: DetailImagesAdapter? = null

    private var loadMoreIndex = 1

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
        return binding.root
    }

    private fun initView() {
        binding.backIv.onClick {
            activity?.finish()
        }
        binding.downloadIv.onClick {
            FileUtil.createExternalDir()
            PermissionUtil.getReadAndWriteExternal(context = requireActivity())
            if (!File(FileUtil.getDownloadPath()).exists()) {
                return@onClick
            }
            if ((mAdapter?.getData()?.size ?: 0) > 0) {
                val file = File("${FileUtil.getDownloadPath()}/${binding.nameTv.text}")
                LogComponent.printD(tag = TAG, message = "initView file:${file.exists()}")
                if (!file.exists()) {
                    file.mkdirs()
                }
            }
        }

        mAdapter = DetailImagesAdapter {
//            loadMore()
        }

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
            mAdapter?.setData(list = info.pictures ?: emptyList())
            updateHasLoad()
        }.bindLife()
    }

    private fun loadMore() {
        if (isLoadMore.get() || isLoadEnd.get()) {
            return
        }
        isLoadMore.set(true)
        LogComponent.printD(tag = TAG, message = "loadMore ${mainLoadsInfo.href} loadMoreIndex:${loadMoreIndex}")
        loadMoreIndex++
        ImageLoadsManager.getXiuRenDetailMore(url = mainLoadsInfo.href.toPageUrl(page = loadMoreIndex)).switchThread().doOnSuccess { list ->
            //如果是重复数据，直接判定end
            if (list.isEmpty() || mAdapter?.getData()?.lastOrNull()?.url == list.lastOrNull()?.url) {
                isLoadEnd.set(true)
            } else {
                mAdapter?.addData(list)
            }
            updateHasLoad()
        }.doFinally {
            isLoadMore.set(false)
        }.bindLife()

    }

    private fun updateHasLoad() {
        binding.hasLoadCountTv.text = "${ContextCompat.getString(context, R.string.has_load)}${mAdapter?.getData()?.size}"
    }

    private fun String.toPageUrl(page: Int = 1): String {
        return "${mainLoadsInfo.href}?page=${page}"
    }


}