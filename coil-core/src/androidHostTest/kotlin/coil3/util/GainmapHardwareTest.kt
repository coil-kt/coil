package coil3.util

import android.graphics.Bitmap
import android.graphics.Gainmap
import androidx.test.filters.SdkSuppress
import coil3.test.utils.RobolectricTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.robolectric.annotation.Config

@Config(sdk = [34])
@SdkSuppress(minSdkVersion = 34)
class GainmapHardwareTest : RobolectricTest() {

    @AfterTest
    fun tearDown() {
        GainmapHardwareChecker.isBugPresentForTesting = null
    }

    @Test
    fun `gainmap hardware bug flag can be overridden for testing`() {
        GainmapHardwareChecker.isBugPresentForTesting = true
        assertTrue(GainmapHardwareChecker.hasGainmapHardwareBug)

        GainmapHardwareChecker.isBugPresentForTesting = false
        assertEquals(false, GainmapHardwareChecker.hasGainmapHardwareBug)
    }

    @Test
    fun `single channel gainmap is converted to triple channel gainmap`() {
        val alphaBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8)
        val originalGainmap = Gainmap(alphaBitmap).apply {
            displayRatioForFullHdr = 2.5f
            minDisplayRatioForHdrTransition = 1.2f
            setEpsilonHdr(0.1f, 0.2f, 0.3f)
            setEpsilonSdr(0.01f, 0.02f, 0.03f)
            setGamma(1.0f, 1.1f, 1.2f)
            setRatioMax(3.0f, 3.1f, 3.2f)
            setRatioMin(0.5f, 0.6f, 0.7f)
        }

        val convertedGainmap = convertSingleChannelGainmapToTripleChannelGainmap(originalGainmap)

        assertEquals(Bitmap.Config.ARGB_8888, convertedGainmap.gainmapContents.config)
        assertEquals(10, convertedGainmap.gainmapContents.width)
        assertEquals(10, convertedGainmap.gainmapContents.height)
        assertEquals(2.5f, convertedGainmap.displayRatioForFullHdr)
        assertEquals(1.2f, convertedGainmap.minDisplayRatioForHdrTransition)
        assertTrue(convertedGainmap.epsilonHdr.contentEquals(floatArrayOf(0.1f, 0.2f, 0.3f)))
        assertTrue(convertedGainmap.epsilonSdr.contentEquals(floatArrayOf(0.01f, 0.02f, 0.03f)))
        assertTrue(convertedGainmap.gamma.contentEquals(floatArrayOf(1.0f, 1.1f, 1.2f)))
        assertTrue(convertedGainmap.ratioMax.contentEquals(floatArrayOf(3.0f, 3.1f, 3.2f)))
        assertTrue(convertedGainmap.ratioMin.contentEquals(floatArrayOf(0.5f, 0.6f, 0.7f)))
    }

    @Test
    fun `copyWithOpaqueAlpha returns ARGB_8888 bitmap with same dimensions`() {
        val alphaBitmap = Bitmap.createBitmap(16, 24, Bitmap.Config.ALPHA_8)
        val argbBitmap = copyWithOpaqueAlpha(alphaBitmap)

        assertEquals(Bitmap.Config.ARGB_8888, argbBitmap.config)
        assertEquals(16, argbBitmap.width)
        assertEquals(24, argbBitmap.height)
    }

    @Test
    fun `fixGainmapHardwareBug converts ALPHA_8 gainmap on bitmap`() {
        val baseBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val alphaGainmap = Gainmap(Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8))
        baseBitmap.gainmap = alphaGainmap

        val resultBitmap = baseBitmap.fixGainmapHardwareBug()
        assertNotNull(resultBitmap)
        val resultGainmap = resultBitmap.gainmap
        assertNotNull(resultGainmap)
        assertEquals(Bitmap.Config.ARGB_8888, resultGainmap.gainmapContents.config)
    }

    @Test
    fun `fixGainmapHardwareBug on bitmap without gainmap succeeds`() {
        val baseBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val resultBitmap = baseBitmap.fixGainmapHardwareBug()
        assertNotNull(resultBitmap)
    }
}
