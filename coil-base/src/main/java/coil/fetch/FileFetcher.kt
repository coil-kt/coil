package coil.fetch

import android.webkit.MimeTypeMap
import coil.decode.DataSource
import coil.decode.Options
import okio.buffer
import okio.source
import java.io.File

internal class FileFetcher(private val addLastModifiedToFileCacheKey: Boolean) : Fetcher<File> {

    override fun cacheKey(data: File): String {
        return if (addLastModifiedToFileCacheKey) "${data.path}:${data.lastModified()}" else data.path
    }

    override suspend fun fetch(data: File, options: Options): FetchResult {
        return SourceResult(
            source = data.source().buffer(),
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(data.extension),
            dataSource = DataSource.DISK
        )
    }
}
