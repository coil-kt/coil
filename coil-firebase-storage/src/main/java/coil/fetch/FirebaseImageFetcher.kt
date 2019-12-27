package coil.fetch

import android.webkit.MimeTypeMap.getSingleton
import coil.bitmappool.BitmapPool
import coil.decode.DataSource.NETWORK
import coil.decode.Options
import coil.size.Size
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import okio.buffer
import okio.source

/**
 * A [Fetcher] that uses [FirebaseStorage](https://firebase.google.com/products/storage) to to download images.
 */
class FirebaseImageFetcher : Fetcher<StorageReference> {

    override fun key(data: StorageReference) = data.toString()

    override suspend fun fetch(
        pool: BitmapPool,
        data: StorageReference,
        size: Size,
        options: Options
    ) = SourceResult(
        source = data.stream.await().stream.source().buffer(),
        mimeType = getSingleton().getMimeTypeFromExtension(data.path.substringAfterLast('.', missingDelimiterValue = "")),
        dataSource = NETWORK
    )
}
