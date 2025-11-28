package com.zasko.imageloads.ui.xiuren

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.data.HasDownloadInfo
import com.zasko.imageloads.databinding.ActivityHasDownloadBinding
import com.zasko.imageloads.databinding.ItemHasDownloadBinding
import com.zasko.imageloads.databinding.ItemImageBinding
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.loadImage
import com.zasko.imageloads.utils.switchThread
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File

class HasDownloadActivity : BaseActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, HasDownloadActivity::class.java))
        }
    }


    private lateinit var binding: ActivityHasDownloadBinding
    private var mAdapter: MAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHasDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MAdapter().apply {
                mAdapter = this
            }
            itemAnimator = null
        }
        getData()
    }

    private fun getData() {

        Single.just(true).subscribeOn(Schedulers.io()).map {
            val parentFile = File(FileUtil.getDownloadPath() + "/${FileUtil.PICTURE_XIUREN}")
            if (!parentFile.exists() || !parentFile.isDirectory) {
                return@map emptyList()
            }
            parentFile.listFiles()?.map { file ->
                val imageList = file.listFiles()?.map { it.absolutePath }?.take(3) ?: emptyList()
                HasDownloadInfo(name = file.name, path = file.absolutePath, images = imageList)
            } ?: emptyList()
        }.switchThread().doOnSuccess { list ->
            mAdapter?.setData(list = list)
        }.bindLife()
    }


    private class MAdapter : RecyclerView.Adapter<MAdapter.MHolder>() {

        private val data = ArrayList<HasDownloadInfo>()
        fun setData(list: List<HasDownloadInfo>) {
            data.clear()
            data.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup, viewType: Int
        ): MHolder {
            return MHolder(
                binding = ItemHasDownloadBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }

        override fun onBindViewHolder(holder: MHolder, position: Int) {
            holder.bind(data[position])
        }

        override fun getItemCount(): Int {
            return data.size
        }

        inner class MHolder(private val binding: ItemHasDownloadBinding) : RecyclerView.ViewHolder(binding.root) {

            private var imageAdapter: ImagesAdapter = ImagesAdapter()

            init {
                binding.pictureRecycler.apply {
                    adapter = imageAdapter
                    layoutManager = GridLayoutManager(context, 3)
                }
            }

            fun bind(info: HasDownloadInfo) {
                binding.titleTv.text = info.name
                imageAdapter.setData(info.images ?: emptyList())
            }
        }
    }

    private class ImagesAdapter() : RecyclerView.Adapter<ImagesAdapter.MHolder>() {

        private val data = ArrayList<String>()

        fun setData(list: List<String>) {
            data.clear()
            data.addAll(list)
            notifyDataSetChanged()
        }

        inner class MHolder(private val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(path: String) {
                binding.imageTv.loadImage(path)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MHolder {
            return MHolder(binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun getItemCount(): Int {
            return data.size
        }

        override fun onBindViewHolder(holder: MHolder, position: Int) {
            holder.bind(path = data[position])
        }

    }


}