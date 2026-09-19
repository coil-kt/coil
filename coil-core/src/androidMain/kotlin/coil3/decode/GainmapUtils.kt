package coil3.decode

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Gainmap
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import coil3.util.MIME_TYPE_JPEG
import java.nio.ByteBuffer

internal object GainmapUtils {

    private val isHardwareGainmapSupported: Boolean by lazy {
        if (SDK_INT < 34) return@lazy true
        try {
            val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
            val copy = bitmap.copy(Bitmap.Config.HARDWARE, false)
            val supported = copy != null
            bitmap.recycle()
            copy?.recycle()
            supported
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Returns true if the decoding path must work around the Android 14 platform bug
     * where single-channel (ALPHA_8) gainmaps are dropped when decoding directly to HARDWARE.
     */
    fun shouldWorkAroundHardwareGainmap(mimeType: String?): Boolean {
        return SDK_INT == 34 && mimeType == MIME_TYPE_JPEG && !isHardwareGainmapSupported
    }

    /**
     * Converts [inBitmap] and its gainmap (if present) to [Bitmap.Config.HARDWARE].
     * If [inBitmap] has an [Bitmap.Config.ALPHA_8] gainmap, converts it to an opaque
     * [Bitmap.Config.ARGB_8888] gainmap before uploading to hardware.
     */
    fun toHardwareBitmap(inBitmap: Bitmap): Bitmap {
        if (SDK_INT < 34 || !inBitmap.hasGainmap()) {
            val hardwareBitmap = inBitmap.copy(Bitmap.Config.HARDWARE, false) ?: return inBitmap
            inBitmap.recycle()
            return hardwareBitmap
        }

        val gainmap = inBitmap.gainmap
        if (gainmap == null) {
            val hardwareBitmap = inBitmap.copy(Bitmap.Config.HARDWARE, false) ?: return inBitmap
            inBitmap.recycle()
            return hardwareBitmap
        }

        val gainmapContents = gainmap.gainmapContents
        val hardwareGainmapContents: Bitmap
        val convertedGainmapContents: Bitmap?

        if (gainmapContents.config == Bitmap.Config.ALPHA_8) {
            convertedGainmapContents = convertAlpha8ToArgb8888(gainmapContents)
            hardwareGainmapContents = convertedGainmapContents.copy(Bitmap.Config.HARDWARE, false)
                ?: convertedGainmapContents
        } else {
            convertedGainmapContents = null
            hardwareGainmapContents = gainmapContents.copy(Bitmap.Config.HARDWARE, false)
                ?: gainmapContents
        }

        val hardwareGainmap = copyGainmap(gainmap, hardwareGainmapContents)
        val hardwareBaseBitmap = inBitmap.copy(Bitmap.Config.HARDWARE, false)

        if (hardwareBaseBitmap == null) {
            inBitmap.gainmap = hardwareGainmap
            if (convertedGainmapContents != null && hardwareGainmapContents !== convertedGainmapContents) {
                convertedGainmapContents.recycle()
            }
            return inBitmap
        }

        hardwareBaseBitmap.gainmap = hardwareGainmap

        // Detach the old gainmap before recycling intermediate software bitmaps
        inBitmap.gainmap = null

        inBitmap.recycle()
        if (hardwareGainmapContents !== gainmapContents) {
            gainmapContents.recycle()
        }
        if (convertedGainmapContents != null && hardwareGainmapContents !== convertedGainmapContents) {
            convertedGainmapContents.recycle()
        }

        return hardwareBaseBitmap
    }

    @RequiresApi(34)
    internal fun convertAlpha8ToArgb8888(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val alphaBuffer = ByteBuffer.allocate(width * height)
        source.copyPixelsToBuffer(alphaBuffer)
        val alphaBytes = alphaBuffer.array()

        val argbArray = IntArray(width * height)
        for (i in alphaBytes.indices) {
            val y = alphaBytes[i].toInt() and 0xFF
            argbArray[i] = Color.argb(255, y, y, y)
        }

        val argbBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        argbBitmap.setPixels(argbArray, 0, width, 0, 0, width, height)
        return argbBitmap
    }

    @RequiresApi(34)
    internal fun copyGainmap(source: Gainmap, newContents: Bitmap): Gainmap {
        val gainmap = Gainmap(newContents)

        val ratioMin = source.ratioMin
        gainmap.setRatioMin(ratioMin[0], ratioMin[1], ratioMin[2])

        val ratioMax = source.ratioMax
        gainmap.setRatioMax(ratioMax[0], ratioMax[1], ratioMax[2])

        val gamma = source.gamma
        gainmap.setGamma(gamma[0], gamma[1], gamma[2])

        val epsilonSdr = source.epsilonSdr
        gainmap.setEpsilonSdr(epsilonSdr[0], epsilonSdr[1], epsilonSdr[2])

        val epsilonHdr = source.epsilonHdr
        gainmap.setEpsilonHdr(epsilonHdr[0], epsilonHdr[1], epsilonHdr[2])

        gainmap.displayRatioForFullHdr = source.displayRatioForFullHdr
        gainmap.minDisplayRatioForHdrTransition = source.minDisplayRatioForHdrTransition

        return gainmap
    }
}
