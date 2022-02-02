package coil.fetch

import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.request.Options
import okio.Path.Companion.toOkioPath
import java.io.File

internal class FileFetcher(private val data: File) : Fetcher {
    override suspend fun fetch(): FetchResult {
        return SourceResult(
            source = ImageSource(file = data.toOkioPath()),
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(data.extension),
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<File> {

        override fun create(data: File, options: Options, imageLoader: ImageLoader): Fetcher {
            return FileFetcher(data)
        }
    }
}
