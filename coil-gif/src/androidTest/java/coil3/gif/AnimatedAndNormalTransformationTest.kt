package coil3.gif

import android.content.ContentResolver.SCHEME_FILE
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
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowConversionToBitmap
import coil3.request.bitmapConfig
import coil3.request.crossfade
import coil3.request.transformations
import coil3.test.utils.assertIsSimilarTo
import coil3.test.utils.context
import coil3.test.utils.decodeBitmapAsset
import coil3.transform.CircleCropTransformation
import kotlin.test.assertIs
import kotlin.test.assertIsNot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AnimatedAndNormalTransformationTest {

    private lateinit var imageLoader: ImageLoader
    private lateinit var imageRequestBuilder: ImageRequest.Builder

    @Before
    fun before() {
        imageLoader = ImageLoader.Builder(context)
            .crossfade(false)
            .components {
                if (SDK_INT >= 28) {
                    add(AnimatedImageDecoder.Factory())
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
    fun animatedGifStillAnimated() = runTest {
        val imageRequest = imageRequestBuilder
            .data("$SCHEME_FILE:///android_asset/animated.gif")
            .build()
        val actual = imageLoader.execute(imageRequest)
        assertIs<SuccessResult>(actual)
        // Make sure this is still an animated result (has not been flattened to
        // apply CircleCropTransformation).
        assertIs<Animatable>(actual.image.asDrawable(context.resources))
    }

    @Test
    fun staticImageStillTransformed() = runTest {
        val expected = context.decodeBitmapAsset("normal_small_circle.png")
        val imageRequest = imageRequestBuilder
            .data("$SCHEME_FILE:///android_asset/normal_small.jpg")
            .build()
        val actual = imageLoader.execute(imageRequest)
        assertIs<SuccessResult>(actual)
        // Make sure this is not an animated result.
        assertIsNot<Animatable>(actual.image.asDrawable(context.resources))
        (actual.image as BitmapImage).bitmap.assertIsSimilarTo(expected)
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
