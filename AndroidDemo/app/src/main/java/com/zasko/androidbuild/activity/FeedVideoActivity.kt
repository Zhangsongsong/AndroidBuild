package com.zasko.androidbuild.activity


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.material3.Text
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.zasko.androidbuild.BaseActivity
import com.zasko.androidbuild.components.LogComponent
import com.zasko.androidbuild.databinding.ActivityFeedVideoBinding
import com.zasko.androidbuild.databinding.ItemFeedVideoBinding
import com.zasko.androidbuild.services.KCHttpHelper
import com.zasko.androidbuild.services.NetworkBaseResponse
import com.zasko.androidbuild.services.VideoVerifyInfo
import com.zasko.androidbuild.services.VideoVerifyParams
import com.zasko.androidbuild.utils.loadImage
import com.zasko.androidbuild.utils.switchThread
import com.zasko.androidbuild.views.ExoPlayerView
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import java.io.File

class FeedVideoActivity : BaseActivity() {

    companion object {

        private const val TAG = "FeedVideoActivity"

        fun start(context: Context) {
            context.startActivity(Intent(context, FeedVideoActivity::class.java))
        }
    }

    private lateinit var binding: ActivityFeedVideoBinding

    private lateinit var mAdapter: FeedVideoAdapter
    private lateinit var pagerSnapHelper: PagerSnapHelper

    private var adapterPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapterPlayer = ExoPlayer.Builder(this).build()

        binding = ActivityFeedVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAdapter = FeedVideoAdapter(playError = { holder ->
            holder.currentInfo?.let { info ->
                KCHttpHelper.postVerify(params = VideoVerifyParams(skits_id = info.skitsId, part_no = info.partNo)).switchThread()
                    .doOnSuccess { result ->
                        result.data?.let { data ->
                            if (!TextUtils.isEmpty(data.resource_url)) {
                                info.playUrl = data.resource_url
                                setVideoCache(data.resource_url)
                                findCenterHolder()
                            }
                        }

                    }.bindLife()
            }
        })
        pagerSnapHelper = PagerSnapHelper()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FeedVideoActivity)
            adapter = mAdapter
            pagerSnapHelper.attachToRecyclerView(this)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    when (newState) {
                        RecyclerView.SCROLL_STATE_IDLE -> {
                            findCenterHolder()
                        }
                    }
                }
            })
        }
        val tmpList = mutableListOf<FeedVideoItemInfo>()
        repeat(10) {
            tmpList.add(
                FeedVideoItemInfo(
                    skitsId = 144, partNo = it + 1, imgUrl = "https://img.kcredshort.com/MTc1NTY2MjE0NDc3MyMxOTkjcG5n.png?imageMogr2/format/webp"
                )
            )
        }
        mAdapter.setData(tmpList)

        CoroutineScope(Dispatchers.Main).launch {
            delay(200)
            findCenterHolder()
        }.bindLife()
    }

    override fun onDestroy() {
        super.onDestroy()
        adapterPlayer?.release()
    }


    private var currentFeedHolder: FeedVideoAdapter.FeedHolder? = null


    private fun findCenterHolder() {
        pagerSnapHelper.findSnapView(binding.recyclerView.layoutManager)?.let { view ->
            val holder = binding.recyclerView.getChildViewHolder(view) as FeedVideoAdapter.FeedHolder
            val pos = binding.recyclerView.getChildAdapterPosition(view)

            fun handlePreload(holder: FeedVideoAdapter.FeedHolder) {
                LogComponent.printD(TAG, "findCenterHolder url:${holder.currentInfo?.playUrl}")
                if (!TextUtils.isEmpty(holder.currentInfo?.playUrl)) {
                    preloadUrl(pos = pos + 1)
                }
            }
            if (currentFeedHolder != holder) {
                currentFeedHolder?.releasePlayer()
                currentFeedHolder = holder
            }
            adapterPlayer?.let {
                holder.play(it) { isNew ->
                    handlePreload(holder)
                }
            }


        }
    }


    @SuppressLint("UnsafeOptInUsageError")
    private fun preloadUrl(pos: Int) {

        if (pos >= mAdapter.getData().size) {
            return
        }
        val info = mAdapter.getData()[pos]

        LogComponent.printD(TAG, "preloadUrl pos:${pos} url:${info.playUrl}")
        Single.just(pos).takeIf { pos < mAdapter.getData().size }?.flatMap {
            val info = mAdapter.getData()[pos]
            Single.just(info)
        }?.flatMap { info ->
            if (!TextUtils.isEmpty(info.playUrl)) {
                Single.never()
            } else {
                KCHttpHelper.postVerify(params = VideoVerifyParams(skits_id = info.skitsId, part_no = info.partNo))
            }

        }?.switchThread()?.doOnSuccess { result ->
            LogComponent.printD(TAG, "preloadUrl resul:${result} ThreadName:${Thread.currentThread().name}")
            result.data?.let { data ->
                mAdapter.getData()[pos].playUrl = data.resource_url
                setVideoCache(playUrl = data.resource_url)

            }
        }?.bindLife()
    }

    @SuppressLint("UnsafeOptInUsageError", "CheckResult")
    private fun setVideoCache(playUrl: String) {
        val dataSpec = DataSpec.Builder().setUri(playUrl).setLength(2 * 1024 * 1024).build()
        val upstream = DefaultDataSource.Factory(this)
        PlayerCache.getCache()?.let {
            val cacheDataSource =
                CacheDataSource.Factory().setCache(it).setUpstreamDataSourceFactory(upstream).setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                    .createDataSource()

            runOnUiThread {
                try {
                    CacheWriter(
                        cacheDataSource, dataSpec, ByteArray(8 * 1024)
                    ) { requestLength, bytesCached, newBytesCached ->
                        LogComponent.printD(TAG, "preloadUrl cached=$bytesCached / $requestLength")
                    }.cache()
                } catch (e: Exception) {

                }
            }
        }
    }


    private class FeedVideoAdapter(private val playError: (FeedHolder) -> Unit = {}) : RecyclerView.Adapter<FeedVideoAdapter.FeedHolder>() {

        companion object {
            const val TAG = "FeedVideoAdapter"
        }

        private val data = ArrayList<FeedVideoItemInfo>()


        fun setData(list: List<FeedVideoItemInfo>) {
            data.clear()
            data.addAll(list)
            notifyDataSetChanged()
        }

        fun getData(): List<FeedVideoItemInfo> {
            return data
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedHolder {
            return FeedHolder(binding = ItemFeedVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun getItemCount(): Int {
            return data.size
        }

        override fun onBindViewHolder(holder: FeedHolder, position: Int) {
            holder.bind(data[position])
        }

        override fun onViewRecycled(holder: FeedHolder) {
            super.onViewRecycled(holder)
            LogComponent.printD(TAG, "onViewRecycled pos:${holder.bindingAdapterPosition}")
        }

        override fun onViewAttachedToWindow(holder: FeedHolder) {
            super.onViewAttachedToWindow(holder)
            holder.showCover()
        }

        inner class FeedHolder(private val binding: ItemFeedVideoBinding) : ViewHolder(binding.root) {
            var currentInfo: FeedVideoItemInfo? = null

            private val playerListener = object : ExoPlayerView.PlayerListener {
                override fun onRenderedFirstFrame() {
                    super.onRenderedFirstFrame()
                    hideCover()
                }
            }

            init {
                binding.exoplayerView.setPlayerListener(playerListener)

            }

            fun bind(info: FeedVideoItemInfo) {
                currentInfo = info
                showCover()
                binding.coverIv.loadImage(info.imgUrl)
                binding.posTv.text = "$bindingAdapterPosition"
                LogComponent.printD(tag = TAG, "bind pos:${bindingAdapterPosition}")
            }


            @SuppressLint("UnsafeOptInUsageError")
            fun play(player: ExoPlayer, playSuccess: (isNew: Boolean) -> Unit = {}) {
                currentInfo?.let { info ->
                    if (TextUtils.isEmpty(info.playUrl)) {
                        playError.invoke(this)
                    } else {
                        var isNew = false
                        if (player != binding.exoplayerView.getPlayer()) {
                            binding.exoplayerView.setPlayer(player)
                            val mediaItem = MediaItem.fromUri(info.playUrl)
                            val mediaSource = ProgressiveMediaSource.Factory(PlayerCache.getCacheDataSourceFactory(binding.exoplayerView.context))
                                .createMediaSource(mediaItem)
                            player.setMediaSource(mediaSource)
                            player.prepare()
                            isNew = true
                        }
                        player.play()
                        playSuccess.invoke(isNew)

                    }
                }
            }

            fun setPlayer(player: ExoPlayer) {
                binding.exoplayerView.setPlayer(player)
            }


            fun hideCover() {
                binding.coverIv.animate().alpha(0f).setDuration(200).start()
            }

            fun showCover() {
                binding.coverIv.alpha = 1.0f
            }


            fun releasePlayer() {
                binding.coverIv.alpha = 1.0f
                binding.exoplayerView.getPlayer()?.stop()
                binding.exoplayerView.unBindBindPlayer()
            }


        }

    }
}

data class FeedVideoItemInfo(var skitsId: Int, var partNo: Int = 0, var playUrl: String = "", var imgUrl: String = "")

object PlayerCache {
    private const val TAG = "PlayerCache"

    @SuppressLint("UnsafeOptInUsageError")
    private var simpleCache: SimpleCache? = null

    @SuppressLint("UnsafeOptInUsageError")
    fun init(context: Context, maxCacheSize: Long = 500L * 1024 * 1024) {
        val cacheDir = File(context.cacheDir, "exo_cache")

        LogComponent.printD(TAG, "init cacheDir:${cacheDir.absolutePath}")
        if (!cacheDir.exists()) cacheDir.mkdirs()


        val databaseProvider: DatabaseProvider = StandaloneDatabaseProvider(context)

        simpleCache = SimpleCache(
            cacheDir, LeastRecentlyUsedCacheEvictor(maxCacheSize), databaseProvider
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val upstreamFactory = DefaultDataSource.Factory(context)
        return CacheDataSource.Factory().setCache(simpleCache!!).setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }


    @SuppressLint("UnsafeOptInUsageError")
    fun getCache(): SimpleCache? {
        return simpleCache
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun clearCache() {
        simpleCache?.release()
        simpleCache = null
    }
}