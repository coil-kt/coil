package coil.transform

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Path.Direction
import android.graphics.PorterDuff.Mode.SRC
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.Animatable
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
import coil.util.assertIsSimilarTo
import coil.util.decodeBitmapAsset
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimatedAndNormalTransformationTest {

    private lateinit var context: Context
    private lateinit var imageLoader: ImageLoader
    private lateinit var imageRequestBuilder: ImageRequest.Builder

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        imageLoader = ImageLoader.Builder(context)
            .crossfade(false)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
        imageRequestBuilder = ImageRequest.Builder(context)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .transformations(CircleCropTransformation())
            .animatedTransformation(AnimatedCircleTransformation())
            .allowConversionToBitmap(false)
    }

    @Test
    fun animatedGifStillAnimated() {
        val actual = runBlocking {
            val imageRequest = imageRequestBuilder
                .data("${ContentResolver.SCHEME_FILE}:///android_asset/animated.gif")
                .build()
            imageLoader.execute(imageRequest)
        }
        assertTrue(actual is SuccessResult)
        // Make sure this is still an animated result (has not been flattened to
        // apply CircleCropTransformation).
        assertTrue(actual.drawable is Animatable)
    }

    @Test
    fun staticImageStillTransformed() {
        val actual = runBlocking {
            val imageRequest = imageRequestBuilder
                .data("${ContentResolver.SCHEME_FILE}:///android_asset/normal_small.jpg")
                .build()
            imageLoader.execute(imageRequest)
        }
        val expected = context.decodeBitmapAsset("normal_small_circle.png")
        assertTrue(actual is SuccessResult)
        // Make sure this is not an animated result.
        assertFalse(actual.drawable is Animatable)
        actual.drawable.toBitmap().assertIsSimilarTo(expected)
    }

    class AnimatedCircleTransformation : AnimatedTransformation {
        override fun transform(canvas: Canvas): PixelOpacity {
            val path = Path()
            path.fillType = Path.FillType.INVERSE_EVEN_ODD
            val width = canvas.width
            val height = canvas.height
            val radius = width / 2f
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            path.addRoundRect(rect, radius, radius, Direction.CW)
            val paint = Paint()
            paint.isAntiAlias = true
            paint.color = Color.TRANSPARENT
            paint.xfermode = PorterDuffXfermode(SRC)
            canvas.drawPath(path, paint)
            return PixelOpacity.TRANSLUCENT
        }
    }
}
