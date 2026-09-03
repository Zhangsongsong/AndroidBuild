package com.zasko.imageloads.ui.common

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
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

    private data class Entry(
        val type: Int,
        val stableKey: String,
        val image: ImageLoadsInfo? = null,
        val imageIndex: Int = -1,
        val title: String = "",
        val subtitles: List<String> = emptyList(),
    )

    private var detailInfo = CommonImageDetailInfo()
    private var isLoadingMore = false
    private var isLoadMoreEnabled = false
    private var entries: List<Entry> = buildEntries(detailInfo, isLoadingMore)

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
        val oldEntries = entries
        val newEntries = buildEntries(detailInfo, isLoadingMore)
        this.detailInfo = detailInfo
        this.isLoadingMore = isLoadingMore
        this.isLoadMoreEnabled = isLoadMoreEnabled

        if (oldEntries == newEntries) {
            return
        }
        val diff = DiffUtil.calculateDiff(EntryDiffCallback(oldEntries = oldEntries, newEntries = newEntries))
        entries = newEntries
        diff.dispatchUpdatesTo(this)
    }

    fun isFullSpan(position: Int): Boolean {
        return getItemViewType(position) != TYPE_IMAGE
    }

    fun pictureCount(): Int = detailInfo.pictures.size

    fun configureRecyclerView(recyclerView: RecyclerView) {
        val cacheSize = (imageColumnCount * 6).coerceIn(8, 20)
        recyclerView.setItemViewCacheSize(cacheSize)
        recyclerView.recycledViewPool.setMaxRecycledViews(TYPE_IMAGE, cacheSize * 2)
        recyclerView.recycledViewPool.setMaxRecycledViews(TYPE_HEADER, 2)
        recyclerView.recycledViewPool.setMaxRecycledViews(TYPE_LOADING, 2)
    }

    override fun getItemCount(): Int {
        return entries.size
    }

    override fun getItemViewType(position: Int): Int {
        return entries[position].type
    }

    override fun getItemId(position: Int): Long {
        return entries[position].stableKey.hashCode().toLong()
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
        val entry = entries[position]
        when (holder) {
            is HeaderHolder -> holder.bind(detailInfo)
            is ImageHolder -> {
                val image = entry.image ?: return
                holder.bind(image)
                if (isLoadMoreEnabled && detailInfo.pictures.size > 2 && entry.imageIndex >= detailInfo.pictures.size - 2) {
                    onLoadMore()
                }
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is ImageHolder) {
            holder.clear()
        }
        super.onViewRecycled(holder)
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
        private var layoutKey: ImageLayoutKey? = null
        private var loadedRequest: ImageRequestKey? = null

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val index = position - 1
                    detailInfo.pictures.getOrNull(index)?.let { onImageClick(it, index) }
                }
            }
        }

        fun bind(imageInfo: ImageLoadsInfo) {
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
            updateImageLayout(imageWidth = imageWidth, imageHeight = imageHeight)
            loadImage(
                imageInfo = imageInfo,
                imageModel = imageModelProvider(imageInfo),
                imageWidth = imageWidth,
                imageHeight = imageHeight,
            )
        }

        fun clear() {
            val coverView = binding.imageTv.getCoverView()
            Glide.with(coverView).clear(coverView)
            layoutKey = null
            loadedRequest = null
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

        private fun loadImage(
            imageInfo: ImageLoadsInfo,
            imageModel: Any?,
            imageWidth: Int,
            imageHeight: Int,
        ) {
            val requestKey = ImageRequestKey(
                url = imageInfo.url,
                model = imageModel,
                width = imageWidth,
                height = imageHeight,
            )
            if (loadedRequest == requestKey) {
                return
            }
            loadedRequest = requestKey
            val coverView = binding.imageTv.getCoverView()
            Glide.with(coverView)
                .load(imageModel)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .dontAnimate()
                .override(imageWidth, imageHeight)
                .placeholder(R.mipmap.icon_pic)
                .centerInside()
                .into(coverView)
        }
    }

    private class LoadingHolder(view: View) : RecyclerView.ViewHolder(view)

    private fun buildEntries(detailInfo: CommonImageDetailInfo, isLoadingMore: Boolean): List<Entry> {
        return buildList {
            add(
                Entry(
                    type = TYPE_HEADER,
                    stableKey = "header",
                    title = detailInfo.title,
                    subtitles = detailInfo.subtitles,
                ),
            )
            detailInfo.pictures.forEachIndexed { index, imageInfo ->
                add(
                    Entry(
                        type = TYPE_IMAGE,
                        stableKey = "image:${imageInfo.url}:$index",
                        image = imageInfo,
                        imageIndex = index,
                    ),
                )
            }
            if (isLoadingMore) {
                add(Entry(type = TYPE_LOADING, stableKey = "loading"))
            }
        }
    }

    private class EntryDiffCallback(
        private val oldEntries: List<Entry>,
        private val newEntries: List<Entry>,
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldEntries.size

        override fun getNewListSize(): Int = newEntries.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldEntries[oldItemPosition].stableKey == newEntries[newItemPosition].stableKey
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldEntries[oldItemPosition] == newEntries[newItemPosition]
        }
    }

}

private data class ImageLayoutKey(
    val width: Int,
    val height: Int,
)

private data class ImageRequestKey(
    val url: String,
    val model: Any?,
    val width: Int,
    val height: Int,
)

private fun Int.dp(density: Float): Int = (this * density).roundToInt()

private fun ImageLoadsInfo.displayRatio(): Float {
    return if (width > 0 && height > 0) {
        (width.toFloat() / height.toFloat()).coerceIn(0.3f, 2.5f)
    } else {
        2f / 3f
    }
}
