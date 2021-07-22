package coil.transform

import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import androidx.core.graphics.drawable.toBitmap
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.request.animatedTransformation
import coil.util.decodeBitmapAsset
import coil.util.isSimilarTo
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class AnimatedTransformationTest {

    private lateinit var context: Context
    private lateinit var imageLoader: ImageLoader

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        imageLoader = ImageLoader(context)
    }

    @Test
    fun gifTransformationTest() {
        val actual = runBlocking {
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
            imageLoader.execute(imageRequest)
        }
        val expected = context.decodeBitmapAsset("animated_gif_rounded.png")
        assertTrue(actual is SuccessResult)
        assertTrue(actual.drawable.toBitmap().isSimilarTo(expected))
    }

    @Test
    fun heifTransformationTest() {
        // Animated HEIF is only support on API 28+.
        assumeTrue(SDK_INT >= 28)

        val actual = runBlocking {
            val imageRequest = ImageRequest.Builder(context)
                .data("$SCHEME_FILE:///android_asset/animated.heif")
                .animatedTransformation(RoundedCornersAnimatedTransformation())
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .decoderFactory(ImageDecoderDecoder.Factory())
                .build()
            imageLoader.execute(imageRequest)
        }
        val expected = context.decodeBitmapAsset("animated_heif_rounded.png")
        assertTrue(actual is SuccessResult)
        assertTrue(actual.drawable.toBitmap().isSimilarTo(expected))
    }

    @Test
    fun animatedWebpTransformationTest() {
        // Animated WebP is only support on API 28+.
        assumeTrue(SDK_INT >= 28)

        val actual = runBlocking {
            val imageRequest = ImageRequest.Builder(context)
                .data("$SCHEME_FILE:///android_asset/animated.webp")
                .animatedTransformation(RoundedCornersAnimatedTransformation())
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .decoderFactory(ImageDecoderDecoder.Factory())
                .build()
            imageLoader.execute(imageRequest)
        }
        val expected = context.decodeBitmapAsset("animated_webp_rounded.png")
        assertTrue(actual is SuccessResult)
        assertTrue(actual.drawable.toBitmap().isSimilarTo(expected))
    }
}
