package coil3.network.okhttp

import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.test.utils.context
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient

class Issue3427AndroidTest {

    @Test
    fun issue3427SampleUrlsDoNotSocketTimeout() = runTest {
        val results = buildList {
            for (url in URLS) {
                add(runCase("coil_default", url) { loadWithCoil(url, diskCache = true) })
                add(runCase("coil_no_disk_cache", url) { loadWithCoil(url, diskCache = false) })
                add(runCase("url_connection", url) { loadWithUrlConnection(url) })
                add(runCase("url_connection_manual_redirects", url) {
                    loadWithUrlConnection(url, followRedirects = true)
                })
            }
        }

        println(results.joinToString(separator = "\n"))
        assertTrue(results.none { it.outcome is Outcome.SocketTimeout }, results.joinToString(separator = "\n"))
    }

    private suspend fun loadWithCoil(
        url: String,
        diskCache: Boolean,
    ): String {
        val imageLoader = ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = {
                            OkHttpClient.Builder()
                                .readTimeout(30, TimeUnit.SECONDS)
                                .build()
                        },
                    ),
                )
            }
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(if (diskCache) CachePolicy.ENABLED else CachePolicy.DISABLED)
            .build()

        val request = ImageRequest.Builder(context)
            .data(url)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(if (diskCache) CachePolicy.ENABLED else CachePolicy.DISABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()

        val result = imageLoader.execute(request)
        if (result is ErrorResult) throw result.throwable
        return result::class.simpleName.orEmpty()
    }

    private fun loadWithUrlConnection(
        url: String,
        followRedirects: Boolean = false,
    ): String {
        var currentUrl = URL(url)
        var redirects = 0
        while (true) {
            val result = loadWithUrlConnection(currentUrl)
            if (!followRedirects) return result.summary

            val location = result.redirectLocation ?: return "${result.summary}, redirects=$redirects"
            currentUrl = URL(currentUrl, location)
            redirects++
            check(redirects <= 5) { "Too many redirects." }
        }
    }

    private fun loadWithUrlConnection(url: URL): UrlConnectionResult {
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        return try {
            val code = connection.responseCode
            val redirectLocation = connection.getHeaderField("Location").takeIf { code in 300 until 400 }
            val inputStream = if (code >= 400) connection.errorStream else connection.inputStream
            inputStream.use { input ->
                val output = ByteArrayOutputStream()
                input?.copyTo(output)
                UrlConnectionResult(
                    summary = "HTTP $code, bytes=${output.size()}, finalUrl=${connection.url}",
                    redirectLocation = redirectLocation,
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    private inline fun runCase(
        name: String,
        url: String,
        block: () -> String,
    ): CaseResult {
        var outcome: Outcome = Outcome.Failure("not executed")
        val elapsedMillis = measureTimeMillis {
            outcome = try {
                Outcome.Success(block())
            } catch (e: SocketTimeoutException) {
                Outcome.SocketTimeout("${e::class.qualifiedName}: ${e.message}")
            } catch (e: Throwable) {
                Outcome.Failure("${e::class.qualifiedName}: ${e.message}")
            }
        }
        return CaseResult(name, url, elapsedMillis, outcome)
    }

    private data class CaseResult(
        val name: String,
        val url: String,
        val elapsedMillis: Long,
        val outcome: Outcome,
    ) {
        override fun toString(): String {
            return "$name | $url | ${elapsedMillis}ms | $outcome"
        }
    }

    private sealed interface Outcome {
        data class Success(val message: String) : Outcome {
            override fun toString() = "OK: $message"
        }

        data class SocketTimeout(val message: String) : Outcome {
            override fun toString() = "SOCKET_TIMEOUT: $message"
        }

        data class Failure(val message: String) : Outcome {
            override fun toString() = "FAIL: $message"
        }
    }

    private data class UrlConnectionResult(
        val summary: String,
        val redirectLocation: String?,
    )

    private companion object {
        val URLS = listOf(
            "http://spaceflightnow.com/wp-content/uploads/2026/05/20260508-Testing-Link-Vibration-tests-2.jpg",
            "https://www.nasaspaceflight.com/wp-content/uploads/2026/05/NSF-2026-05-08-23-42-01-222.jpg",
        )
    }
}
