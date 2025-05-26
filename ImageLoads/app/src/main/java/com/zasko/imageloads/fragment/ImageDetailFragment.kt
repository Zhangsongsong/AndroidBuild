package com.zasko.imageloads.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.zasko.imageloads.adapter.DetailImagesAdapter
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.MainLoadsInfo
import com.zasko.imageloads.databinding.FragmentDetailImageBinding
import com.zasko.imageloads.manager.ImageLoadsManager
import com.zasko.imageloads.utils.loadImageWithInside
import com.zasko.imageloads.utils.switchThread

class ImageDetailFragment : MainLoadFragment() {


    companion object {
        const val KEY_DATA = "key_data"
        private const val TAG = "ImageDetailFragment"
    }

    private lateinit var mainLoadsInfo: MainLoadsInfo

    private lateinit var binding: FragmentDetailImageBinding
    private var mAdapter: DetailImagesAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mainLoadsInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(KEY_DATA, MainLoadsInfo::class.java)!!
            } else {
                it.getSerializable(KEY_DATA) as MainLoadsInfo
            }
            LogComponent.printD(tag = TAG, message = "mainLoadInfo:${mainLoadsInfo}")
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDetailImageBinding.inflate(inflater)
        initView()
        getDetail()
        return binding.root
    }

    private fun initView() {

        mAdapter = DetailImagesAdapter()

        binding.coverIv.loadImageWithInside(url = mainLoadsInfo.url)
        binding.pictureRecycler.layoutManager = GridLayoutManager(context, 2)
        binding.pictureRecycler.adapter = mAdapter
    }

    private fun getDetail() {
        ImageLoadsManager.getXiuRenDetail(url = mainLoadsInfo.href).switchThread().doOnSuccess { info ->
            binding.nameTv.text = info.name
            binding.descTv.text = info.desc
            binding.timeTv.text = info.time
//            mAdapter?.setData(list = info.pictures ?: emptyList())
        }.bindLife()
    }


}