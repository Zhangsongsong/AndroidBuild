package com.zasko.imageloads.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.zasko.imageloads.MainActivity
import com.zasko.imageloads.fragment.ThemePagerFragment

class MainViewPagerAdapter(act: MainActivity) : FragmentStateAdapter(act) {

    private val data = ArrayList<ThemePagerFragment>()


    fun setData(list: List<ThemePagerFragment>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun createFragment(position: Int): Fragment {
        return data[position]
    }
}