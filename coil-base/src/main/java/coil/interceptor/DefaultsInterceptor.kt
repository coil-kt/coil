package coil.interceptor

import coil.DefaultRequestOptions
import coil.annotation.ExperimentalCoilApi
import coil.request.ImageRequest
import coil.request.RequestResult
import coil.util.BIT_ALLOW_HARDWARE
import coil.util.BIT_ALLOW_RGB565
import coil.util.BIT_BITMAP_CONFIG
import coil.util.BIT_DISK_CACHE_POLICY
import coil.util.BIT_DISPATCHER
import coil.util.BIT_ERROR
import coil.util.BIT_FALLBACK
import coil.util.BIT_MEMORY_CACHE_POLICY
import coil.util.BIT_NETWORK_CACHE_POLICY
import coil.util.BIT_PLACEHOLDER
import coil.util.BIT_PRECISION
import coil.util.BIT_TRANSITION

/** Applies [DefaultRequestOptions] to an [ImageRequest]. */
@OptIn(ExperimentalCoilApi::class)
internal class DefaultsInterceptor(private val defaults: DefaultRequestOptions) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): RequestResult {
        val request = chain.request
        val writes = request.writes
        val builder = request.newBuilder()
        if (!writes[BIT_DISPATCHER]) builder.dispatcher(defaults.dispatcher)
        if (!writes[BIT_TRANSITION]) builder.transition(defaults.transition)
        if (!writes[BIT_PRECISION]) builder.precision(defaults.precision)
        if (!writes[BIT_BITMAP_CONFIG]) builder.bitmapConfig(defaults.bitmapConfig)
        if (!writes[BIT_ALLOW_HARDWARE]) builder.allowHardware(defaults.allowHardware)
        if (!writes[BIT_ALLOW_RGB565]) builder.allowRgb565(defaults.allowRgb565)
        if (!writes[BIT_PLACEHOLDER]) builder.placeholder(defaults.placeholder)
        if (!writes[BIT_ERROR]) builder.error(defaults.error)
        if (!writes[BIT_FALLBACK]) builder.fallback(defaults.fallback)
        if (!writes[BIT_MEMORY_CACHE_POLICY]) builder.memoryCachePolicy(defaults.memoryCachePolicy)
        if (!writes[BIT_DISK_CACHE_POLICY]) builder.diskCachePolicy(defaults.diskCachePolicy)
        if (!writes[BIT_NETWORK_CACHE_POLICY]) builder.networkCachePolicy(defaults.networkCachePolicy)
        return chain.proceed(builder.build())
    }
}
