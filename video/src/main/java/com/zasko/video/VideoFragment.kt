package com.zasko.video

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.zasko.video.databinding.FragmentVideoBinding
import java.io.Serializable
import kotlin.random.Random

class VideoFragment : Fragment() {
    companion object {
        const val TAG = "VideoFragment"

        const val KEY_INFO = "key_info"

        val SOURCE_IDS = arrayOf(R.raw.video11, R.raw.video12, R.raw.video13, R.raw.video14)
    }

    private lateinit var viewBinding: FragmentVideoBinding


    private lateinit var tranData: VideoFragmentData

    private var player: Player? = null
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
        initPlayer()
    }


    @SuppressLint("UnsafeOptInUsageError")
    private fun initPlayer() {
        releasePlayer()

        player = ExoPlayer.Builder(viewBinding.playerView.context)
            .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(2000, 4000, 1000, 2000).build()).build()

        player?.let {
            viewBinding.playerView.setPlayer(it)
            it.repeatMode = Player.REPEAT_MODE_ONE
            it.setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://" + context?.packageName + "/" + SOURCE_IDS[Random.nextInt(SOURCE_IDS.size)])))
            it.prepare()
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.let {
            it.release()
            Log.e(TAG, "releasePlayer: index:${tranData.index}")
            player = null
        }
    }

    fun resetPlayer() {
        player?.let {
            it.seekTo(0)
        }
    }

}

data class VideoFragmentData(var index: Int = 0) : Serializable