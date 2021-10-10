package coil.network

import coil.util.Time
import coil.util.toNonNegativeInt
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import java.util.Date
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.max
import kotlin.math.min

/** Modified from OkHttp's `okhttp3.internal.cache.CacheStrategy`. */
internal class CacheStrategy private constructor(
    /** The request to send on the network, or null if this call doesn't use the network. */
    val networkRequest: Request?,
    /** The cached response to return or validate, or null if this call doesn't use a cache. */
    val cacheResponse: CacheResponse?
) {

    class Factory(
        private val request: Request,
        private val cacheResponse: CacheResponse?
    ) {

        /** The server's time when the cached response was served, if known. */
        private var servedDate: Date? = null
        private var servedDateString: String? = null

        /** The last modified date of the cached response, if known. */
        private var lastModified: Date? = null
        private var lastModifiedString: String? = null

        /**
         * The expiration date of the cached response, if known.
         * If both this field and the max age are set, the max age is preferred.
         */
        private var expires: Date? = null

        /** @see [Response.sentRequestAtMillis] */
        private var sentRequestMillis = 0L

        /** @see [Response.receivedResponseAtMillis] */
        private var receivedResponseMillis = 0L

        /** Etag of the cached response. */
        private var etag: String? = null

        /** Age of the cached response. */
        private var ageSeconds = -1

        init {
            if (cacheResponse != null) {
                sentRequestMillis = cacheResponse.sentRequestAtMillis
                receivedResponseMillis = cacheResponse.receivedResponseAtMillis
                val headers = cacheResponse.responseHeaders
                for (i in 0 until headers.size) {
                    val name = headers.name(i)
                    val value = headers.value(i)
                    when {
                        name.equals("Date", ignoreCase = true) -> {
                            servedDate = headers.getDate("Date")
                            servedDateString = value
                        }
                        name.equals("Expires", ignoreCase = true) -> {
                            expires = headers.getDate("Expires")
                        }
                        name.equals("Last-Modified", ignoreCase = true) -> {
                            lastModified = headers.getDate("Last-Modified")
                            lastModifiedString = value
                        }
                        name.equals("ETag", ignoreCase = true) -> {
                            etag = value
                        }
                        name.equals("Age", ignoreCase = true) -> {
                            ageSeconds = value.toNonNegativeInt(-1)
                        }
                    }
                }
            }
        }

        /** Returns a strategy to satisfy [request] using [cacheResponse]. */
        fun compute(): CacheStrategy {
            // No cached response.
            if (cacheResponse == null) {
                return CacheStrategy(request, null)
            }

            // Drop the cached response if it's missing a required handshake.
            if (request.isHttps && !cacheResponse.isTls) {
                return CacheStrategy(request, null)
            }

            // If this response shouldn't have been stored, it should never be used as a response
            // source. This check should be redundant as long as the persistence store is
            // well-behaved and the rules are constant.
            val responseCaching = cacheResponse.cacheControl()
            if (!isCacheable(request, cacheResponse)) {
                return CacheStrategy(request, null)
            }

            val requestCaching = request.cacheControl
            if (requestCaching.noCache || hasConditions(request)) {
                return CacheStrategy(request, null)
            }

            val ageMillis = cacheResponseAge()
            var freshMillis = computeFreshnessLifetime()

            if (requestCaching.maxAgeSeconds != -1) {
                freshMillis = min(freshMillis, SECONDS.toMillis(requestCaching.maxAgeSeconds.toLong()))
            }

            var minFreshMillis = 0L
            if (requestCaching.minFreshSeconds != -1) {
                minFreshMillis = SECONDS.toMillis(requestCaching.minFreshSeconds.toLong())
            }

            var maxStaleMillis = 0L
            if (!responseCaching.mustRevalidate && requestCaching.maxStaleSeconds != -1) {
                maxStaleMillis = SECONDS.toMillis(requestCaching.maxStaleSeconds.toLong())
            }

            if (!responseCaching.noCache && ageMillis + minFreshMillis < freshMillis + maxStaleMillis) {
                return CacheStrategy(null, cacheResponse)
            }

            // Find a condition to add to the request.
            // If the condition is satisfied, the response body will not be transmitted.
            val conditionName: String
            val conditionValue: String?
            when {
                etag != null -> {
                    conditionName = "If-None-Match"
                    conditionValue = etag
                }
                lastModified != null -> {
                    conditionName = "If-Modified-Since"
                    conditionValue = lastModifiedString
                }
                servedDate != null -> {
                    conditionName = "If-Modified-Since"
                    conditionValue = servedDateString
                }
                // No condition! Make a regular request.
                else -> return CacheStrategy(request, null)
            }

            val conditionalRequestHeaders = request.headers.newBuilder()
            conditionalRequestHeaders.add(conditionName, conditionValue!!)

            val conditionalRequest = request.newBuilder()
                .headers(conditionalRequestHeaders.build())
                .build()
            return CacheStrategy(conditionalRequest, cacheResponse)
        }

        /**
         * Returns the number of milliseconds that the response was fresh for,
         * starting from the served date.
         */
        private fun computeFreshnessLifetime(): Long {
            val responseCaching = cacheResponse!!.cacheControl()
            if (responseCaching.maxAgeSeconds != -1) {
                return SECONDS.toMillis(responseCaching.maxAgeSeconds.toLong())
            }

            val expires = expires
            if (expires != null) {
                val servedMillis = servedDate?.time ?: receivedResponseMillis
                val delta = expires.time - servedMillis
                return if (delta > 0L) delta else 0L
            }

            if (lastModified != null && request.url.query == null) {
                // As recommended by the HTTP RFC and implemented in Firefox, the max age of a
                // document should be defaulted to 10% of the document's age at the time it was
                // served. Default expiration dates aren't used for URIs containing a query.
                val servedMillis = servedDate?.time ?: sentRequestMillis
                val delta = servedMillis - lastModified!!.time
                return if (delta > 0L) delta / 10 else 0L
            }

            return 0L
        }

        /**
         * Returns the current age of the response, in milliseconds.
         * The calculation is specified by RFC 7234, 4.2.3 Calculating Age.
         */
        private fun cacheResponseAge(): Long {
            val servedDate = servedDate
            val apparentReceivedAge = if (servedDate != null) {
                max(0, receivedResponseMillis - servedDate.time)
            } else {
                0
            }

            val receivedAge = if (ageSeconds != -1) {
                max(apparentReceivedAge, SECONDS.toMillis(ageSeconds.toLong()))
            } else {
                apparentReceivedAge
            }

            val responseDuration = receivedResponseMillis - sentRequestMillis
            val residentDuration = Time.currentMillis() - receivedResponseMillis
            return receivedAge + responseDuration + residentDuration
        }

        /**
         * Returns true if the request contains conditions that save the server from sending a
         * response that the client has locally. When a request is enqueued with its own conditions,
         * the built-in response cache won't be used.
         */
        private fun hasConditions(request: Request): Boolean {
            return request.header("If-Modified-Since") != null ||
                request.header("If-None-Match") != null
        }
    }

    companion object {

        /** Returns true if the response can be stored to later serve another request. */
        fun isCacheable(request: Request, response: Response): Boolean {
            // A 'no-store' directive on request or response prevents the response from being cached.
            return !request.cacheControl.noStore && !response.cacheControl.noStore &&
                // Vary all responses cannot be cached.
                response.headers["Vary"] != "*"
        }

        /** Returns true if the response can be stored to later serve another request. */
        fun isCacheable(request: Request, response: CacheResponse): Boolean {
            // A 'no-store' directive on request or response prevents the response from being cached.
            return !request.cacheControl.noStore && !response.cacheControl().noStore &&
                // Vary all responses cannot be cached.
                response.responseHeaders["Vary"] != "*"
        }

        /** Combines cached headers with a network headers as defined by RFC 7234, 4.3.4. */
        fun combineHeaders(cachedHeaders: Headers, networkHeaders: Headers): Headers {
            val result = Headers.Builder()

            for (index in 0 until cachedHeaders.size) {
                val name = cachedHeaders.name(index)
                val value = cachedHeaders.value(index)
                if ("Warning".equals(name, ignoreCase = true) && value.startsWith("1")) {
                    // Drop 100-level freshness warnings.
                    continue
                }
                if (isContentSpecificHeader(name) ||
                    !isEndToEnd(name) ||
                    networkHeaders[name] == null) {
                    result.add(name, value)
                }
            }

            for (index in 0 until networkHeaders.size) {
                val fieldName = networkHeaders.name(index)
                if (!isContentSpecificHeader(fieldName) && isEndToEnd(fieldName)) {
                    result.add(fieldName, networkHeaders.value(index))
                }
            }

            return result.build()
        }

        /** Returns true if [name] is an end-to-end HTTP header, as defined by RFC 2616, 13.5.1. */
        private fun isEndToEnd(name: String): Boolean {
            return !"Connection".equals(name, ignoreCase = true) &&
                !"Keep-Alive".equals(name, ignoreCase = true) &&
                !"Proxy-Authenticate".equals(name, ignoreCase = true) &&
                !"Proxy-Authorization".equals(name, ignoreCase = true) &&
                !"TE".equals(name, ignoreCase = true) &&
                !"Trailers".equals(name, ignoreCase = true) &&
                !"Transfer-Encoding".equals(name, ignoreCase = true) &&
                !"Upgrade".equals(name, ignoreCase = true)
        }

        /** Returns true if [name] is content specific and therefore should always be used from cached headers. */
        private fun isContentSpecificHeader(name: String): Boolean {
            return "Content-Length".equals(name, ignoreCase = true) ||
                "Content-Encoding".equals(name, ignoreCase = true) ||
                "Content-Type".equals(name, ignoreCase = true)
        }
    }
}
