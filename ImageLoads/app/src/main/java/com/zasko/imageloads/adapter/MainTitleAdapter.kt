package com.zasko.imageloads.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.zasko.imageloads.R
import com.zasko.imageloads.databinding.ItemMainTitleBinding
import com.zasko.imageloads.utils.Constants
import com.zasko.imageloads.utils.onClick
import kotlin.random.Random

class MainTitleAdapter(private val itemClickListener: (ItemInfo) -> Unit) : ListAdapter<MainTitleAdapter.ItemInfo, ViewHolder>(diffUtil) {

    companion object {


        private val diffUtil = object : DiffUtil.ItemCallback<ItemInfo>() {
            override fun areItemsTheSame(oldItem: ItemInfo, newItem: ItemInfo): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ItemInfo, newItem: ItemInfo): Boolean {
                return oldItem == newItem
            }
        }
    }


    private var lastSelectIndex = 0


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return MHolder(binding = ItemMainTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder as MHolder).bind(getItem(position))
    }

    inner class MHolder(private val binding: ItemMainTitleBinding) : ViewHolder(binding.root) {
        private var currentInfo: ItemInfo? = null

        init {
            binding.titleTv.onClick {
                currentInfo?.let {
                    val pos = adapterPosition
                    if (lastSelectIndex == pos) {
                        return@let
                    }
                    getItem(lastSelectIndex).isSelect = false
                    notifyItemChanged(lastSelectIndex)
                    lastSelectIndex = pos
                    it.isSelect = true
                    notifyItemChanged(lastSelectIndex)
                    itemClickListener.invoke(it)
                }
            }
        }

        fun bind(info: ItemInfo) {
            currentInfo = info
            binding.titleTv.text = binding.titleTv.context.getString(info.id)
            binding.titleTv.setBackgroundColor(binding.titleTv.context.getColor(Constants.RANDOM_COLOR_ID[adapterPosition % itemCount]))
            binding.titleTv.setTextColor(binding.titleTv.context.getColor(if (info.isSelect) R.color.white else R.color.black))
        }

    }

    data class ItemInfo(var id: Int = 0, var isSelect: Boolean = false)
}