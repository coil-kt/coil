package coil3.network

/**
 * Exception for an unexpected, non-2xx HTTP response.
 *
 * @see NetworkFetcher
 */
class HttpException(val response: NetworkResponse) : RuntimeException("HTTP ${response.code}")
