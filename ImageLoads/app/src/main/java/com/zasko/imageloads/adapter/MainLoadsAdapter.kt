package com.zasko.imageloads.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.MainLoadsInfo
import com.zasko.imageloads.databinding.ItemMainLoadsBinding
import com.zasko.imageloads.fragment.MainLoadFragment
import com.zasko.imageloads.utils.loadImageWithInside

class MainLoadsAdapter(private val loadMore: () -> Unit = {}) : RecyclerView.Adapter<ViewHolder>() {


    private val data = ArrayList<MainLoadsInfo>()

    fun setData(list: List<MainLoadsInfo>) {
        val size = list.size
        data.clear()
        data.addAll(list)
        notifyItemRangeChanged(0, size)
    }

    fun addData(list: List<MainLoadsInfo>) {
        val size = data.size
        data.addAll(list)
        notifyItemRangeInserted(size, list.size)
    }


    fun removeData() {
        val size = data.size
        data.clear()
        notifyItemRangeRemoved(0, size)
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

            if (position == data.size - 2) {
                LogComponent.printD(tag = "MainLoadsAdapter", message = "onBindViewHolder pos:${position} size:${data.size}")
                loadMore.invoke()
            }
        }
    }


    class MHolder(private val binding: ItemMainLoadsBinding) : ViewHolder(binding.root) {
        fun bind(info: MainLoadsInfo) {
            val param = binding.coverIv.layoutParams
            param.height = (MainLoadFragment.screenWidth / 2) * info.height / info.width
//            LogComponent.printD(tag = "MainLoadsAdapter", message = "bind height:${param.height}")
            binding.coverIv.layoutParams = param
            binding.coverIv.loadImageWithInside(info.url)
        }
    }
}