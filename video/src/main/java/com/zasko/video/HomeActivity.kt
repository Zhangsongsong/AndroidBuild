package com.zasko.video

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.zasko.video.databinding.ActivityHomeBinding
import kotlin.random.Random

class HomeActivity : AppCompatActivity() {

    companion object {
        const val TAG = "HomeActivity"
    }


    private lateinit var viewBinding: ActivityHomeBinding

    private var isLoadMore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        initView()

        loadData()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            MMediaCodec.codecName(this)
        }

    }

    private var mAdapter: ViewPagerAdapter? = null

    private var fragments = ArrayList<VideoFragment>()

    private var currentPosition: Int = 0
    private fun initView() {

        mAdapter = ViewPagerAdapter(this)
        viewBinding.viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        viewBinding.viewPager.offscreenPageLimit = 1
        viewBinding.viewPager.adapter = mAdapter

        viewBinding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)

            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                Log.d(TAG, "onPageSelected: position:${position}")
                if (position != currentPosition) {
                    resetChildPlayer(currentPosition)
                }
                currentPosition = position

                if (position >= fragments.size - 3 && !isLoadMore) {
                    loadMore()
                }
            }
        })
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


    private fun loadMore() {
        isLoadMore = true
        viewBinding.viewPager.post {
            val tmp = ArrayList<VideoFragment>()
            val size = fragments.size
            for (index in 0..(Random.nextInt(10))) {
                tmp.add(VideoFragment().apply {
                    val bundle = Bundle()
                    bundle.putSerializable(VideoFragment.KEY_INFO, VideoFragmentData(index = index + size))
                    this.arguments = bundle
                })
            }
            fragments.addAll(tmp)
            mAdapter?.addMore(tmp)
            isLoadMore = false
        }
    }

    private fun resetChildPlayer(pos: Int) {
        fragments[pos].resetPlayer()
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