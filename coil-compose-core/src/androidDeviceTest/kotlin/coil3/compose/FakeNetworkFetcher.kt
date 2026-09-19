package coil3.compose

import androidx.annotation.RawRes
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.Uri
import coil3.compose.core.test.R
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.Buffer
import okio.BufferedSource

class FakeNetworkFetcher(
    private val url: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        return when (url.path) {
            "/image" -> SourceFetchResult(
                source = ImageSource(
                    source = rawResourceAsSource(R.drawable.sample),
                    fileSystem = options.fileSystem,
                ),
                mimeType = "image/jpeg",
                dataSource = DataSource.NETWORK,
            )
            "/blue" -> SourceFetchResult(
                source = ImageSource(
                    source = rawResourceAsSource(R.drawable.blue_rectangle),
                    fileSystem = options.fileSystem,
                ),
                mimeType = "image/png",
                dataSource = DataSource.NETWORK,
            )
            "/red" -> SourceFetchResult(
                source = ImageSource(
                    source = rawResourceAsSource(R.drawable.red_rectangle),
                    fileSystem = options.fileSystem,
                ),
                mimeType = "image/png",
                dataSource = DataSource.NETWORK,
            )
            else -> {
                error("404 unknown url: $url")
            }
        }
    }

    private fun rawResourceAsSource(
        @RawRes id: Int,
    ): BufferedSource {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        return Buffer().apply { readFrom(resources.openRawResource(id)) }
    }

    class Factory : Fetcher.Factory<Uri> {

        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            if (!isApplicable(data)) return null
            return FakeNetworkFetcher(
                url = data,
                options = options,
            )
        }

        private fun isApplicable(data: Uri): Boolean {
            return data.scheme == "http" || data.scheme == "https"
        }
    }
}
