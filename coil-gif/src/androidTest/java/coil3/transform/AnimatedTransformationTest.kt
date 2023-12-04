package coil3.transform

import android.content.ContentResolver.SCHEME_FILE
import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import androidx.core.graphics.drawable.toBitmap
import coil3.ImageLoader
import coil3.decode.GifDecoder
import coil3.decode.ImageDecoderDecoder
import coil3.drawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.animatedTransformation
import coil3.request.bitmapConfig
import coil3.test.assertIsSimilarTo
import coil3.test.assumeTrue
import coil3.test.context
import coil3.test.decodeBitmapAsset
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AnimatedTransformationTest {

    private lateinit var imageLoader: ImageLoader

    @Before
    fun before() {
        imageLoader = ImageLoader(context)
    }

    @Test
    fun gifTransformationTest() = runTest {
        val decoderFactory = if (SDK_INT >= 28) {
            ImageDecoderDecoder.Factory()
        } else {
            GifDecoder.Factory()
        }
        val imageRequest = ImageRequest.Builder(context)
            .data("$SCHEME_FILE:///android_asset/animated.gif")
            .animatedTransformation(RoundedCornersAnimatedTransformation())
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .decoderFactory(decoderFactory)
            .build()
        val actual = imageLoader.execute(imageRequest)
        val expected = context.decodeBitmapAsset("animated_gif_rounded.png")
        assertTrue(actual is SuccessResult)
        actual.image.drawable.toBitmap().assertIsSimilarTo(expected, threshold = 0.98)
    }

    @Test
    fun heifTransformationTest() = runTest {
        // Animated HEIF is only support on API 28+.
        assumeTrue(SDK_INT >= 28)

        val imageRequest = ImageRequest.Builder(context)
            .data("$SCHEME_FILE:///android_asset/animated.heif")
            .animatedTransformation(RoundedCornersAnimatedTransformation())
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .decoderFactory(ImageDecoderDecoder.Factory())
            .build()
        val actual = imageLoader.execute(imageRequest)
        val expected = context.decodeBitmapAsset("animated_heif_rounded.png")
        assertTrue(actual is SuccessResult)
        actual.image.drawable.toBitmap().assertIsSimilarTo(expected, threshold = 0.98)
    }

    @Test
    fun animatedWebpTransformationTest() = runTest {
        // Animated WebP is only support on API 28+.
        assumeTrue(SDK_INT >= 28)

        val imageRequest = ImageRequest.Builder(context)
            .data("$SCHEME_FILE:///android_asset/animated.webp")
            .animatedTransformation(RoundedCornersAnimatedTransformation())
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .decoderFactory(ImageDecoderDecoder.Factory())
            .build()
        val actual = imageLoader.execute(imageRequest)
        val expected = context.decodeBitmapAsset("animated_webp_rounded.png")
        assertTrue(actual is SuccessResult)
        actual.image.drawable.toBitmap().assertIsSimilarTo(expected, threshold = 0.98)
    }
}
