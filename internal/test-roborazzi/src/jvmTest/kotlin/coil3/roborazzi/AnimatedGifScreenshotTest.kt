package coil3.roborazzi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import coil3.Image
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.gif.AnimatedSkiaImageDecoder
import coil3.request.Options
import coil3.test.FakeImageLoaderEngine
import coil3.test.utils.FakeTimeSource
import coil3.test.utils.context
import coil3.toBitmap
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

@OptIn(ExperimentalTestApi::class)
class AnimatedGifScreenshotTest {

    @Test
    fun firstFrame() {
        capture(animatedGifAt(ZERO))
    }

    @Test
    fun thirdFrame() {
        capture(animatedGifAt(800.milliseconds))
    }

    private fun capture(image: Image) {
        val engine = FakeImageLoaderEngine(image)
        val imageLoader = ImageLoader.Builder(context)
            .components { add(engine) }
            .build()

        runComposeUiTest {
            setContent {
                AsyncImage(
                    model = "animated.gif",
                    contentDescription = null,
                    imageLoader = imageLoader,
                    contentScale = ContentScale.None,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                )
            }

            onRoot().captureRoboImage()
        }
    }

    private fun animatedGifAt(elapsed: Duration): Image {
        val timeSource = FakeTimeSource()
        val image = runBlocking(Dispatchers.Default) {
            val source = FileSystem.RESOURCES.source("animated_infinite.gif".toPath()).buffer()
            val result = SourceFetchResult(
                source = ImageSource(source, FileSystem.RESOURCES),
                mimeType = "image/gif",
                dataSource = DataSource.DISK,
            )
            val decoder = assertNotNull(
                AnimatedSkiaImageDecoder.Factory(timeSource = timeSource).create(
                    result = result,
                    options = Options(context),
                    imageLoader = ImageLoader(context),
                ),
            )
            assertNotNull(decoder.decode()).image
        }

        image.toBitmap().close()
        timeSource.advanceBy(elapsed)
        return image
    }
}
