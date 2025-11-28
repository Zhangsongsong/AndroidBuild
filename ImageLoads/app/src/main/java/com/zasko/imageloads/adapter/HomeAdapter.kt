package com.zasko.imageloads.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.zasko.imageloads.activity.PersonListActivity
import com.zasko.imageloads.data.DataUseFrom
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.databinding.ItemMainThemeSelectBinding
import com.zasko.imageloads.ui.xiuren.XiuRenHasDownloadActivity
import com.zasko.imageloads.utils.loadImage
import com.zasko.imageloads.utils.onClick

class HomeAdapter(private val itemClickListener: (View, MainThemeSelectInfo) -> Unit) : RecyclerView.Adapter<HomeAdapter.MHolder>() {

    private val data = ArrayList<MainThemeSelectInfo>()

    fun setData(list: List<MainThemeSelectInfo>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MHolder {
        return MHolder(ItemMainThemeSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: MHolder, position: Int) {
        return holder.bind(info = data[position])
    }

    inner class MHolder(private val binding: ItemMainThemeSelectBinding) : ViewHolder(binding.root) {
        private var currentInfo: MainThemeSelectInfo? = null

        init {
            binding.coverCardView.onClick { view ->
                currentInfo?.let { info -> itemClickListener(view, info) }

            }
            binding.useDataFromSwitch.setOnCheckedChangeListener { _, isChecked ->
                currentInfo?.dataUseFrom = if (isChecked) DataUseFrom.PRIVATE_FILE.value else DataUseFrom.NETWORK.value
            }
            binding.userDataFromCons.onClick {
                binding.useDataFromSwitch.isChecked = !binding.useDataFromSwitch.isChecked
            }
            binding.hasDownloadTv.onClick { view ->
                currentInfo?.let { info -> itemClickListener(view, info) }
            }
        }

        fun bind(info: MainThemeSelectInfo) {
            currentInfo = info
            binding.titleTv.text = info.title
            binding.coverIv.loadImage(info.cover)
            binding.useDataFromSwitch.isChecked = info.dataUseFrom == DataUseFrom.PRIVATE_FILE.value
        }
    }
}