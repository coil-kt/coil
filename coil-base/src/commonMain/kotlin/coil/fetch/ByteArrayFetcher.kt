package coil.fetch

import coil.decode.DataSource
import coil.decode.ImageSource
import coil.request.Options
import okio.Buffer

internal class ByteArrayFetcher(
    private val data: ByteArray,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        return SourceFetchResult(
            source = ImageSource(Buffer().apply { write(data) }),
            mimeType = null,
            dataSource = DataSource.MEMORY,
        )
    }
}
