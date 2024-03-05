package com.zasko.video.player

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.annotation.RequiresApi
import com.zasko.video.R
import java.nio.ByteBuffer

object MMediaCodec {

    const val TAG = "MMediaCodec"

    private val handlerThread = HandlerThread("codecName")


    @RequiresApi(Build.VERSION_CODES.N)
    fun codecName(context: Context) {
        handlerThread.start()
        val handler = Handler(handlerThread.looper) {
            Log.d(TAG, "codecName: thread name:${Thread.currentThread().isInterrupted}")
            val extractor = MediaExtractor()
            extractor.setDataSource(context.resources.openRawResourceFd(R.raw.video100))

            var mediaFormat: MediaFormat? = null
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)

                val mime = format.getString(MediaFormat.KEY_MIME)
                Log.d(TAG, "codecName: index:${index} format:${mime} ")

                if (mime?.startsWith("video/") == true) {
                    mediaFormat = format
                    extractor.selectTrack(index)
                }
            }

            val maxSize = mediaFormat?.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) ?: (1024 * 1024)
            Log.d(TAG, "codecName: buffSize:${maxSize}")
            val byteBuffer = ByteBuffer.allocate(maxSize)
            while (true) {
                if (handlerThread.isInterrupted) {
                    break
                }
                byteBuffer.clear()
                val size = extractor.readSampleData(byteBuffer, 0)
                if (size < 0) {
                    break
                }
                extractor.advance()
            }
            extractor.release()

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