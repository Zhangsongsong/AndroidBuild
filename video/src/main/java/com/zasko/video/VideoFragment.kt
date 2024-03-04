package com.zasko.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.zasko.video.databinding.FragmentVideoBinding
import java.io.Serializable
import kotlin.random.Random

class VideoFragment : Fragment() {
    companion object {
        const val TAG = "VideoFragment"

        const val KEY_INFO = "key_info"

        val COLORS = arrayOf(R.color.purple_200, R.color.purple_500, R.color.purple_700)
    }

    private lateinit var viewBinding: FragmentVideoBinding


    private lateinit var tranData: VideoFragmentData
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tranData = (arguments?.getSerializable(KEY_INFO) as? VideoFragmentData)!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewBinding = FragmentVideoBinding.inflate(inflater)
        initView()
        return viewBinding.root
    }

    private fun initView() {
        viewBinding.indexTv.text = "${tranData.index}"
        viewBinding.videoCons.setBackgroundColor(ContextCompat.getColor(viewBinding.videoCons.context, COLORS[Random.nextInt(COLORS.size)]))
    }

}

data class VideoFragmentData(var index: Int = 0) : Serializable