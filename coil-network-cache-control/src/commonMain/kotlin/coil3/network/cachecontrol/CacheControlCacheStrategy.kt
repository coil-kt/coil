/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package coil3.network.cachecontrol

import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheResponse
import coil3.network.CacheStrategy
import coil3.network.CacheStrategy.Input
import coil3.network.CacheStrategy.Output
import coil3.network.NetworkRequest
import coil3.network.cachecontrol.internal.BROWSER_DATE_TIME_FORMAT
import coil3.network.cachecontrol.internal.CacheControl
import coil3.network.cachecontrol.internal.toNonNegativeInt
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * A [CacheStrategy] that uses the 'Cache-Control' response header and associated headers to
 * determine if a cached response should be used.
 *
 * See: https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control
 *
 * This implementation is based on OkHttp's `CacheStrategy`.
 */
@ExperimentalCoilApi
class CacheControlCacheStrategy(
    private val now: () -> Instant = Clock.System::now,
) : CacheStrategy {

    override suspend fun compute(input: Input): Output {
        return Computation(input.cacheResponse, input.networkRequest, now()).compute()
    }

    private class Computation(
        private val cacheResponse: CacheResponse,
        private val request: NetworkRequest,
        private val now: Instant,
    ) {
        private val responseCaching = CacheControl.parse(cacheResponse.responseHeaders)
        private val requestCaching = CacheControl.parse(request.headers)

        /** The server's time when the cached response was served, if known. */
        private var servedDate: Instant? = null
        private var servedDateString: String? = null

        /** The last modified date of the cached response, if known. */
        private var lastModified: Instant? = null
        private var lastModifiedString: String? = null

        /**
         * The expiration date of the cached response, if known. If both this field and the max age
         * are set, the max age is preferred.
         */
        private var expires: Instant? = null

        /**
         * Extension header set by OkHttp specifying the timestamp when the cached HTTP request was
         * first initiated.
         */
        private var sentRequestMillis = 0L

        /**
         * Extension header set by OkHttp specifying the timestamp when the cached HTTP response was
         * first received.
         */
        private var receivedResponseMillis = 0L

        /** Etag of the cached response. */
        private var etag: String? = null

        /** Age of the cached response. */
        private var ageSeconds = -1

        init {
            for ((name, values) in cacheResponse.responseHeaders.asMap()) {
                val value = values.firstOrNull() ?: continue
                when {
                    name.equals("Date", ignoreCase = true) -> {
                        servedDate = Instant.parse(value, BROWSER_DATE_TIME_FORMAT)
                        servedDateString = value
                    }
                    name.equals("Expires", ignoreCase = true) -> {
                        expires = Instant.parse(value, BROWSER_DATE_TIME_FORMAT)
                    }
                    name.equals("Last-Modified", ignoreCase = true) -> {
                        lastModified = Instant.parse(value, BROWSER_DATE_TIME_FORMAT)
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

        fun compute(): Output {
            // If this response shouldn't have been stored, it should never be used as a response
            // source. This check should be redundant as long as the persistence store is
            // well-behaved and the rules are constant.
            if (!isCacheable(responseCaching, requestCaching)) {
                return Output(request)
            }

            if (requestCaching.noCache || hasConditions(request)) {
                return Output(request)
            }

            val ageMillis = cacheResponseAge()
            var freshMillis = computeFreshnessLifetime()

            if (requestCaching.maxAgeSeconds != -1) {
                freshMillis = minOf(freshMillis, 1000L * requestCaching.maxAgeSeconds)
            }

            var minFreshMillis = 0L
            if (requestCaching.minFreshSeconds != -1) {
                minFreshMillis = 1000L * requestCaching.minFreshSeconds
            }

            var maxStaleMillis = 0L
            if (!responseCaching.mustRevalidate && requestCaching.maxStaleSeconds != -1) {
                maxStaleMillis = 1000L * requestCaching.maxStaleSeconds
            }

            if (!responseCaching.noCache && ageMillis + minFreshMillis < freshMillis + maxStaleMillis) {
                val headersBuilder = cacheResponse.responseHeaders.newBuilder()
                if (ageMillis + minFreshMillis >= freshMillis) {
                    headersBuilder.add("Warning", "110 HttpURLConnection \"Response is stale\"")
                }
                val oneDayMillis = 24 * 60 * 60 * 1000L
                if (ageMillis > oneDayMillis && isFreshnessLifetimeHeuristic()) {
                    headersBuilder.add("Warning", "113 HttpURLConnection \"Heuristic expiration\"")
                }
                return Output(cacheResponse.copy(responseHeaders = headersBuilder.build()))
            }

            // Find a condition to add to the request. If the condition is satisfied, the response
            // body will not be transmitted.
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
                else -> return Output(request) // No condition! Make a regular request.
            }

            val conditionalRequestHeaders = request.headers.newBuilder()
            conditionalRequestHeaders.add(conditionName, conditionValue!!)

            val conditionalRequest = request.copy(
                headers = conditionalRequestHeaders.build(),
            )
            return Output(cacheResponse, conditionalRequest)
        }

        /**
         * Returns true if computeFreshnessLifetime used a heuristic. If we used a heuristic to
         * serve a cached response older than 24 hours, we are required to attach a warning.
         */
        private fun isFreshnessLifetimeHeuristic(): Boolean {
            return responseCaching.maxAgeSeconds == -1 && expires == null
        }

        /**
         * Returns the number of milliseconds that the response was fresh for, starting from the
         * served date.
         */
        private fun computeFreshnessLifetime(): Long {
            if (responseCaching.maxAgeSeconds != -1) {
                return 1000L * responseCaching.maxAgeSeconds
            }

            val expires = expires
            if (expires != null) {
                val servedMillis = servedDate?.toEpochMilliseconds() ?: receivedResponseMillis
                val delta = expires.toEpochMilliseconds() - servedMillis
                return if (delta > 0L) delta else 0L
            }

            return 0L
        }

        /**
         * Returns the current age of the response, in milliseconds. The calculation is specified
         * by RFC 7234, 4.2.3 Calculating Age.
         */
        private fun cacheResponseAge(): Long {
            val servedDate = servedDate
            val apparentReceivedAge = if (servedDate != null) {
                maxOf(0, receivedResponseMillis - servedDate.toEpochMilliseconds())
            } else {
                0
            }

            val ageSeconds = ageSeconds
            val receivedAge = if (ageSeconds != -1) {
                maxOf(apparentReceivedAge, 1000L * ageSeconds)
            } else {
                apparentReceivedAge
            }

            val responseDuration = maxOf(0, receivedResponseMillis - sentRequestMillis)
            val residentDuration = maxOf(0, now.toEpochMilliseconds() - receivedResponseMillis)
            return receivedAge + responseDuration + residentDuration
        }

        /**
         * Returns true if the request contains conditions that save the server from sending a
         * response that the client has locally. When a request is enqueued with its own conditions,
         * the built-in response cache won't be used.
         */
        private fun hasConditions(request: NetworkRequest): Boolean {
            return request.headers["If-Modified-Since"] != null ||
                request.headers["If-None-Match"] != null
        }

        /** Returns true if the response can be stored to later serve another request. */
        private fun isCacheable(
            responseCaching: CacheControl,
            requestCaching: CacheControl,
        ): Boolean {
            return !responseCaching.noStore && !requestCaching.noStore
        }
    }
}
