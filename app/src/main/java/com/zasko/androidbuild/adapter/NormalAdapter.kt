package com.zasko.androidbuild.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zasko.androidbuild.databinding.ItemHomeBinding
import com.zasko.androidbuild.utils.onClick

class NormalAdapter(private val listener: (Int, ItemInfo) -> Unit = { _, _ ->

}) : RecyclerView.Adapter<NormalAdapter.MHolder>() {

    private val data = ArrayList<ItemInfo>()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<ItemInfo>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MHolder {
        return MHolder(ItemHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: MHolder, position: Int) {
        holder.bind(data[position])
    }

    inner class MHolder(private val itemBinding: ItemHomeBinding) : RecyclerView.ViewHolder(itemBinding.root) {

        private var bean: ItemInfo? = null

        init {
            itemBinding.constraint.onClick {
                bean?.let {
                    listener.invoke(adapterPosition, it)
                }
            }
        }

        fun bind(info: ItemInfo) {
            bean = info
            itemBinding.titleTv.text = info.title
        }
    }

    data class ItemInfo(var id: Int = 0, var title: String = "")

}

