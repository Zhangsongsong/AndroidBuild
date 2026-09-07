package com.zasko.imageloads.compose

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.zasko.imageloads.R
import com.zasko.imageloads.data.ImageLoadsInfo
import kotlin.math.roundToInt

class ImageListRecyclerAdapter(
    private var imageModelProvider: (ImageLoadsInfo) -> Any?,
    private var imageRatioProvider: (ImageLoadsInfo) -> Float,
    private var imageScaleType: ImageView.ScaleType,
    private var pageLabelProvider: (Int, ImageLoadsInfo) -> String?,
    private var imageKeyProvider: (ImageLoadsInfo) -> String,
    private var onLoadMore: () -> Unit,
    private var onImageClick: (ImageLoadsInfo) -> Unit,
    private var onFavoriteClick: (ImageLoadsInfo) -> Unit,
    private var onDownloadClick: (ImageLoadsInfo) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_EMPTY = 1
        const val TYPE_LABEL = 2
        const val TYPE_IMAGE = 3
        const val TYPE_LOADING = 4
        const val PAYLOAD_IMAGE_STATE = "image_state"
        const val FIXED_IMAGE_RATIO = 2f / 3f
    }

    private data class Entry(
        val type: Int,
        val stableKey: String,
        val image: ImageLoadsInfo? = null,
        val imageIndex: Int = -1,
        val label: String? = null,
        val ratio: Float = 2f / 3f,
        val title: String = "",
        val imageModel: Any? = null,
        val isSelectionMode: Boolean = false,
        val isSelected: Boolean = false,
        val showFavoriteAction: Boolean = false,
        val isFavorite: Boolean = false,
        val showDownloadAction: Boolean = false,
        val isItemDownloading: Boolean = false,
        val isItemDownloaded: Boolean = false,
        val downloadProgressText: String? = null,
    )

    private var entries: List<Entry> = emptyList()
    private var images: List<ImageLoadsInfo> = emptyList()
    private var topContent: (@Composable () -> Unit)? = null
    private var isRefreshing = false
    private var isLoadingMore = false
    private var loadMoreRequestedImageCount = -1

    init {
        setHasStableIds(true)
    }

    fun updateCallbacks(
        imageModelProvider: (ImageLoadsInfo) -> Any?,
        imageRatioProvider: (ImageLoadsInfo) -> Float,
        imageScaleType: ImageView.ScaleType,
        pageLabelProvider: (Int, ImageLoadsInfo) -> String?,
        imageKeyProvider: (ImageLoadsInfo) -> String,
        onLoadMore: () -> Unit,
        onImageClick: (ImageLoadsInfo) -> Unit,
        onFavoriteClick: (ImageLoadsInfo) -> Unit,
        onDownloadClick: (ImageLoadsInfo) -> Unit,
    ) {
        this.imageModelProvider = imageModelProvider
        this.imageRatioProvider = imageRatioProvider
        this.imageScaleType = imageScaleType
        this.pageLabelProvider = pageLabelProvider
        this.imageKeyProvider = imageKeyProvider
        this.onLoadMore = onLoadMore
        this.onImageClick = onImageClick
        this.onFavoriteClick = onFavoriteClick
        this.onDownloadClick = onDownloadClick
    }

    fun configureRecyclerView(recyclerView: RecyclerView) {
        recyclerView.setItemViewCacheSize(12)
        recyclerView.recycledViewPool.setMaxRecycledViews(TYPE_IMAGE, 24)
        recyclerView.recycledViewPool.setMaxRecycledViews(TYPE_LABEL, 8)
        recyclerView.recycledViewPool.setMaxRecycledViews(TYPE_LOADING, 2)
    }

    fun submit(
        images: List<ImageLoadsInfo>,
        isRefreshing: Boolean,
        isLoadingMore: Boolean,
        topContent: (@Composable () -> Unit)?,
        selectedImageKeys: Set<String>,
        isSelectionMode: Boolean,
        showFavoriteAction: Boolean,
        favoriteImageKeys: Set<String>,
        showItemDownloadAction: Boolean,
        downloadingImageKeys: Set<String>,
        downloadedImageKeys: Set<String>,
        itemDownloadProgressProvider: (ImageLoadsInfo) -> String?,
    ) {
        val oldEntries = entries
        val imagesChanged = this.images != images
        this.images = images.toList()
        this.isRefreshing = isRefreshing
        this.isLoadingMore = isLoadingMore
        this.topContent = topContent
        if (imagesChanged) {
            loadMoreRequestedImageCount = -1
        }

        val newEntries = buildList {
            if (topContent != null) {
                add(Entry(TYPE_HEADER, "header"))
            }
            if (images.isEmpty() && !isRefreshing) {
                add(Entry(TYPE_EMPTY, "empty"))
            }
            images.forEachIndexed { index, imageInfo ->
                pageLabelProvider(index, imageInfo)?.let { label ->
                    add(Entry(TYPE_LABEL, "label:$index:$label", label = label))
                }
                val key = imageKeyProvider(imageInfo)
                add(
                    Entry(
                        type = TYPE_IMAGE,
                        stableKey = "image:$key:$index",
                        image = imageInfo,
                        imageIndex = index,
                        ratio = FIXED_IMAGE_RATIO,
                        title = imageInfo.displayTitleForRecycler(),
                        imageModel = imageModelProvider(imageInfo),
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedImageKeys.contains(key),
                        showFavoriteAction = showFavoriteAction && !isSelectionMode,
                        isFavorite = favoriteImageKeys.contains(key),
                        showDownloadAction = showItemDownloadAction && !isSelectionMode,
                        isItemDownloading = downloadingImageKeys.contains(key),
                        isItemDownloaded = downloadedImageKeys.contains(key),
                        downloadProgressText = itemDownloadProgressProvider(imageInfo),
                    ),
                )
            }
            if (isLoadingMore) {
                add(Entry(TYPE_LOADING, "loading_more"))
            }
        }

        if (oldEntries == newEntries) {
            entries = newEntries
            if (entries.firstOrNull()?.type == TYPE_HEADER) {
                notifyItemChanged(0)
            }
            return
        }

        val diff = DiffUtil.calculateDiff(EntryDiffCallback(oldEntries = oldEntries, newEntries = newEntries))
        entries = newEntries
        diff.dispatchUpdatesTo(this)
        if (entries.firstOrNull()?.type == TYPE_HEADER) {
            notifyItemChanged(0)
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

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val oldEntry = oldEntries[oldItemPosition]
            val newEntry = newEntries[newItemPosition]
            return if (oldEntry.canBindStateOnly(newEntry)) {
                PAYLOAD_IMAGE_STATE
            } else {
                null
            }
        }

        private fun Entry.canBindStateOnly(other: Entry): Boolean {
            return type == TYPE_IMAGE &&
                other.type == TYPE_IMAGE &&
                image == other.image &&
                imageIndex == other.imageIndex &&
                ratio == other.ratio &&
                title == other.title &&
                imageModel == other.imageModel
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        val entry = entries[position]
        if (holder is ImageHolder && payloads.contains(PAYLOAD_IMAGE_STATE)) {
            holder.bindState(entry)
            maybeRequestLoadMore(position)
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val entry = entries[position]
        when (holder) {
            is HeaderHolder -> holder.bind(topContent)
            is EmptyHolder -> Unit
            is LabelHolder -> holder.bind(entry.label.orEmpty())
            is LoadingHolder -> Unit
            is ImageHolder -> {
                entry.image?.let {
                    holder.bind(entry)
                    maybeRequestLoadMore(position)
                }
            }
        }
    }

    fun isFullSpan(position: Int): Boolean {
        return entries.getOrNull(position)?.type != TYPE_IMAGE
    }

    fun isPageLabel(position: Int): Boolean {
        return entries.getOrNull(position)?.type == TYPE_LABEL
    }

    fun stickyPageLabelForPosition(position: Int): String? {
        if (position == RecyclerView.NO_POSITION || entries.isEmpty()) {
            return null
        }
        val start = position.coerceIn(0, entries.lastIndex)
        for (index in start downTo 0) {
            val entry = entries[index]
            if (entry.type == TYPE_LABEL) {
                return entry.label
            }
        }
        return null
    }

    fun imageCount(): Int = images.size

    fun maybeRequestLoadMore(position: Int) {
        val entry = entries.getOrNull(position) ?: return
        if (entry.type != TYPE_IMAGE || isRefreshing || isLoadingMore || images.size <= 5) {
            return
        }
        if (entry.imageIndex >= images.size - 3 && loadMoreRequestedImageCount != images.size) {
            loadMoreRequestedImageCount = images.size
            onLoadMore()
        }
    }

    override fun getItemCount(): Int = entries.size

    override fun getItemViewType(position: Int): Int = entries[position].type

    override fun getItemId(position: Int): Long {
        return entries[position].stableKey.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderHolder(
                ComposeView(parent.context).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                },
            )
            TYPE_EMPTY -> EmptyHolder(parent.context)
            TYPE_LABEL -> LabelHolder(parent.context)
            TYPE_LOADING -> LoadingHolder(parent.context)
            else -> ImageHolder(CoverItemView(parent.context))
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is ImageHolder) {
            holder.clear()
        }
        super.onViewRecycled(holder)
    }

    private class HeaderHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(content: (@Composable () -> Unit)?) {
            composeView.setContent {
                ImageLoadsTheme(applySystemBars = false) {
                    content?.invoke()
                }
            }
        }
    }

    private class EmptyHolder(context: android.content.Context) : RecyclerView.ViewHolder(
        FrameLayout(context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                240.dp(context.resources.displayMetrics.density),
            )
            addView(TextView(context).apply {
                text = "暂无图片"
                gravity = Gravity.CENTER
                setTextColor(Color.GRAY)
                textSize = 14f
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            })
        },
    )

    private class LabelHolder(context: android.content.Context) : RecyclerView.ViewHolder(
        FrameLayout(context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 8.dp(context.resources.displayMetrics.density)
                bottomMargin = 8.dp(context.resources.displayMetrics.density)
            }
            addView(TextView(context).apply {
                gravity = Gravity.CENTER
                setTextColor(Color.DKGRAY)
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(12.dp(context.resources.displayMetrics.density), 5.dp(context.resources.displayMetrics.density), 12.dp(context.resources.displayMetrics.density), 5.dp(context.resources.displayMetrics.density))
                background = GradientDrawable().apply {
                    cornerRadius = 8.dp(context.resources.displayMetrics.density).toFloat()
                    setColor(0xFFE7E0EC.toInt())
                }
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                )
            })
        },
    ) {
        fun bind(text: String) {
            (itemView as FrameLayout).getChildAt(0).let { (it as TextView).text = text }
        }
    }

    private class LoadingHolder(context: android.content.Context) : RecyclerView.ViewHolder(
        ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                4.dp(context.resources.displayMetrics.density),
            )
        },
    )

    private inner class ImageHolder(private val root: CoverItemView) : RecyclerView.ViewHolder(root) {
        private val imageView = ImageView(root.context)
        private val titleView = TextView(root.context)
        private val selectionShade = View(root.context)
        private val selectionIndicator = TextView(root.context)
        private val downloadButton = createButton(R.drawable.baseline_cloud_download_24)
        private val favoriteButton = createButton(R.drawable.baseline_favorite_border_24)
        private val progressOverlay = FrameLayout(root.context)
        private val progressText = TextView(root.context)
        private var model: Any? = null
        private var imageInfo: ImageLoadsInfo? = null
        private var ratio = 2f / 3f
        private var loadedRequest: ImageRequestKey? = null

        init {
            root.clipToOutline = true
            root.background = GradientDrawable().apply {
                cornerRadius = 4.dp(root.resources.displayMetrics.density).toFloat()
                setColor(0xFFE7E0EC.toInt())
            }
            imageView.scaleType = imageScaleType
            root.addView(imageView, FrameLayout.LayoutParams(-1, -1))
            titleView.apply {
                setTextColor(Color.WHITE)
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(8.dp(root.resources.displayMetrics.density), 7.dp(root.resources.displayMetrics.density), 8.dp(root.resources.displayMetrics.density), 7.dp(root.resources.displayMetrics.density))
                setBackgroundColor(0x3D000000)
            }
            root.addView(titleView, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
            selectionShade.setBackgroundColor(0x33000000)
            root.addView(selectionShade, FrameLayout.LayoutParams(-1, -1))
            selectionIndicator.apply {
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                }
            }
            root.addView(selectionIndicator, FrameLayout.LayoutParams(24.dp(root.resources.displayMetrics.density), 24.dp(root.resources.displayMetrics.density), Gravity.TOP or Gravity.END).apply {
                topMargin = 8.dp(root.resources.displayMetrics.density)
                rightMargin = 8.dp(root.resources.displayMetrics.density)
            })
            root.addView(downloadButton, FrameLayout.LayoutParams(34.dp(root.resources.displayMetrics.density), 34.dp(root.resources.displayMetrics.density), Gravity.TOP or Gravity.START).apply {
                topMargin = 8.dp(root.resources.displayMetrics.density)
                leftMargin = 8.dp(root.resources.displayMetrics.density)
            })
            root.addView(favoriteButton, FrameLayout.LayoutParams(34.dp(root.resources.displayMetrics.density), 34.dp(root.resources.displayMetrics.density), Gravity.TOP or Gravity.END).apply {
                topMargin = 8.dp(root.resources.displayMetrics.density)
                rightMargin = 8.dp(root.resources.displayMetrics.density)
            })
            progressOverlay.setBackgroundColor(0x55000000)
            progressText.apply {
                setTextColor(Color.WHITE)
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(12.dp(root.resources.displayMetrics.density), 6.dp(root.resources.displayMetrics.density), 12.dp(root.resources.displayMetrics.density), 6.dp(root.resources.displayMetrics.density))
                background = GradientDrawable().apply {
                    cornerRadius = 8.dp(root.resources.displayMetrics.density).toFloat()
                    setColor(0xCC202124.toInt())
                }
            }
            progressOverlay.addView(progressText, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
            root.addView(progressOverlay, FrameLayout.LayoutParams(-1, -1))
            selectionShade.isClickable = false
            root.setOnClickListener { imageInfo?.let { info -> if (!root.isDownloading) onImageClick(info) } }
            downloadButton.setOnClickListener { imageInfo?.let(onDownloadClick) }
            favoriteButton.setOnClickListener { imageInfo?.let(onFavoriteClick) }
            root.onSizeChangedCallback = { width, height ->
                if (width > 0 && height > 0) {
                    loadImage(width, height)
                }
            }
        }

        fun bind(entry: Entry) {
            imageInfo = entry.image
            model = entry.imageModel
            ratio = entry.ratio
            root.setRatio(ratio)
            if (imageView.scaleType != imageScaleType) {
                imageView.scaleType = imageScaleType
                loadedRequest = null
            }
            titleView.text = entry.title
            titleView.visibility = if (entry.title.isBlank()) View.GONE else View.VISIBLE
            bindState(entry)
            if (root.width > 0 && root.height > 0) {
                loadImage(root.width, root.height)
            }
        }

        fun bindState(entry: Entry) {
            imageInfo = entry.image
            root.isDownloading = entry.isItemDownloading
            selectionShade.visibility = if (entry.isSelectionMode && entry.isSelected) View.VISIBLE else View.GONE
            selectionIndicator.visibility = if (entry.isSelectionMode) View.VISIBLE else View.GONE
            (selectionIndicator.background as? GradientDrawable)?.setColor(
                if (entry.isSelected) 0xFF6750A4.toInt() else 0xCDE6E1E5.toInt(),
            )
            selectionIndicator.text = if (entry.isSelected) "✓" else ""
            downloadButton.visibility = if (entry.showDownloadAction && !entry.isItemDownloading) View.VISIBLE else View.GONE
            downloadButton.setColorFilter(if (entry.isItemDownloaded) 0xFF137333.toInt() else 0xFF49454F.toInt())
            favoriteButton.visibility = if (entry.showFavoriteAction) View.VISIBLE else View.GONE
            favoriteButton.setImageResource(if (entry.isFavorite) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24)
            favoriteButton.setColorFilter(if (entry.isFavorite) 0xFFE91E63.toInt() else 0xFF49454F.toInt())
            progressOverlay.visibility = if (entry.isItemDownloading) View.VISIBLE else View.GONE
            progressText.text = entry.downloadProgressText.orEmpty().ifBlank { "下载中" }
        }

        fun clear() {
            Glide.with(imageView).clear(imageView)
            loadedRequest = null
            imageInfo = null
            model = null
        }

        private fun loadImage(width: Int, height: Int) {
            val requestKey = ImageRequestKey(
                model = model,
                width = width,
                height = height,
                scaleType = imageScaleType,
            )
            if (loadedRequest == requestKey) {
                return
            }
            loadedRequest = requestKey
            Glide.with(imageView)
                .load(model)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .dontAnimate()
                .override(width.coerceAtLeast(1), height.coerceAtLeast(1))
                .placeholder(R.mipmap.icon_pic)
                .into(imageView)
        }

        private fun createButton(@androidx.annotation.DrawableRes icon: Int): ImageButton {
            return ImageButton(root.context).apply {
                setImageResource(icon)
                setPadding(7.dp(root.resources.displayMetrics.density), 7.dp(root.resources.displayMetrics.density), 7.dp(root.resources.displayMetrics.density), 7.dp(root.resources.displayMetrics.density))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xDDE6E1E5.toInt())
                }
                setColorFilter(0xFF49454F.toInt())
            }
        }
    }

    private data class ImageRequestKey(
        val model: Any?,
        val width: Int,
        val height: Int,
        val scaleType: ImageView.ScaleType,
    )

    private class CoverItemView(context: android.content.Context) : FrameLayout(context) {
        private var ratio = 2f / 3f
        var isDownloading: Boolean = false
        var onSizeChangedCallback: ((Int, Int) -> Unit)? = null

        fun setRatio(value: Float) {
            val normalized = value.coerceIn(0.2f, 5f)
            if (ratio != normalized) {
                ratio = normalized
                requestLayout()
            }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = when (MeasureSpec.getMode(widthMeasureSpec)) {
                MeasureSpec.EXACTLY, MeasureSpec.AT_MOST -> MeasureSpec.getSize(widthMeasureSpec)
                else -> suggestedMinimumWidth
            }.coerceAtLeast(1)
            val desiredHeight = (width / ratio).roundToInt().coerceAtLeast(1)
            val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
                MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
                MeasureSpec.AT_MOST -> desiredHeight.coerceAtMost(MeasureSpec.getSize(heightMeasureSpec))
                else -> desiredHeight
            }
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
            )
            setMeasuredDimension(width, height)
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w != oldw || h != oldh) {
                onSizeChangedCallback?.invoke(w, h)
            }
        }
    }
}

private fun Int.dp(density: Float): Int = (this * density).roundToInt()

private fun ImageLoadsInfo.displayTitleForRecycler(): String {
    return title.trim()
        .ifBlank {
            href.trim()
                .trimEnd('/')
                .substringAfterLast('/')
                .substringBefore('?')
                .replace(Regex("[-_]+"), " ")
                .trim()
        }
}
