package coil.fetch

import coil.bitmappool.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.network.HttpException
import coil.size.Size
import coil.util.await
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.Request

internal class HttpUrlFetcher(
    private val callFactory: Lazy<Call.Factory>
) : Fetcher<HttpUrl> {

    companion object {
        private val CACHE_CONTROL_FORCE_NETWORK_NO_CACHE = CacheControl.Builder().noCache().noStore().build()
        private val CACHE_CONTROL_NO_NETWORK_NO_CACHE = CacheControl.Builder().noCache().onlyIfCached().build()
    }

    override fun key(data: HttpUrl): String = data.toString()

    override suspend fun fetch(
        pool: BitmapPool,
        data: HttpUrl,
        size: Size,
        options: Options
    ): FetchResult {
        val request = Request.Builder().url(data)

        val networkRead = options.networkCachePolicy.readEnabled
        val diskRead = options.diskCachePolicy.readEnabled
        when {
            !networkRead && diskRead -> {
                request.cacheControl(CacheControl.FORCE_CACHE)
            }
            networkRead && !diskRead -> if (options.diskCachePolicy.writeEnabled) {
                request.cacheControl(CacheControl.FORCE_NETWORK)
            } else {
                request.cacheControl(CACHE_CONTROL_FORCE_NETWORK_NO_CACHE)
            }
            !networkRead && !diskRead -> {
                // This causes the request to fail with a 504 Unsatisfiable Request.
                request.cacheControl(CACHE_CONTROL_NO_NETWORK_NO_CACHE)
            }
        }

        val response = callFactory.value.newCall(request.build()).await()
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val body = checkNotNull(response.body()) { "Null response body!" }

        return SourceResult(
            source = body.source(),
            mimeType = body.contentType()?.toString(),
            dataSource = if (response.cacheResponse() != null) DataSource.DISK else DataSource.NETWORK
        )
    }
}
