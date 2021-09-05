@file:JvmName("OkHttpClients")

package coil.network

import android.content.Context
import coil.ImageLoader
import coil.util.CoilUtils
import coil.util.removeIfIndices
import java.io.File
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.Source

/**
 * A convenience function to set the default image loader disk cache for this [OkHttpClient].
 *
 * NOTE: You should set this **after** adding any [Interceptor]s.
 */
fun OkHttpClient.Builder.imageLoaderDiskCache(context: Context) =
    imageLoaderDiskCache(CoilUtils.createDiskCache(context))

/**
 * Sets the disk cache for this [OkHttpClient] and adds extensions so an [ImageLoader] can
 * read its disk cache.
 *
 * NOTE: You should set this **after** adding any [Interceptor]s.
 *
 * @param diskCache The disk cache to use with this [OkHttpClient].
 */
fun OkHttpClient.Builder.imageLoaderDiskCache(diskCache: Cache?) = apply {
    cache(diskCache)
    interceptors().removeIfIndices { it is DiskCacheInterceptor }
    networkInterceptors().removeIfIndices { it is InexhaustibleSourceInterceptor }
    if (diskCache != null) {
        interceptors().add(DiskCacheInterceptor(diskCache))
        networkInterceptors().add(0, InexhaustibleSourceInterceptor())
    }
}

/**
 * Tags [Response]s with their associated disk cache file.
 * This should be the last non-network interceptor in the chain as it relies on
 * implementation details of the [Cache] class to determine the file name.
 */
private class DiskCacheInterceptor(private val diskCache: Cache) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val request = response.request
        val cacheFile = CoilUtils.getDiskCacheFile(diskCache, request.url)
        return response.newBuilder()
            .request(request.newBuilder().cacheFile(cacheFile).build())
            .build()
    }
}

/** Use a private class so its tag is guaranteed not to be overwritten. */
private class CacheFile(@JvmField val file: File)

/** Set the cache file on disk for this request. */
private fun Request.Builder.cacheFile(file: File) =
    tag(CacheFile::class.java, CacheFile(file))

/** Get the cache file on disk for this response. */
internal val Response.cacheFile: File?
    get() = request.tag(CacheFile::class.java)?.file

/**
 * Wraps the [Response] body's source with [InexhaustibleSource].
 */
private class InexhaustibleSourceInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body ?: return response
        if (response.inexhaustibleSource != null) return response

        val source = InexhaustibleSource(body.source())
        val request = response.request.newBuilder()
            .inexhaustibleSource(source)
            .build()
        return response.newBuilder()
            .request(request)
            .body(object : ResponseBody() {
                override fun source() = source
                override fun contentType() = body.contentType()
                override fun contentLength() = body.contentLength()
                override fun close() = body.close()
            })
            .build()
    }
}

/**
 * Wraps [delegate] so it returns 0 instead of -1 from [Source.read] if [isEnabled] is `true`.
 */
internal class InexhaustibleSource(
    private val delegate: BufferedSource
) : BufferedSource by delegate {

    var isEnabled = false
    var isExhausted = false
        private set

    override fun read(sink: Buffer, byteCount: Long): Long {
        val bytesRead = delegate.read(sink, byteCount)
        if (bytesRead == -1L) {
            isExhausted = true
            if (isEnabled) return 0
        }
        return bytesRead
    }
}

/** Set the cache file on disk for this request. */
private fun Request.Builder.inexhaustibleSource(source: InexhaustibleSource) =
    tag(InexhaustibleSource::class.java, source)

/** Get the cache file on disk for this response. */
internal val Response.inexhaustibleSource: InexhaustibleSource?
    get() = request.tag(InexhaustibleSource::class.java)

/**
 * Fail loudly if it can be determined that this is an [OkHttpClient]
 * with a [Cache] that was built without calling [imageLoaderDiskCache].
 */
internal fun Call.Factory.assertHasDiskCacheInterceptor() {
    if (this !is OkHttpClient || cache == null) return
    check(interceptors.any { it is DiskCacheInterceptor } &&
        networkInterceptors.any { it is InexhaustibleSourceInterceptor }) {
        "The ImageLoader is unable to read the disk cache of the OkHttpClient provided to it." +
            "Set `OkHttpClient.Builder.imageLoaderDiskCache` to fix this."
    }
}
