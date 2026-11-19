package com.zasko.apppoint

import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.zasko.apppoint.automation.DemonRunner
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class EntranceRecognitionTest {
    @Test fun locatesProportionalButtonOnDeviceScreenshots() {
        val names = InstrumentationRegistry.getArguments().getString("entranceScreenshots")
        assumeTrue("Pass entranceScreenshots with device-local screenshot file names", !names.isNullOrBlank())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        names!!.split(',').forEach { name ->
            val file = File(context.filesDir, name)
            val image = requireNotNull(BitmapFactory.decodeFile(file.path))
            try {
                val point = DemonRunner.recognizeEntrance(image)
                Log.i("EntranceRecognitionTest", "$name point=$point size=${image.width}x${image.height}")
                assertNotNull("No entrance found in $name", point)
                assertEquals(.5, point!!.x, .02)
                assertTrue(point.y in .1.. .78)
            } finally { image.recycle() }
        }
    }
}
