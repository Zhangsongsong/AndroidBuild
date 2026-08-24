package com.zasko.imageloads.ui.common

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.zasko.imageloads.R
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.databinding.ItemDetailImageBinding
import kotlin.math.roundToInt

class CommonDetailRecyclerAdapter(
    private val imageColumnCount: Int,
    private var imageModelProvider: (ImageLoadsInfo) -> Any?,
    private var onLoadMore: () -> Unit,
    private var onImageClick: (ImageLoadsInfo, Int) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_IMAGE = 1
        const val TYPE_LOADING = 2
        const val TAG_TITLE = "detail_title"
        const val TAG_SUBTITLES = "detail_subtitles"
    }

    private var detailInfo = CommonImageDetailInfo()
    private var isLoadingMore = false
    private var isLoadMoreEnabled = false

    init {
        setHasStableIds(true)
    }

    fun updateCallbacks(
        imageModelProvider: (ImageLoadsInfo) -> Any?,
        onLoadMore: () -> Unit,
        onImageClick: (ImageLoadsInfo, Int) -> Unit,
    ) {
        this.imageModelProvider = imageModelProvider
        this.onLoadMore = onLoadMore
        this.onImageClick = onImageClick
    }

    fun submit(
        detailInfo: CommonImageDetailInfo,
        isLoadingMore: Boolean,
        isLoadMoreEnabled: Boolean = false,
    ) {
        val oldInfo = this.detailInfo
        val oldLoading = this.isLoadingMore
        val oldLoadMoreEnabled = this.isLoadMoreEnabled
        this.detailInfo = detailInfo
        this.isLoadingMore = isLoadingMore
        this.isLoadMoreEnabled = isLoadMoreEnabled

        if (oldInfo == detailInfo && oldLoading == isLoadingMore && oldLoadMoreEnabled == isLoadMoreEnabled) {
            return
        }
        if (oldLoading == isLoadingMore && oldLoadMoreEnabled == isLoadMoreEnabled && oldInfo.pictures.isNotEmpty() &&
            detailInfo.pictures.size >= oldInfo.pictures.size &&
            detailInfo.pictures.subList(0, oldInfo.pictures.size) == oldInfo.pictures
        ) {
            val addedCount = detailInfo.pictures.size - oldInfo.pictures.size
            if (addedCount > 0) {
                notifyItemRangeInserted(1 + oldInfo.pictures.size, addedCount)
            }
            if (addedCount == 0 && oldLoading == isLoadingMore) {
                notifyItemChanged(0)
            }
        } else {
            notifyDataSetChanged()
        }
    }

    fun isFullSpan(position: Int): Boolean {
        return getItemViewType(position) != TYPE_IMAGE
    }

    fun pictureCount(): Int = detailInfo.pictures.size

    override fun getItemCount(): Int {
        return 1 + detailInfo.pictures.size + if (isLoadingMore) 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 -> TYPE_HEADER
            position <= detailInfo.pictures.lastIndex + 1 -> TYPE_IMAGE
            else -> TYPE_LOADING
        }
    }

    override fun getItemId(position: Int): Long {
        return when (getItemViewType(position)) {
            TYPE_HEADER -> Long.MIN_VALUE
            TYPE_IMAGE -> {
                val image = detailInfo.pictures[position - 1]
                (image.url.hashCode().toLong() shl 32) xor (position - 1).toLong()
            }
            else -> Long.MAX_VALUE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderHolder(createHeaderView(parent))
            TYPE_IMAGE -> ImageHolder(
                ItemDetailImageBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            )
            else -> LoadingHolder(
                ProgressBar(parent.context).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        56.dp(parent.resources.displayMetrics.density),
                    )
                },
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderHolder -> holder.bind(detailInfo)
            is ImageHolder -> {
                val imageIndex = position - 1
                holder.bind(detailInfo.pictures[imageIndex], imageIndex)
                if (isLoadMoreEnabled && detailInfo.pictures.size > 2 && imageIndex >= detailInfo.pictures.size - 2) {
                    onLoadMore()
                }
            }
        }
    }

    private fun createHeaderView(parent: ViewGroup): LinearLayout {
        val context = parent.context
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(6.dp(context.resources.displayMetrics.density), 8.dp(context.resources.displayMetrics.density), 6.dp(context.resources.displayMetrics.density), 8.dp(context.resources.displayMetrics.density))
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            addView(TextView(context).apply {
                tag = TAG_TITLE
                setTextColor(Color.DKGRAY)
                textSize = 18f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                tag = TAG_SUBTITLES
            })
        }
    }

    private class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(detailInfo: CommonImageDetailInfo) {
            val root = itemView as LinearLayout
            root.findViewWithTag<TextView>(TAG_TITLE).text = detailInfo.title
            val subtitles = root.findViewWithTag<LinearLayout>(TAG_SUBTITLES)
            subtitles.removeAllViews()
            detailInfo.subtitles.filter { it.isNotBlank() }.forEach { text ->
                subtitles.addView(TextView(root.context).apply {
                    setTextColor(Color.GRAY)
                    textSize = 13f
                    this.text = text
                })
            }
        }
    }

    private inner class ImageHolder(
        private val binding: ItemDetailImageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val index = position - 1
                    detailInfo.pictures.getOrNull(index)?.let { onImageClick(it, index) }
                }
            }
        }

        fun bind(imageInfo: ImageLoadsInfo, index: Int) {
            val ratio = imageInfo.displayRatio()
            val recyclerView = binding.root.parent as? RecyclerView
            val contentWidth = recyclerView?.width
                ?.minus(recyclerView.paddingLeft + recyclerView.paddingRight)
                ?.takeIf { it > 0 }
                ?: binding.root.resources.displayMetrics.widthPixels
            val spacing = if (imageColumnCount > 1) {
                8.dp(binding.root.resources.displayMetrics.density) * (imageColumnCount - 1)
            } else {
                0
            }
            val imageWidth = ((contentWidth - spacing) / imageColumnCount).coerceAtLeast(1)
            val imageHeight = (imageWidth / ratio).roundToInt().coerceAtLeast(1)
            (binding.imageTv.layoutParams as? ConstraintLayout.LayoutParams)?.let { params ->
                params.dimensionRatio = "h,${imageWidth}:${imageHeight}"
                binding.imageTv.layoutParams = params
            }
            Glide.with(binding.imageTv.getCoverView())
                .load(imageModelProvider(imageInfo))
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .dontAnimate()
                .override(imageWidth, imageHeight)
                .placeholder(R.mipmap.icon_pic)
                .centerInside()
                .into(binding.imageTv.getCoverView())
        }
    }

    private class LoadingHolder(view: View) : RecyclerView.ViewHolder(view)

}

private fun Int.dp(density: Float): Int = (this * density).roundToInt()

private fun ImageLoadsInfo.displayRatio(): Float {
    return if (width > 0 && height > 0) {
        (width.toFloat() / height.toFloat()).coerceIn(0.3f, 2.5f)
    } else {
        2f / 3f
    }
}
