package coil3.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Gainmap
import android.graphics.Paint
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting

internal object GainmapHardwareChecker {
    @get:VisibleForTesting
    internal var isBugPresentForTesting: Boolean? = null

    private val isBugPresentOnDevice by lazy {
        detectGainmapHardwareBug()
    }

    val hasGainmapHardwareBug: Boolean
        get() = isBugPresentForTesting ?: isBugPresentOnDevice

    private fun detectGainmapHardwareBug(): Boolean {
        if (SDK_INT != 34) return false
        return try {
            val testBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
            val hardwareBitmap = testBitmap.copy(Bitmap.Config.HARDWARE, false)
            val hasBug = hardwareBitmap == null
            hardwareBitmap?.recycle()
            testBitmap.recycle()
            hasBug
        } catch (_: Throwable) {
            true
        }
    }
}

/**
 * Android 14 (API 34) has a bug where uploading single-channel (ALPHA_8) gainmaps
 * to hardware bitmaps fails under OpenGL/skiagl, causing the gainmap to be silently dropped.
 *
 * This workaround converts the single-channel gainmap into a 3-channel ARGB_8888 gainmap
 * before copying the bitmap to [Bitmap.Config.HARDWARE].
 *
 * See https://github.com/coil-kt/coil/issues/2094
 */
@RequiresApi(34)
internal fun Bitmap.fixGainmapHardwareBug(): Bitmap {
    if (!hasGainmap()) {
        val hardwareBitmap = copy(Bitmap.Config.HARDWARE, false)
        if (hardwareBitmap != null) {
            recycle()
            return hardwareBitmap
        }
        return this
    }

    val currentGainmap = gainmap
    if (currentGainmap != null && currentGainmap.gainmapContents.config == Bitmap.Config.ALPHA_8) {
        gainmap = convertSingleChannelGainmapToTripleChannelGainmap(currentGainmap)
    }

    val hardwareBitmap = copy(Bitmap.Config.HARDWARE, false)
    if (hardwareBitmap != null) {
        if (!hardwareBitmap.hasGainmap()) {
            hardwareBitmap.gainmap = gainmap
        }
        recycle()
        return hardwareBitmap
    }
    return this
}

@RequiresApi(34)
@VisibleForTesting
internal fun convertSingleChannelGainmapToTripleChannelGainmap(gainmap: Gainmap): Gainmap {
    val contents = gainmap.gainmapContents
    val newContents = copyWithOpaqueAlpha(contents)
    return Gainmap(newContents).apply {
        displayRatioForFullHdr = gainmap.displayRatioForFullHdr
        minDisplayRatioForHdrTransition = gainmap.minDisplayRatioForHdrTransition

        val epsilonHdr = gainmap.epsilonHdr
        if (epsilonHdr.size >= 3) {
            setEpsilonHdr(epsilonHdr[0], epsilonHdr[1], epsilonHdr[2])
        } else if (epsilonHdr.isNotEmpty()) {
            setEpsilonHdr(epsilonHdr[0], epsilonHdr[0], epsilonHdr[0])
        }

        val epsilonSdr = gainmap.epsilonSdr
        if (epsilonSdr.size >= 3) {
            setEpsilonSdr(epsilonSdr[0], epsilonSdr[1], epsilonSdr[2])
        } else if (epsilonSdr.isNotEmpty()) {
            setEpsilonSdr(epsilonSdr[0], epsilonSdr[0], epsilonSdr[0])
        }

        val gamma = gainmap.gamma
        if (gamma.size >= 3) {
            setGamma(gamma[0], gamma[1], gamma[2])
        } else if (gamma.isNotEmpty()) {
            setGamma(gamma[0], gamma[0], gamma[0])
        }

        val ratioMax = gainmap.ratioMax
        if (ratioMax.size >= 3) {
            setRatioMax(ratioMax[0], ratioMax[1], ratioMax[2])
        } else if (ratioMax.isNotEmpty()) {
            setRatioMax(ratioMax[0], ratioMax[0], ratioMax[0])
        }

        val ratioMin = gainmap.ratioMin
        if (ratioMin.size >= 3) {
            setRatioMin(ratioMin[0], ratioMin[1], ratioMin[2])
        } else if (ratioMin.isNotEmpty()) {
            setRatioMin(ratioMin[0], ratioMin[1], ratioMin[0])
        }
    }
}

@RequiresApi(34)
@VisibleForTesting
internal fun copyWithOpaqueAlpha(bitmap: Bitmap): Bitmap {
    val newContents = Bitmap.createBitmap(
        bitmap.width,
        bitmap.height,
        Bitmap.Config.ARGB_8888,
    )
    val canvas = Canvas(newContents)
    val paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(
            floatArrayOf(
                0f, 0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f, 0f,
                0f, 0f, 0f, 0f, 255f,
            ),
        )
    }
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return newContents
}
