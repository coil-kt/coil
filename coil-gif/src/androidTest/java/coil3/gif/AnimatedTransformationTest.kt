package coil3.gif

import android.content.ContentResolver.SCHEME_FILE
import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import androidx.core.graphics.drawable.toBitmap
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.bitmapConfig
import coil3.test.utils.assertIsSimilarTo
import coil3.test.utils.assumeTrue
import coil3.test.utils.context
import coil3.test.utils.decodeBitmapAsset
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class AnimatedTransformationTest {

    private lateinit var imageLoader: ImageLoader

    @Before
    fun before() {
        imageLoader = ImageLoader(context)
    }

    @After
    fun after() {
        imageLoader.shutdown()
    }

    @Test
    fun gifTransformationTest() = runTest {
        val decoderFactory = if (SDK_INT >= 28) {
            AnimatedImageDecoder.Factory()
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
        assertIs<SuccessResult>(actual)
        actual.image.asDrawable(context.resources).toBitmap()
            .assertIsSimilarTo(expected, threshold = 0.98)
    }

    @Test
    fun heifTransformationTest() = runTest {
        // HEIF files are only supported on API 29+.
        assumeTrue(SDK_INT >= 29)

        // TODO: Figure out why this fails on recent emulators.
        assumeTrue(SDK_INT < 32)

        val imageRequest = ImageRequest.Builder(context)
            .data("$SCHEME_FILE:///android_asset/animated.heif")
            .animatedTransformation(RoundedCornersAnimatedTransformation())
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .decoderFactory(AnimatedImageDecoder.Factory())
            .build()
        val actual = imageLoader.execute(imageRequest)
        val expected = context.decodeBitmapAsset("animated_heif_rounded.png")
        assertIs<SuccessResult>(actual)
        actual.image.asDrawable(context.resources).toBitmap()
            .assertIsSimilarTo(expected, threshold = 0.98)
    }

    @Test
    fun animatedWebpTransformationTest() = runTest {
        // Animated WebP is only support on API 28+.
        assumeTrue(SDK_INT >= 28)

        val imageRequest = ImageRequest.Builder(context)
            .data("$SCHEME_FILE:///android_asset/animated.webp")
            .animatedTransformation(RoundedCornersAnimatedTransformation())
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .decoderFactory(AnimatedImageDecoder.Factory())
            .build()
        val actual = imageLoader.execute(imageRequest)
        val expected = context.decodeBitmapAsset("animated_webp_rounded.png")
        assertIs<SuccessResult>(actual)
        actual.image.asDrawable(context.resources).toBitmap()
            .assertIsSimilarTo(expected, threshold = 0.98)
    }
}
