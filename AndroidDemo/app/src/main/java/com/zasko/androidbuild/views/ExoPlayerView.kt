package com.zasko.androidbuild.views

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout

@UnstableApi
class ExoPlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var contentFrame: AspectRatioFrameLayout = AspectRatioFrameLayout(context)
    private var currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT


    private var surfaceView: TextureView

    private var mPlayer: Player? = null

    private val componentListener = object : ComponentListener() {

    }

    init {
        contentFrame.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {}
        addView(contentFrame, 0)

        surfaceView = TextureView(context)
        surfaceView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        surfaceView.isClickable = false
        contentFrame.addView(surfaceView, 0)
    }

    fun setPlayer(exoPlayer: ExoPlayer) {
        mPlayer?.let { p ->
            p.removeListener(componentListener)
        }
        mPlayer = exoPlayer
        mPlayer?.let { p ->
            p.setVideoTextureView(surfaceView)
            p.addListener(componentListener)

        }

    }


    open inner class ComponentListener : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            super.onVideoSizeChanged(videoSize)
            if (videoSize == VideoSize.UNKNOWN || mPlayer == null || mPlayer?.playbackState == Player.STATE_IDLE) {
                return
            }
            updateAspectRatio()
        }
    }

    private fun updateAspectRatio() {
        mPlayer?.let { p ->
            val videoSize = p.videoSize
            val width = videoSize.width
            val height = videoSize.height
            val videoAspectRatio = if ((height == 0 || width == 0)) 0f else (width * videoSize.pixelWidthHeightRatio) / height
            contentFrame.setAspectRatio(videoAspectRatio)
        }
    }


}