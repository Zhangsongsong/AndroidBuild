package com.zasko.imageloads.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.zasko.imageloads.data.MainLoadsInfo
import com.zasko.imageloads.databinding.ItemMainLoadsBinding
import com.zasko.imageloads.utils.loadImageWithInside

class MainLoadsAdapter : RecyclerView.Adapter<ViewHolder>() {

    private val data = ArrayList<MainLoadsInfo>()

    fun setData(list: List<MainLoadsInfo>) {
        val size = list.size
        data.clear()
        data.addAll(list)
        notifyItemRangeChanged(0, size)
    }

    fun addData(info: MainLoadsInfo) {
        val size = data.size
        data.add(info)
        notifyItemInserted(size)
    }

    fun getDataSize(): Int {
        return data.size
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return MHolder(binding = ItemMainLoadsBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position >= 0 && position < data.size) {
            (holder as MHolder).bind(data[position])
        }
    }


    class MHolder(private val binding: ItemMainLoadsBinding) : ViewHolder(binding.root) {
        fun bind(info: MainLoadsInfo) {
            binding.coverIv.loadImageWithInside(info.data)
        }
    }
}