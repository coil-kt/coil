package coil3.fetch

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.request.Options
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import okio.Buffer

/**
 * Fetches data URIs: http://www.ietf.org/rfc/rfc2397.txt
 */
internal class DataUriFetcher(
    private val uri: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val tagIndex = uri.toString().indexOf(BASE64_TAG)
        check(tagIndex != -1) { "invalid data uri: $uri" }

        val pathStartIndex = uri.toString().indexOf(':')
        check(pathStartIndex != -1) { "invalid data uri: $uri" }

        val mimeType = uri.toString().substring(pathStartIndex + 1, tagIndex)

        @OptIn(ExperimentalEncodingApi::class)
        val data = Base64.decode(
            source = uri.toString(),
            startIndex = tagIndex + BASE64_TAG.length,
        )

        return SourceFetchResult(
            source = ImageSource(
                source = Buffer().apply { write(data) },
                fileSystem = options.fileSystem,
            ),
            mimeType = mimeType,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            if (data.scheme != "data") return null
            return DataUriFetcher(data, options)
        }
    }

    private companion object {
        private const val BASE64_TAG = ";base64,"
    }
}
