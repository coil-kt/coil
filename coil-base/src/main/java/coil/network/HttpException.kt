@file:Suppress("MemberVisibilityCanBePrivate")

package coil.network

import coil.fetch.HttpUrlFetcher
import okhttp3.Response

/**
 * Exception for an unexpected, non-2xx HTTP response.
 *
 * @see HttpUrlFetcher
 */
class HttpException(val response: Response) : RuntimeException("HTTP ${response.code}: ${response.message}")
