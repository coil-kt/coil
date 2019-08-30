package coil.fetch

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import coil.bitmappool.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.size.Size
import okio.buffer
import okio.source

internal class ContentUriFetcher(private val context: Context) : Fetcher<Uri> {

    override fun handles(data: Uri) = data.scheme == ContentResolver.SCHEME_CONTENT

    override fun key(data: Uri) = data.toString()

    override suspend fun fetch(
        pool: BitmapPool,
        data: Uri,
        size: Size,
        options: Options
    ): FetchResult {
        return SourceResult(
            source = checkNotNull(context.contentResolver.openInputStream(data)).source().buffer(),
            mimeType = context.contentResolver.getType(data),
            dataSource = DataSource.DISK
        )
    }
}
