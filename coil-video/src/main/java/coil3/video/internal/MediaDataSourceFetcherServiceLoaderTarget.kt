package coil3.video.internal

import android.media.MediaDataSource
import android.os.Build.VERSION.SDK_INT
import coil3.util.FetcherServiceLoaderTarget
import coil3.video.MediaDataSourceFetcher

internal class MediaDataSourceFetcherServiceLoaderTarget : FetcherServiceLoaderTarget<MediaDataSource> {
    override fun factory() = if (SDK_INT >= 23) MediaDataSourceFetcher.Factory() else null
    override fun type() = if (SDK_INT >= 23) MediaDataSource::class else null
}
