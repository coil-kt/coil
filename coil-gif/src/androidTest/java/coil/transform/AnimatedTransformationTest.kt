package coil.transform

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import androidx.core.graphics.drawable.toBitmap
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.CachePolicy
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
    private lateinit var transformation: RoundedCornersAnimatedTransformation
    private lateinit var imageLoader: ImageLoader
    private lateinit var imageRequestBuilder: ImageRequest.Builder

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        transformation = RoundedCornersAnimatedTransformation()
        imageLoader = ImageLoader.Builder(context)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
        imageRequestBuilder = ImageRequest.Builder(context)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .animatedTransformation(transformation)
    }

    @Test
    fun gifTransformationTest() {
        val actual = runBlocking {
            val decoder = if (SDK_INT >= 28) {
                ImageDecoderDecoder(context)
            } else {
                GifDecoder()
            }
            val imageRequest = imageRequestBuilder
                .decoder(decoder)
                .data("${ContentResolver.SCHEME_FILE}:///android_asset/animated.gif")
                .build()
            imageLoader.execute(imageRequest)
        }
        val expected = context.decodeBitmapAsset("animated_gif_rounded.png")
        assertTrue(actual is SuccessResult)
        assertTrue(actual.drawable.toBitmap().isSimilarTo(expected))
    }

    @Test
    fun heifTransformationTest() {
        assumeTrue(SDK_INT >= 28)

        val actual = runBlocking {
            val imageRequest = imageRequestBuilder
                .decoder(ImageDecoderDecoder())
                .data("${ContentResolver.SCHEME_FILE}:///android_asset/animated.heif")
                .build()
            imageLoader.execute(imageRequest)
        }
        val expected = context.decodeBitmapAsset("animated_heif_rounded.png")
        assertTrue(actual is SuccessResult)
        assertTrue(actual.drawable.toBitmap().isSimilarTo(expected))
    }

    @Test
    fun webpTransformationTest() {
        assumeTrue(SDK_INT >= 28)

        val actual = runBlocking {
            val imageRequest = imageRequestBuilder
                .decoder(ImageDecoderDecoder())
                .data("${ContentResolver.SCHEME_FILE}:///android_asset/animated.webp")
                .build()
            imageLoader.execute(imageRequest)
        }
        val expected = context.decodeBitmapAsset("animated_webp_rounded.png")
        assertTrue(actual is SuccessResult)
        assertTrue(actual.drawable.toBitmap().isSimilarTo(expected))
    }
}
