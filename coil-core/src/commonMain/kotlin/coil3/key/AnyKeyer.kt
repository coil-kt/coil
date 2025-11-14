package coil3.key

import coil3.request.Options

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> CacheEnabledKeyer(): Keyer<T> = CacheEnabledKeyer as Keyer<T>

private object CacheEnabledKeyer : Keyer<Any> {
    override fun key(data: Any, options: Options): String {
        return data.toString()
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> CacheDisabledKeyer(): Keyer<T> = CacheDisabledKeyer as Keyer<T>

private object CacheDisabledKeyer : Keyer<Any> {
    override fun key(data: Any, options: Options): String? {
        return null
    }
}
