package com.zasko.imageloads.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.databinding.ItemMainTitleBinding
import com.zasko.imageloads.utils.Constants
import com.zasko.imageloads.viewmodel.MainViewModel
import kotlin.random.Random

class MainTitleAdapter : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return MHolder(binding = ItemMainTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return MainViewModel.DRAWER_ITEMS.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder as MHolder).bind(MainViewModel.DRAWER_ITEMS[position])
    }

    inner class MHolder(private val binding: ItemMainTitleBinding) : ViewHolder(binding.root) {
        fun bind(id: Int) {
            binding.titleTv.text = binding.titleTv.context.getString(id)
            val next = Random.nextInt(until = Constants.RANDOM_COLOR_ID.size)
            binding.titleTv.setBackgroundColor(binding.titleTv.context.getColor(Constants.RANDOM_COLOR_ID[next]))
        }
    }
}