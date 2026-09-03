package com.zasko.imageloads.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
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

    private companion object {
        const val TYPE_IMAGE = 0
    }

    private val data = ArrayList<ImageInfo>()

    init {
        setHasStableIds(true)
    }

    fun setData(list: List<ImageInfo>) {
        val oldData = data.toList()
        val newData = list.toList()
        if (oldData == newData) {
            return
        }
        val diff = DiffUtil.calculateDiff(ImageInfoDiffCallback(oldData = oldData, newData = newData))
        data.clear()
        data.addAll(newData)
        diff.dispatchUpdatesTo(this)
    }

    fun addData(list: List<ImageInfo>) {
        val size = data.size
        data.addAll(list)
        notifyItemRangeInserted(size, list.size)
    }

    fun getData(): List<ImageInfo> {
        return data
    }

    fun configureRecyclerView(recyclerView: RecyclerView) {
        recyclerView.setItemViewCacheSize(12)
        recyclerView.recycledViewPool.setMaxRecycledViews(TYPE_IMAGE, 24)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MHolder {
        return MHolder(binding = ItemDetailImageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemViewType(position: Int): Int {
        return TYPE_IMAGE
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

    override fun onViewRecycled(holder: MHolder) {
        holder.clear()
        super.onViewRecycled(holder)
    }

    inner class MHolder(private val binding: ItemDetailImageBinding) : ViewHolder(binding.root) {
        private var layoutKey: ImageLayoutKey? = null
        private var loadedRequest: ImageRequestKey? = null

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
            val imageWidth = calculateImageWidth()
            val imageHeight = (imageWidth / displayRatio).roundToInt().coerceAtLeast(1)
            updateImageLayout(imageWidth = imageWidth, imageHeight = imageHeight)
            loadImage(url = info.url, imageWidth = imageWidth, imageHeight = imageHeight)
        }

        fun clear() {
            val coverView = binding.imageTv.getCoverView()
            Glide.with(coverView).clear(coverView)
            layoutKey = null
            loadedRequest = null
        }

        private fun calculateImageWidth(): Int {
            val recyclerView = binding.root.parent as? RecyclerView
            val contentWidth = recyclerView?.width
                ?.minus(recyclerView.paddingLeft + recyclerView.paddingRight)
                ?.takeIf { it > 0 }
                ?: binding.root.resources.displayMetrics.widthPixels
            val spanCount = (recyclerView?.layoutManager as? GridLayoutManager)
                ?.spanCount
                ?.coerceAtLeast(1)
                ?: 1
            return (contentWidth / spanCount).coerceAtLeast(1)
        }

        private fun updateImageLayout(imageWidth: Int, imageHeight: Int) {
            val newLayoutKey = ImageLayoutKey(width = imageWidth, height = imageHeight)
            if (layoutKey == newLayoutKey) {
                return
            }
            layoutKey = newLayoutKey
            (binding.imageTv.layoutParams as? ConstraintLayout.LayoutParams)?.let { params ->
                params.dimensionRatio = "h,$imageWidth:$imageHeight"
                binding.imageTv.layoutParams = params
            }
        }

        private fun loadImage(url: String, imageWidth: Int, imageHeight: Int) {
            val requestKey = ImageRequestKey(url = url, width = imageWidth, height = imageHeight)
            if (loadedRequest == requestKey) {
                return
            }
            loadedRequest = requestKey
            val coverView = binding.imageTv.getCoverView()
            Glide.with(coverView)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .dontAnimate()
                .override(imageWidth, imageHeight)
                .placeholder(R.mipmap.icon_pic)
                .centerInside()
                .into(coverView)
        }
    }

    private class ImageInfoDiffCallback(
        private val oldData: List<ImageInfo>,
        private val newData: List<ImageInfo>,
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldData.size

        override fun getNewListSize(): Int = newData.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return stableImageKey(oldData[oldItemPosition], oldItemPosition) == stableImageKey(newData[newItemPosition], newItemPosition)
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldData[oldItemPosition] == newData[newItemPosition]
        }
    }
}

private data class ImageLayoutKey(
    val width: Int,
    val height: Int,
)

private data class ImageRequestKey(
    val url: String,
    val width: Int,
    val height: Int,
)

private fun stableImageKey(info: ImageInfo, position: Int): String = "${info.url}:$position"
