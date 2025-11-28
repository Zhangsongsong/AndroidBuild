package com.zasko.imageloads.fragment.test

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.zasko.imageloads.adapter.MainLoadsAdapter
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.databinding.FragmentNormalBinding
import com.zasko.imageloads.fragment.LoadBaseFragment
import com.zasko.imageloads.manager.ImageLoadsManager

class HeiSiFragment : LoadBaseFragment() {

    private lateinit var binding: FragmentNormalBinding
    private lateinit var adapter: MainLoadsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNormalBinding.inflate(inflater)
        initView()
        return binding.root
    }

    private fun initView() {
        adapter = MainLoadsAdapter() {

        }
        binding.recyclerView.let {
            it.layoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL)
            it.itemAnimator = null
            it.adapter = adapter
        }

    }

    override fun initByResume() {
        super.initByResume()
        startLoadImages()
    }

    private fun startLoadImages() {
        ImageLoadsManager.looperLoadImage {
            ImageLoadsManager.getImageData().doOnSuccess { result ->
                Glide.with(this).asBitmap().load(result.data).into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        adapter.addData(mutableListOf(ImageLoadsInfo(url = result.data, width = resource.width, height = resource.height)))
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
            }.bindLife()
        }
    }

}