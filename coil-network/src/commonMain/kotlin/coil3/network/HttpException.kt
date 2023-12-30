package coil3.network

import coil3.fetch.NetworkFetcher
import io.ktor.client.statement.HttpResponse

/**
 * Exception for an unexpected, non-2xx HTTP response.
 *
 * @see NetworkFetcher
 */
class HttpException(val response: HttpResponse) : RuntimeException("HTTP ${response.status}")
