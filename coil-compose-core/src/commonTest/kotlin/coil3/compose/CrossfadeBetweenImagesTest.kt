package coil3.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImagePainter.State
import coil3.decode.DataSource
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.test.utils.FakeImage
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Crossfading between two loaded images (#3473). With the opt-in, the previous image is the
 * [State.Loading] painter, so the crossfade runs from it to the new result. The cases vary the
 * new result's [DataSource] and whether `useExistingImageAsPlaceholder` is set.
 */
class CrossfadeBetweenImagesTest : RobolectricTest() {

    private val previousImage: Painter = ColorPainter(Color.Red)
    private val newImage: Painter = ColorPainter(Color.Blue)

    /** Control: a disk-sourced result crossfades from the previous image. */
    @Test
    fun crossfadeBetweenImages_fromDisk_isApplied() {
        val crossfade = maybeNewCrossfadePainter(
            previous = State.Loading(previousImage),
            current = successState(DataSource.DISK, useExistingImage = true),
            contentScale = ContentScale.Fit,
        )

        val painter = assertNotNull(crossfade)
        assertSame(newImage, painter.end)
    }

    /** A cache hit should still crossfade from the previous image when opted in. */
    @Test
    fun crossfadeBetweenImages_fromMemoryCache_withOptIn_isApplied() {
        val crossfade = maybeNewCrossfadePainter(
            previous = State.Loading(previousImage),
            current = successState(DataSource.MEMORY_CACHE, useExistingImage = true),
            contentScale = ContentScale.Fit,
        )

        val painter = assertNotNull(
            crossfade,
            "Expected a crossfade from the previous image to the memory-cache result.",
        )
        assertSame(newImage, painter.end)
    }

    /** Without the opt-in, cache hits keep swapping instantly (no re-animation on scroll). */
    @Test
    fun crossfadeBetweenImages_fromMemoryCache_withoutOptIn_isNotApplied() {
        val crossfade = maybeNewCrossfadePainter(
            previous = State.Loading(previousImage),
            current = successState(DataSource.MEMORY_CACHE, useExistingImage = false),
            contentScale = ContentScale.Fit,
        )

        assertNull(crossfade)
    }

    private fun successState(
        dataSource: DataSource,
        useExistingImage: Boolean,
    ) = State.Success(
        painter = newImage,
        result = SuccessResult(
            image = FakeImage(),
            request = ImageRequest.Builder(context)
                .data("https://example.com/image")
                .crossfade(true)
                .useExistingImageAsPlaceholder(useExistingImage)
                .build(),
            dataSource = dataSource,
        ),
    )
}
