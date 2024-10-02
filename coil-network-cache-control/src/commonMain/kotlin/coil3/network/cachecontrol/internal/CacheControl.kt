/*
 * Copyright (C) 2019 Square, Inc.
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
package coil3.network.cachecontrol.internal

import coil3.network.NetworkHeaders

internal class CacheControl private constructor(
    /**
     * In a response, this field's name "no-cache" is misleading. It doesn't prevent us from caching
     * the response; it only means we have to validate the response with the origin server before
     * returning it. We can do this with a conditional GET.
     *
     * In a request, it means do not use a cache to satisfy the request.
     */
    val noCache: Boolean,
    /** If true, this response should not be cached. */
    val noStore: Boolean,
    /** The duration past the response's served date that it can be served without validation. */
    val maxAgeSeconds: Int,
    /**
     * The "s-maxage" directive is the max age for shared caches. Not to be confused with "max-age"
     * for non-shared caches, As in Firefox and Chrome, this directive is not honored by this cache.
     */
    val sMaxAgeSeconds: Int,
    val isPrivate: Boolean,
    val isPublic: Boolean,
    val mustRevalidate: Boolean,
    val maxStaleSeconds: Int,
    val minFreshSeconds: Int,
    /**
     * This field's name "only-if-cached" is misleading. It actually means "do not use the network".
     * It is set by a client who only wants to make a request if it can be fully satisfied by the
     * cache. Cached responses that would require validation (ie. conditional gets) are not permitted
     * if this header is set.
     */
    val onlyIfCached: Boolean,
    val noTransform: Boolean,
    val immutable: Boolean,
    val headerValue: String?,
) {
    companion object {
        fun parse(headers: NetworkHeaders): CacheControl {
            var noCache = false
            var noStore = false
            var maxAgeSeconds = -1
            var sMaxAgeSeconds = -1
            var isPrivate = false
            var isPublic = false
            var mustRevalidate = false
            var maxStaleSeconds = -1
            var minFreshSeconds = -1
            var onlyIfCached = false
            var noTransform = false
            var immutable = false

            var canUseHeaderValue = true
            var headerValue: String? = null

            loop@ for ((name, values) in headers.asMap()) {
                val value = values.firstOrNull() ?: continue

                when {
                    name.equals("Cache-Control", ignoreCase = true) -> {
                        if (headerValue != null) {
                            // Multiple cache-control headers means we can't use the raw value.
                            canUseHeaderValue = false
                        } else {
                            headerValue = value
                        }
                    }
                    name.equals("Pragma", ignoreCase = true) -> {
                        // Might specify additional cache-control params. We invalidate just in case.
                        canUseHeaderValue = false
                    }
                    else -> {
                        continue@loop
                    }
                }

                var pos = 0
                while (pos < value.length) {
                    val tokenStart = pos
                    pos = value.indexOfElement("=,;", pos)
                    val directive = value.substring(tokenStart, pos).trim()
                    val parameter: String?

                    if (pos == value.length || value[pos] == ',' || value[pos] == ';') {
                        pos++ // Consume ',' or ';' (if necessary).
                        parameter = null
                    } else {
                        pos++ // Consume '='.
                        pos = value.indexOfNonWhitespace(pos)

                        if (pos < value.length && value[pos] == '\"') {
                            // Quoted string.
                            pos++ // Consume '"' open quote.
                            val parameterStart = pos
                            pos = value.indexOf('"', pos)
                            parameter = value.substring(parameterStart, pos)
                            pos++ // Consume '"' close quote (if necessary).
                        } else {
                            // Unquoted string.
                            val parameterStart = pos
                            pos = value.indexOfElement(",;", pos)
                            parameter = value.substring(parameterStart, pos).trim()
                        }
                    }

                    when {
                        "no-cache".equals(directive, ignoreCase = true) -> {
                            noCache = true
                        }
                        "no-store".equals(directive, ignoreCase = true) -> {
                            noStore = true
                        }
                        "max-age".equals(directive, ignoreCase = true) -> {
                            maxAgeSeconds = parameter.toNonNegativeInt(-1)
                        }
                        "s-maxage".equals(directive, ignoreCase = true) -> {
                            sMaxAgeSeconds = parameter.toNonNegativeInt(-1)
                        }
                        "private".equals(directive, ignoreCase = true) -> {
                            isPrivate = true
                        }
                        "public".equals(directive, ignoreCase = true) -> {
                            isPublic = true
                        }
                        "must-revalidate".equals(directive, ignoreCase = true) -> {
                            mustRevalidate = true
                        }
                        "max-stale".equals(directive, ignoreCase = true) -> {
                            maxStaleSeconds = parameter.toNonNegativeInt(Int.MAX_VALUE)
                        }
                        "min-fresh".equals(directive, ignoreCase = true) -> {
                            minFreshSeconds = parameter.toNonNegativeInt(-1)
                        }
                        "only-if-cached".equals(directive, ignoreCase = true) -> {
                            onlyIfCached = true
                        }
                        "no-transform".equals(directive, ignoreCase = true) -> {
                            noTransform = true
                        }
                        "immutable".equals(directive, ignoreCase = true) -> {
                            immutable = true
                        }
                    }
                }
            }

            if (!canUseHeaderValue) {
                headerValue = null
            }

            return CacheControl(
                noCache = noCache,
                noStore = noStore,
                maxAgeSeconds = maxAgeSeconds,
                sMaxAgeSeconds = sMaxAgeSeconds,
                isPrivate = isPrivate,
                isPublic = isPublic,
                mustRevalidate = mustRevalidate,
                maxStaleSeconds = maxStaleSeconds,
                minFreshSeconds = minFreshSeconds,
                onlyIfCached = onlyIfCached,
                noTransform = noTransform,
                immutable = immutable,
                headerValue = headerValue,
            )
        }
    }
}
