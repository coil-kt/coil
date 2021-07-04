package coil.compose

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.annotation.IdRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import coil.decode.DecodeUtils
import coil.size.Scale
import coil.util.isSimilarTo
import kotlin.test.assertTrue

fun resourceUri(id: Int): Uri {
    val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
    return "${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/$id".toUri()
}

fun ImageBitmap.assertIsSimilarTo(@IdRes resId: Int) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val expected = context.getDrawable(resId)!!.toBitmap().fitCenter(width, height)
    assertTrue(asAndroidBitmap().isSimilarTo(expected, threshold = 0.9))
}

private fun Bitmap.fitCenter(width: Int, height: Int): Bitmap {
    val input = this.apply { density = Bitmap.DENSITY_NONE }

    return createBitmap(width, height).applyCanvas {
        // Draw the white background to match the test background.
        drawColor(Color.WHITE)

        val scale = DecodeUtils.computeSizeMultiplier(
            srcWidth = input.width,
            srcHeight = input.height,
            dstWidth = width,
            dstHeight = height,
            scale = Scale.FIT
        ).toFloat()
        val dx = (width - scale * input.width) / 2
        val dy = (height - scale * input.height) / 2

        translate(dx, dy)
        scale(scale, scale)
        drawBitmap(input, 0f, 0f, null)
    }
}
