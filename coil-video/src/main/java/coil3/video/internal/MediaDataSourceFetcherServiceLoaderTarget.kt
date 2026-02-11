package coil3.video.internal

import android.media.MediaDataSource
import coil3.util.FetcherServiceLoaderTarget
import coil3.video.MediaDataSourceFetcher

internal class MediaDataSourceFetcherServiceLoaderTarget : FetcherServiceLoaderTarget<MediaDataSource> {
    override fun factory() = MediaDataSourceFetcher.Factory()
    override fun type() = MediaDataSource::class
}
