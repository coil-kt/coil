package coil3.video

import coil3.decode.DecodeUtils
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import okio.buffer
import okio.source
import org.junit.Test

class DecodeUtilsTest : RobolectricTest() {

    @Test
    fun detectsMp4() = assertVideoDetection("sample.mp4")

    @Test
    fun detectsOgg() = assertVideoDetection("sample.ogv")

    @Test
    fun detectsWebm() = assertVideoDetection("sample.webm")

    @Test
    fun detectsFlv() = assertVideoDetection("sample.flv")

    @Test
    fun detectsMpegProgramStream() = assertVideoDetection("sample.mpg")

    @Test
    fun detectsAsf() = assertVideoDetection("sample.asf")

    @Test
    fun detectsRealMedia() = assertVideoDetection("sample.rm")

    @Test
    fun detectsMpegTransportStream() = assertVideoDetection("sample.ts")

    @Test
    fun doesNotDetectJpeg() = assertNotVideoDetection("sample.jpg")

    @Test
    fun doesNotDetectAnimatedGif() = assertNotVideoDetection("animated.gif")

    private fun assertVideoDetection(fileName: String) {
        context.assets.open("video/$fileName").use { stream ->
            stream.source().buffer().use { source ->
                assertTrue(
                    actual = DecodeUtils.isVideo(source),
                    message = "Expected DecodeUtils to detect $fileName as a video container.",
                )
            }
        }
    }

    private fun assertNotVideoDetection(fileName: String) {
        context.assets.open("video/$fileName").use { stream ->
            stream.source().buffer().use { source ->
                assertFalse(
                    actual = DecodeUtils.isVideo(source),
                    message = "Expected DecodeUtils NOT to detect $fileName as a video container.",
                )
            }
        }
    }
}
