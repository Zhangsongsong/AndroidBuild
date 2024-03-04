package com.zasko.video

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.zasko.video.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {


    private lateinit var viewBinding: ActivityHomeBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        initView()

        loadData()
    }

    private var mAdapter: ViewPagerAdapter? = null

    private var fragments = ArrayList<VideoFragment>()
    private fun initView() {

        mAdapter = ViewPagerAdapter(this)
        viewBinding.viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        viewBinding.viewPager.offscreenPageLimit = 1
        viewBinding.viewPager.adapter = mAdapter
    }

    private fun loadData() {

        for (index in 0..6) {
            fragments.add(VideoFragment().apply {
                val bundle = Bundle()
                bundle.putSerializable(VideoFragment.KEY_INFO, VideoFragmentData(index = index))
                this.arguments = bundle
            })
        }
        mAdapter?.setData(fragments)

    }
}


class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private val data = ArrayList<VideoFragment>()

    fun setData(list: List<VideoFragment>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    fun addMore(list: List<VideoFragment>) {
        val size = data.size
        data.addAll(list)
        notifyItemInserted(size)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun createFragment(position: Int): Fragment {
        return data[position]
    }

}