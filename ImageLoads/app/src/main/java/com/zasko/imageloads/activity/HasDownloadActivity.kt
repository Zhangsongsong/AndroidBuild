package com.zasko.imageloads.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.data.HasDownloadInfo
import com.zasko.imageloads.databinding.ActivityHasDownloadBinding
import com.zasko.imageloads.databinding.ItemHasDownloadBinding
import com.zasko.imageloads.databinding.ItemImageBinding
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.loadImage
import com.zasko.imageloads.utils.switchThread
import io.reactivex.rxjava3.core.Single

class HasDownloadActivity : BaseActivity() {


    private lateinit var binding: ActivityHasDownloadBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHasDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getData()
    }

    private fun getData() {

        Single.just(true).map {
            val parentFile = FileUtil.getDownloadPath()

        }.switchThread().doOnSuccess {

        }.bindLife()
    }


    private class MAdapter : RecyclerView.Adapter<MAdapter.MHolder> {

        inner class MHolder(private val binding: ItemHasDownloadBinding) : ViewHolder(binding.root) {

            private var imageAdapter: ImagesAdapter = ImagesAdapter()

            init {
                binding.pictureRecycler.apply {
                    adapter = imageAdapter
                    layoutManager = LinearLayoutManager(context)
                }
            }

            fun bind(info: HasDownloadInfo) {
                binding.titleTv.text = info.name
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

        inner class MHolder(private val binding: ItemImageBinding) : ViewHolder(binding.root) {
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