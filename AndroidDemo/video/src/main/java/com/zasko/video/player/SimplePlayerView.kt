package com.zasko.video.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_GET_TRACKS
import androidx.media3.common.Player.COMMAND_SET_VIDEO_SURFACE
import androidx.media3.common.VideoSize
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.AspectRatioFrameLayout.ResizeMode

/**
 *
 */

@SuppressLint("UnsafeOptInUsageError")
class SimplePlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {


    private var contentFrame: AspectRatioFrameLayout
    private var currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

    private var surfaceView: View

    private var mPlayer: Player? = null

    private var textureViewRotation = 0

    init {

        contentFrame = AspectRatioFrameLayout(context)
        contentFrame.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
            this.gravity = Gravity.CENTER
        }
        addView(contentFrame, 0)
        contentFrame.resizeMode = currentResizeMode

        //默认是textureView
        surfaceView = TextureView(context)
        surfaceView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        surfaceView.isClickable = false
        contentFrame.addView(surfaceView, 0)

    }

    fun setResizeMode(resizeMode: @ResizeMode Int) {
        contentFrame.resizeMode = resizeMode
    }

    fun setPlayer(player: Player) {
        if (mPlayer == player) {
            return
        }
        mPlayer?.let { oldPlayer ->
            oldPlayer.removeListener(componentListener)
            if (oldPlayer.isCommandAvailable(COMMAND_SET_VIDEO_SURFACE)) {
                oldPlayer.clearVideoTextureView(surfaceView as? TextureView)
            }
        }
        mPlayer = player
        if (player.isCommandAvailable(COMMAND_SET_VIDEO_SURFACE)) {
            player.setVideoTextureView(surfaceView as? TextureView)
        }
        if (!player.isCommandAvailable(COMMAND_GET_TRACKS) || player.currentTracks.isTypeSupported(C.TRACK_TYPE_VIDEO)) {
            updateAspectRatio()
        }
        player.addListener(componentListener)
    }

    private fun updateAspectRatio() {
        val videoSize = mPlayer?.videoSize ?: VideoSize.UNKNOWN
        val width = videoSize.width
        val height = videoSize.height

        val unAppliedRotationDegrees = videoSize.unappliedRotationDegrees
        var videoAspectRatio: Float = if ((height == 0 || width == 0)) 0f else (width * videoSize.pixelWidthHeightRatio) / height

        if (surfaceView is TextureView) {
            // Try to apply rotation transformation when our surface is a TextureView.
            if (videoAspectRatio > 0 && (unAppliedRotationDegrees == 90 || unAppliedRotationDegrees == 270)) {
                // We will apply a rotation 90/270 degree to the output texture of the TextureView.
                // In this case, the output video's width and height will be swapped.
                videoAspectRatio = 1 / videoAspectRatio
            }
            if (textureViewRotation != 0) {
                surfaceView.removeOnLayoutChangeListener(componentListener)
            }
            textureViewRotation = unAppliedRotationDegrees
            if (textureViewRotation != 0) {
                // The texture view's dimensions might be changed after layout step.
                // So add an OnLayoutChangeListener to apply rotation after layout step.
                surfaceView.addOnLayoutChangeListener(componentListener)
            }
            applyTextureViewRotation(surfaceView as TextureView, textureViewRotation)
        }

        val aspectRatio = videoAspectRatio
        contentFrame.setAspectRatio(aspectRatio)

    }

    private val componentListener = object : ComponentListener() {


    }


    open inner class ComponentListener : Player.Listener, OnLayoutChangeListener {
        override fun onLayoutChange(
            view: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
        ) {
            (view as? TextureView)?.let {
                applyTextureViewRotation(it, textureViewRotation)
            }

        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize == VideoSize.UNKNOWN || mPlayer == null || mPlayer?.playbackState == Player.STATE_IDLE) {
                return
            }
            updateAspectRatio()
        }
    }

    private fun applyTextureViewRotation(textureView: TextureView, textureViewRotation: Int) {
        val transformMatrix = Matrix()
        val textureViewWidth = textureView.width.toFloat()
        val textureViewHeight = textureView.height.toFloat()
        if (textureViewWidth != 0f && textureViewHeight != 0f && textureViewRotation != 0) {
            val pivotX = textureViewWidth / 2
            val pivotY = textureViewHeight / 2
            transformMatrix.postRotate(textureViewRotation.toFloat(), pivotX, pivotY)

            // After rotation, scale the rotated texture to fit the TextureView size.
            val originalTextureRect = RectF(0f, 0f, textureViewWidth, textureViewHeight)
            val rotatedTextureRect = RectF()
            transformMatrix.mapRect(rotatedTextureRect, originalTextureRect)
            transformMatrix.postScale(
                textureViewWidth / rotatedTextureRect.width(), textureViewHeight / rotatedTextureRect.height(), pivotX, pivotY
            )
        }
        textureView.setTransform(transformMatrix)
    }


}