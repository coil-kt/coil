package coil.decode

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import org.junit.Before
import org.junit.Test

class VideoFrameDecoderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var pool: BitmapPool
    private lateinit var service: VideoFrameDecoder

    @Before
    fun before() {
        pool = BitmapPool(0)
        service = VideoFrameDecoder(context)
    }

    @Test
    fun noSetFrameTime() {

    }

    @Test
    fun specificFrameTime() {

    }
}
