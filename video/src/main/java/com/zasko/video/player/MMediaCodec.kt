package com.zasko.video.player

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.annotation.RequiresApi
import com.zasko.video.R
import java.nio.ByteBuffer
import kotlin.math.min

object MMediaCodec {

    const val TAG = "MMediaCodec"

    private val handlerThread = HandlerThread("codecName")


    @RequiresApi(Build.VERSION_CODES.N)
    fun codecName(context: Context) {
        handlerThread.start()
        val handler = Handler(handlerThread.looper) {
            Log.d(TAG, "codecName: thread name:${Thread.currentThread().isInterrupted}")
            val extractor = MediaExtractor()
            extractor.setDataSource(context.resources.openRawResourceFd(R.raw.video12))

            var mediaFormat: MediaFormat? = null
            var mediaCodec: MediaCodec? = null


            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)

                val mime = format.getString(MediaFormat.KEY_MIME)
                Log.d(TAG, "codecName: index:${index} format:${mime} ")

                if (mime?.startsWith("video/") == true) {
                    mediaFormat = format
                    extractor.selectTrack(index)

                    mediaCodec = MediaCodec.createDecoderByType(mime)
                    break
                }
            }
            Log.d(TAG, "codecName: mediaCodec")
            mediaCodec?.setCallback(object : MediaCodec.Callback() {
                override fun onInputBufferAvailable(codec: MediaCodec, inputBufferId: Int) {
                    if (handlerThread.isInterrupted) {
                        Log.e(TAG, "onInputBufferAvailable: isInterrupted")
                        extractor.release()
                        mediaCodec.stop()
                        mediaCodec.release()
                        return
                    }
                    codec.getInputBuffer(inputBufferId)?.let { byteBuffer ->
                        val sampleSize = extractor.readSampleData(byteBuffer, 0)
                        Log.d(TAG, "onInputBufferAvailable: sampleSize:${sampleSize} time:${extractor.sampleTime}")
                        if (sampleSize < 0) {
                            //结束了
//                            mediaCodec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            handlerThread.interrupt()

                        } else {
                            mediaCodec.queueInputBuffer(inputBufferId, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                override fun onOutputBufferAvailable(codec: MediaCodec, outputBufferId: Int, bufferInfo: MediaCodec.BufferInfo) {

                    val buffer = codec.getOutputBuffer(outputBufferId)
                    val mediaFormat = codec.getOutputFormat(outputBufferId)
                    Log.e(TAG, "onOutputBufferAvailable: bufferInfo ${bufferInfo.presentationTimeUs}")
                    codec.releaseOutputBuffer(outputBufferId, false)
                }

                override fun onError(p0: MediaCodec, p1: MediaCodec.CodecException) {
                }

                override fun onOutputFormatChanged(p0: MediaCodec, p1: MediaFormat) {
                }

            })
            mediaCodec?.configure(mediaFormat, null, null, 0)
            mediaCodec?.start()

            true
        }
        handler.sendEmptyMessage(0)

    }

    fun interrupted() {
        if (!handlerThread.isInterrupted) {
            handlerThread.interrupt()
        }
    }
}