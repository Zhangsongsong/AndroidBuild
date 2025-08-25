package com.zasko.androidbuild.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.zasko.androidbuild.BaseActivity
import com.zasko.androidbuild.components.LogComponent
import com.zasko.androidbuild.databinding.ActivityFeedVideoBinding
import com.zasko.androidbuild.databinding.ItemFeedVideoBinding
import com.zasko.androidbuild.services.KCHttpHelper
import com.zasko.androidbuild.services.VideoVerifyParams
import com.zasko.androidbuild.utils.loadImage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FeedVideoActivity : BaseActivity() {

    companion object {

        fun start(context: Context) {
            context.startActivity(Intent(context, FeedVideoActivity::class.java))
        }
    }

    private lateinit var binding: ActivityFeedVideoBinding

    private lateinit var mAdapter: FeedVideoAdapter
    private lateinit var pagerSnapHelper: PagerSnapHelper

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


    private class FeedVideoAdapter : RecyclerView.Adapter<FeedVideoAdapter.FeedHolder>() {

        private val data = ArrayList<FeedVideoItemInfo>()

        fun setData(list: List<FeedVideoItemInfo>) {
            data.clear()
            data.addAll(list)
            notifyDataSetChanged()
        }

        inner class FeedHolder(private val binding: ItemFeedVideoBinding) : ViewHolder(binding.root) {
            fun bind(info: FeedVideoItemInfo) {
                binding.coverIv.loadImage(info.imgUrl)
            }
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
    }
}

data class FeedVideoItemInfo(var skitsId: Int, var partNo: Int = 0, var playUrl: String = "", var imgUrl: String = "")