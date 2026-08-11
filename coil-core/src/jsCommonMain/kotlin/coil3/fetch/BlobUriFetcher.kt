package coil3.fetch

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.request.Options
import okio.Buffer

/**
 * Fetches images from blob URLs created by `URL.createObjectURL` on the web
 * (e.g. "blob:http://localhost:8081/a3953f30-e6ef-47b6-8750-731ce8afe0f8").
 */
internal class BlobUriFetcher(
    private val uri: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val result = fetchBlob(uri.toString())

        return SourceFetchResult(
            source = ImageSource(
                source = Buffer().apply { write(result.bytes) },
                fileSystem = options.fileSystem,
            ),
            mimeType = result.mimeType,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            if (!data.toString().startsWith("blob:")) return null
            return BlobUriFetcher(data, options)
        }
    }
}

/** Read the data referenced by a blob [url] using the browser's `fetch` API. */
internal expect suspend fun fetchBlob(url: String): BlobFetchResult

internal class BlobFetchResult(
    val bytes: ByteArray,
    val mimeType: String?,
)
