package com.zasko.imageloads.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.zasko.imageloads.data.ImageInfo
import com.zasko.imageloads.databinding.ItemDetailImageBinding
import com.zasko.imageloads.utils.loadImageWithInside

class DetailImagesAdapter(private val loadMore: () -> Unit = {}) : RecyclerView.Adapter<DetailImagesAdapter.MHolder>() {

    private val data = ArrayList<ImageInfo>()

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

    override fun onBindViewHolder(holder: MHolder, position: Int) {
        if (position >= 0 && position < data.size) {
            holder.bind(info = data[position])
            if (position == data.size - 2) {
                loadMore.invoke()
            }
        }

    }

    inner class MHolder(private val binding: ItemDetailImageBinding) : ViewHolder(binding.root) {
        fun bind(info: ImageInfo) {
            binding.imageTv.getCoverView().loadImageWithInside(info.url)
        }
    }
}