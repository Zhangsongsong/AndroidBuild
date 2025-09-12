package com.zasko.imageloads.adapter

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.databinding.ItemMainLoadsBinding
import com.zasko.imageloads.fragment.ThemePagerFragment
import com.zasko.imageloads.utils.onClick

class MainLoadsAdapter(private val loadMore: () -> Unit = {}, private val itemListener: (ImageLoadsInfo) -> Unit) :
    RecyclerView.Adapter<ViewHolder>() {


    private val data = ArrayList<ImageLoadsInfo>()

    fun setData(list: List<ImageLoadsInfo>) {
        val size = list.size
        data.clear()
        data.addAll(list)
        notifyItemRangeChanged(0, size)
    }

    fun addData(list: List<ImageLoadsInfo>) {
        val size = data.size
        data.addAll(list)
        notifyItemRangeInserted(size, list.size)
    }


    fun removeData() {
        val size = data.size
        data.clear()
        notifyItemRangeRemoved(0, size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return MHolder(binding = ItemMainLoadsBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position >= 0 && position < data.size) {
            (holder as MHolder).bind(data[position])

            if (position == data.size - 2) {
                LogComponent.printD(tag = "MainLoadsAdapter", message = "onBindViewHolder pos:${position} size:${data.size}")
                loadMore.invoke()
            }
        }
    }


    inner class MHolder(private val binding: ItemMainLoadsBinding) : ViewHolder(binding.root) {
        private var currentInfo: ImageLoadsInfo? = null

        init {
            binding.coverIv.onClick {
                currentInfo?.let(itemListener)
            }
        }

        fun bind(info: ImageLoadsInfo) {
            currentInfo = info
            val param = binding.coverIv.layoutParams
            param.height = (ThemePagerFragment.screenWidth / 2) * info.height / info.width
            binding.coverIv.layoutParams = param
            Glide.with(binding.coverIv.context).load(info.url).diskCacheStrategy(DiskCacheStrategy.DATA)
                .addListener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable, model: Any, target: Target<Drawable>?, dataSource: DataSource, isFirstResource: Boolean
                    ): Boolean {


                        val bitmap = (resource as? BitmapDrawable)?.bitmap
                        LogComponent.printD(
                            tag = "MainLoadsAdapter", message = "isFirstResource:${isFirstResource} width:${bitmap?.width} height:${bitmap?.height}"
                        )
                        return false
                    }

                }).centerInside().into(binding.coverIv.getCoverView())
//            binding.coverIv.loadImageWithInside(info.url)

        }
    }
}