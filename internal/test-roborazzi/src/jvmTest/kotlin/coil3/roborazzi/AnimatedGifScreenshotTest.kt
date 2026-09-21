package coil3.roborazzi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.decode.ImageSource
import coil3.gif.AnimatedSkiaImage
import coil3.gif.AnimatedSkiaImageDecoder
import coil3.request.Options
import coil3.test.FakeImageLoaderEngine
import coil3.test.utils.FakeTimeSource
import coil3.test.utils.context
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class AnimatedGifScreenshotTest {

    @Test
    fun firstFrame() = runTest {
        capture(elapsed = ZERO)
    }

    @Test
    fun thirdFrame() = runTest {
        capture(elapsed = 800.milliseconds)
    }

    private suspend fun TestScope.capture(elapsed: Duration) {
        val timeSource = FakeTimeSource()
        val image = decodeAnimatedGif(timeSource)
        assertFalse(image.isRunning())
        val engine = FakeImageLoaderEngine(image)
        val imageLoader = ImageLoader.Builder(context)
            .components { add(engine) }
            .build()

        try {
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

                if (elapsed > ZERO) {
                    // Start playback through Compose's first draw, then let the decoder's timer
                    // invalidate the UI. A manual image.draw() would hide invalidation bugs.
                    onRoot().captureToImage()
                    assertTrue(image.isRunning())
                    testScheduler.runCurrent()
                    timeSource.advanceBy(elapsed)
                    testScheduler.advanceTimeBy(elapsed.inWholeMilliseconds)
                    testScheduler.runCurrent()
                    Snapshot.sendApplyNotifications()
                    waitForIdle()
                }

                onRoot().captureRoboImage()
                assertTrue(image.isRunning())
            }
        } finally {
            try {
                imageLoader.shutdown()
            } finally {
                image.close()
            }
        }
    }

    private suspend fun decodeAnimatedGif(timeSource: FakeTimeSource): AnimatedSkiaImage {
        val source = FileSystem.RESOURCES.source("animated_infinite.gif".toPath()).buffer()
        val decoder = AnimatedSkiaImageDecoder(
            source = ImageSource(source, FileSystem.RESOURCES),
            options = Options(context),
            bufferedFramesCount = AnimatedSkiaImageDecoder.Factory.DEFAULT_BUFFERED_FRAMES_COUNT,
            timeSource = timeSource,
        )
        return assertIs<AnimatedSkiaImage>(decoder.decode().image)
    }
}
