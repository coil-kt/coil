package coil.compose

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertTrue

fun resourceUri(id: Int): Uri {
    val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
    return "${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/$id".toUri()
}

fun ImageBitmap.assertPixels(expected: Color, tolerance: Float = 0.001f) {
    toPixelMap().buffer.forEach { pixel ->
        val color = Color(pixel)
        assertTrue(color.red in rangeOf(expected.red, tolerance))
        assertTrue(color.green in rangeOf(expected.green, tolerance))
        assertTrue(color.blue in rangeOf(expected.blue, tolerance))
        assertTrue(color.alpha in rangeOf(expected.blue, tolerance))
    }
}

private fun rangeOf(value: Float, tolerance: Float): ClosedFloatingPointRange<Float> {
    return (value - tolerance)..(value + tolerance)
}
