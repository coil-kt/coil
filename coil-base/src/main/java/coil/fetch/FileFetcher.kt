package coil.fetch

import android.webkit.MimeTypeMap
import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.size.Size
import okio.buffer
import okio.source
import java.io.File

internal class FileFetcher : Fetcher<File> {

    override fun key(data: File) = "${data.path}:${data.lastModified()}"

    override suspend fun fetch(
        pool: BitmapPool,
        data: File,
        size: Size,
        options: Options
    ): FetchResult {
        return SourceResult(
            source = data.source().buffer(),
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(data.extension),
            dataSource = DataSource.DISK
        )
    }
}
