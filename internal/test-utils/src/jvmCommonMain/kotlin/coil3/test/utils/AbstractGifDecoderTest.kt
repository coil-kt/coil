package coil3.test.utils

import coil3.Bitmap
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.Scale
import coil3.size.Size
import coil3.toBitmap
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem

abstract class AbstractGifDecoderTest(
    private val decoderFactory: Decoder.Factory,
) {

    @Test
    fun basic() = runTest {
        val source = FileSystem.RESOURCES.source("animated.gif".toPath()).buffer()
        val options = Options(
            context = context,
            size = Size(300, 300),
            scale = Scale.FIT,
        )

        val result = assertNotNull(
            decoderFactory.create(
                result = source.asSourceResult(),
                options = options,
                imageLoader = ImageLoader(context),
            )?.decode(),
        )

        (1..5).forEach { frameIndex ->
            val expected: Bitmap = decodeBitmapResource("animated_$frameIndex.png")
            val actual: Bitmap = result.image.toBitmap(frameIndex)
            actual.assertIsSimilarTo(expected)
        }
    }

    fun BufferedSource.asSourceResult(
        mimeType: String? = null,
        dataSource: DataSource = DataSource.DISK,
    ) = SourceFetchResult(
        source = ImageSource(this, FakeFileSystem()),
        mimeType = mimeType,
        dataSource = dataSource,
    )
}
