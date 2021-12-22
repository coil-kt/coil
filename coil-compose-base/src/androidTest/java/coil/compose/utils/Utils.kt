package coil.compose.utils

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.FloatRange
import androidx.annotation.IdRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.unit.width
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import coil.decode.DecodeUtils
import coil.size.Scale
import coil.util.assertIsSimilarTo
import coil.util.assumeTrue
import kotlin.math.abs

fun assumeSupportsCaptureToImage() {
    assumeTrue(SDK_INT >= 26, "captureToImage is not supported on SDK_INT=$SDK_INT")
}

fun resourceUri(@IdRes resId: Int): Uri {
    val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
    return "$SCHEME_ANDROID_RESOURCE://$packageName/$resId".toUri()
}

fun ImageBitmap.assertIsSimilarTo(
    @IdRes resId: Int,
    scale: Scale = Scale.FIT,
    @FloatRange(from = -1.0, to = 1.0) threshold: Double = 0.9 // Use a lower threshold by default.
) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val expected = AppCompatResources.getDrawable(context, resId)!!
        .toBitmap().scale(width, height, scale)
    asAndroidBitmap().assertIsSimilarTo(expected, threshold)
}

fun ImageBitmap.assertIsSimilarTo(
    bitmap: ImageBitmap,
    @FloatRange(from = -1.0, to = 1.0) threshold: Double = 0.9 // Use a lower threshold by default.
) {
    asAndroidBitmap().assertIsSimilarTo(bitmap.asAndroidBitmap(), threshold)
}

private fun Bitmap.scale(width: Int, height: Int, scale: Scale = Scale.FIT): Bitmap {
    val input = apply { density = Bitmap.DENSITY_NONE }

    return createBitmap(width, height).applyCanvas {
        // Draw the white background to match the test background.
        drawColor(Color.WHITE)

        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = input.width,
            srcHeight = input.height,
            dstWidth = width,
            dstHeight = height,
            scale = scale
        ).toFloat()
        val dx = (width - multiplier * input.width) / 2
        val dy = (height - multiplier * input.height) / 2

        translate(dx, dy)
        scale(multiplier, multiplier)
        drawBitmap(input, 0f, 0f, null)
    }
}

fun SemanticsNodeInteraction.assertWidthIsEqualTo(
    expectedWidth: Dp,
    tolerance: Dp = Dp(0.5f)
) = withUnclippedBoundsInRoot {
    it.width.assertIsEqualTo(expectedWidth, "width", tolerance)
}

fun SemanticsNodeInteraction.assertHeightIsEqualTo(
    expectedHeight: Dp,
    tolerance: Dp = Dp(0.5f)
) = withUnclippedBoundsInRoot {
    it.height.assertIsEqualTo(expectedHeight, "height", tolerance)
}

private fun SemanticsNodeInteraction.withUnclippedBoundsInRoot(
    assertion: (DpRect) -> Unit
) = apply {
    val node = fetchSemanticsNode("Failed to retrieve bounds of the node.")
    val bounds = with(node.root!!.density) {
        node.unclippedBoundsInRoot.let {
            DpRect(it.left.toDp(), it.top.toDp(), it.right.toDp(), it.bottom.toDp())
        }
    }
    assertion.invoke(bounds)
}

private val SemanticsNode.unclippedBoundsInRoot: Rect
    get() = if (layoutInfo.isPlaced) {
        Rect(positionInRoot, size.toSize())
    } else {
        Dp.Unspecified.value.let { Rect(it, it, it, it) }
    }

private fun Dp.assertIsEqualTo(expected: Dp, subject: String, tolerance: Dp) {
    if (!isWithinTolerance(expected, tolerance)) {
        throw AssertionError(
            "Actual $subject is $this, expected $expected (tolerance: $tolerance)"
        )
    }
}

private fun Dp.isWithinTolerance(reference: Dp, tolerance: Dp): Boolean {
    return when {
        reference.isUnspecified -> this.isUnspecified
        reference.value.isInfinite() -> this.value == reference.value
        else -> abs(this.value - reference.value) <= tolerance.value
    }
}
