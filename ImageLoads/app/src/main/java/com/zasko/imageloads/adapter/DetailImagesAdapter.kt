package com.zasko.imageloads.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.zasko.imageloads.R
import com.zasko.imageloads.data.ImageInfo
import com.zasko.imageloads.databinding.ItemDetailImageBinding
import kotlin.math.roundToInt

class DetailImagesAdapter(
    private val loadMore: () -> Unit = {},
    private val itemClick: (ImageInfo, Int) -> Unit = { _, _ -> },
) : RecyclerView.Adapter<DetailImagesAdapter.MHolder>() {

    private val data = ArrayList<ImageInfo>()

    init {
        setHasStableIds(true)
    }

    fun setData(list: List<ImageInfo>) {
        val size = data.size
        data.clear()
        notifyItemRangeRemoved(0, size)
        data.addAll(list)
        notifyItemRangeInserted(0, data.size)
    }

    fun addData(list: List<ImageInfo>) {
        val size = data.size
        data.addAll(list)
        notifyItemRangeInserted(size, list.size)
    }

    fun getData(): List<ImageInfo> {
        return data
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MHolder {
        return MHolder(binding = ItemDetailImageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemId(position: Int): Long {
        val info = data[position]
        return (info.url.hashCode().toLong() shl 32) xor position.toLong()
    }

    override fun onBindViewHolder(holder: MHolder, position: Int) {
        if (position >= 0 && position < data.size) {
            holder.bind(info = data[position])
            if (data.size > 1 && position >= data.size - 2) {
                loadMore.invoke()
            }
        }

    }

    inner class MHolder(private val binding: ItemDetailImageBinding) : ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && position < data.size) {
                    itemClick(data[position], position)
                }
            }
        }

        fun bind(info: ImageInfo) {
            val displayRatio = if (info.width > 0 && info.height > 0) {
                (info.width.toFloat() / info.height.toFloat()).coerceIn(0.3f, 2.5f)
            } else {
                2f / 3f
            }
            val imageWidth = binding.root.resources.displayMetrics.widthPixels
            val imageHeight = (imageWidth / displayRatio).roundToInt()
            (binding.imageTv.layoutParams as? ConstraintLayout.LayoutParams)?.let { params ->
                params.dimensionRatio = "h,${imageWidth}:${imageHeight}"
                binding.imageTv.layoutParams = params
            }
            Glide.with(binding.imageTv.getCoverView())
                .load(info.url)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .dontAnimate()
                .override(imageWidth, imageHeight)
                .placeholder(R.mipmap.icon_pic)
                .centerInside()
                .into(binding.imageTv.getCoverView())
        }
    }
}
