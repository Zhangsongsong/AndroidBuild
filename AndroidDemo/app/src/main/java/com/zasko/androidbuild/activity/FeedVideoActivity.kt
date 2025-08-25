package com.zasko.androidbuild.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.zasko.androidbuild.BaseActivity
import com.zasko.androidbuild.components.LogComponent
import com.zasko.androidbuild.databinding.ActivityFeedVideoBinding
import com.zasko.androidbuild.databinding.ItemFeedVideoBinding
import com.zasko.androidbuild.utils.loadImage


import java.io.File
import java.io.IOException

class FeedVideoActivity : BaseActivity() {

    companion object {

        fun start(context: Context) {
            context.startActivity(Intent(context, FeedVideoActivity::class.java))
        }
    }

    private lateinit var binding: ActivityFeedVideoBinding

    private lateinit var mAdapter: FeedVideoAdapter
    private lateinit var pagerSnapHelper: PagerSnapHelper

    private val adapterPlayer = ExoPlayer.Builder(this).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAdapter = FeedVideoAdapter()
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

    }

    private fun findCenterHolder() {
        pagerSnapHelper.findSnapView(binding.recyclerView.layoutManager)?.let { view ->
            val holder = binding.recyclerView.getChildViewHolder(view)
            val pos = binding.recyclerView.getChildAdapterPosition(view)

        }
    }

    private fun preloadUrl() {
    }


    private class FeedVideoAdapter() : RecyclerView.Adapter<FeedVideoAdapter.FeedHolder>() {

        companion object {
            const val TAG = "FeedVideoAdapter"
        }

        private val data = ArrayList<FeedVideoItemInfo>()


        fun setData(list: List<FeedVideoItemInfo>) {
            data.clear()
            data.addAll(list)
            notifyDataSetChanged()
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
            holder.player = null
        }

        inner class FeedHolder(private val binding: ItemFeedVideoBinding) : ViewHolder(binding.root) {
            var player: ExoPlayer? = null
            fun bind(info: FeedVideoItemInfo) {
                binding.coverIv.loadImage(info.imgUrl)
                LogComponent.printD(tag = TAG, "bind pos:${bindingAdapterPosition}")
            }
        }

    }
}

data class FeedVideoItemInfo(var skitsId: Int, var partNo: Int = 0, var playUrl: String = "", var imgUrl: String = "")

@UnstableApi
object PlayerCache {
    private const val TAG = "PlayerCache"
    private var simpleCache: SimpleCache? = null

    fun init(context: Context, maxCacheSize: Long = 500L * 1024 * 1024) {
        val cacheDir = File(context.cacheDir, "exo_cache")

        LogComponent.printD(TAG, "init cacheDir:${cacheDir.absolutePath}")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val databaseProvider: DatabaseProvider = StandaloneDatabaseProvider(context)
        simpleCache = SimpleCache(
            cacheDir, LeastRecentlyUsedCacheEvictor(maxCacheSize), databaseProvider
        )
    }

    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val upstreamFactory = DefaultDataSource.Factory(context)
        return CacheDataSource.Factory().setCache(simpleCache!!).setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun clearCache() {
        simpleCache?.release()
        simpleCache = null
    }
}