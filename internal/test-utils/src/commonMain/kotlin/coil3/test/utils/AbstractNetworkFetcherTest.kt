package coil3.test.utils

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.SourceFetchResult
import coil3.network.DeDupeInFlightRequestStrategy
import coil3.network.InFlightRequestStrategy
import coil3.network.NetworkFetcher
import coil3.request.Options
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.launch
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.blackholeSink
import okio.fakefilesystem.FakeFileSystem

abstract class AbstractNetworkFetcherTest : RobolectricTest() {

    lateinit var fileSystem: FakeFileSystem
    lateinit var diskCache: DiskCache
    lateinit var imageLoader: ImageLoader

    @BeforeTest
    open fun before() {
        fileSystem = FakeFileSystem()
        diskCache = DiskCache.Builder()
            .directory(fileSystem.workingDirectory)
            .fileSystem(fileSystem)
            .maxSizeBytes(Long.MAX_VALUE)
            .build()
        imageLoader = ImageLoader.Builder(context)
            .diskCache(diskCache)
            .fileSystem(diskCache.fileSystem)
            .build()
    }

    @AfterTest
    open fun after() {
        imageLoader.shutdown()
        diskCache.shutdown()
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun basicNetworkFetch() = runTestAsync {
        val expectedSize = 1_000
        val result = newFetcher(responseBody = ByteArray(expectedSize).toByteString()).fetch()

        assertIs<SourceFetchResult>(result)
        val actualSize = result.source.use { it.source().readAll(blackholeSink()) }
        assertEquals(expectedSize.toLong(), actualSize)
    }

    @Test
    fun mimeTypeIsParsedCorrectlyFromContentType() = runTestAsync {
        val expectedSize = 1_000
        val fetcher = newFetcher(responseBody = ByteArray(expectedSize).toByteString())

        val url1 = "https://example.com/image.jpg"
        val type1 = "image/svg+xml"
        assertEquals(type1, fetcher.getMimeType(url1, type1))

        val url2 = "https://www.example.com/image.svg"
        val type2: String? = null
        assertEquals("image/svg+xml", fetcher.getMimeType(url2, type2))

        val url3 = "https://www.example.com/image"
        val type3 = "image/svg+xml;charset=utf-8"
        assertEquals("image/svg+xml", fetcher.getMimeType(url3, type3))

        val url4 = "https://www.example.com/image.svg"
        val type4 = "text/plain"
        assertEquals("image/svg+xml", fetcher.getMimeType(url4, type4))

        val url5 = "https://www.example.com/image"
        val type5: String? = null
        assertNull(fetcher.getMimeType(url5, type5))
    }

    @Test
    fun noDiskCache_fetcherReturnsASourceResult() = runTestAsync {
        val expectedSize = 1_000
        val path = "image.jpg"
        val result = newFetcher(path, ByteArray(expectedSize).toByteString()).fetch()

        assertIs<SourceFetchResult>(result)
        val actualSize = result.source.use { it.source().readAll(blackholeSink()) }
        assertEquals(expectedSize.toLong(), actualSize)
    }

    @Test
    fun noCachedFile_fetcherReturnsTheFile() = runTestAsync {
        val expectedSize = 1_000
        val path = "image.jpg"
        val result = newFetcher(path, ByteArray(expectedSize).toByteString()).fetch()

        assertIs<SourceFetchResult>(result)
        val file = assertNotNull(result.source.fileOrNull())

        // Ensure we can read the source.
        val actualSize = result.source.use { it.source().readAll(blackholeSink()) }
        assertEquals(expectedSize.toLong(), actualSize)

        // Ensure the result file is present.
        diskCache.openSnapshot(url("image.jpg"))!!.use { snapshot ->
            assertContains(fileSystem.list(diskCache.directory), snapshot.data)
            assertEquals(snapshot.data, file)
        }
    }

    @Test
    fun existingCachedFile_fetcherReturnsTheFile() = runTestAsync {
        val expectedSize = 1_000
        val path = "image.jpg"

        // Run the fetcher once to create the disk cache file.
        var result = newFetcher(path, ByteArray(expectedSize).toByteString()).fetch()
        assertIs<SourceFetchResult>(result)
        assertNotNull(result.source.fileOrNull())
        var actualSize = result.source.use { it.source().readAll(blackholeSink()) }
        assertEquals(expectedSize.toLong(), actualSize)

        // Run the fetcher a second time.
        result = newFetcher(path, ByteArray(expectedSize).toByteString()).fetch()
        assertIs<SourceFetchResult>(result)
        val file = assertNotNull(result.source.fileOrNull())
        actualSize = result.source.use { it.source().readAll(blackholeSink()) }
        assertEquals(expectedSize.toLong(), actualSize)

        // Ensure the result file is present.
        diskCache.openSnapshot(url("image.jpg"))!!.use { snapshot ->
            assertContains(fileSystem.list(diskCache.directory), snapshot.data)
            assertEquals(snapshot.data, file)
        }
    }

    @Test
    fun dedupe_multiple_requests() = runTestAsync {
        val expectedSize = 1_000
        val path = "image.jpg"
        val inFlightRequestStrategy = DeDupeInFlightRequestStrategy()

        val results = mutableListOf<FetchResult>()
        launch {
            for (i in 1..10) {
                launch {
                    val result = newFetcher(
                        path = path,
                        responseBody = ByteArray(expectedSize).toByteString(),
                        inFlightRequestStrategy = inFlightRequestStrategy
                    ).fetch()
                    assertIs<SourceFetchResult>(result)
                    result.source.close()
                    results.add(result)
                }
            }
        }.join()

        assertEquals(
            1,
            results.filterIsInstance<SourceFetchResult>()
                .count { it.dataSource == DataSource.NETWORK },
        )

        assertEquals(
            9,
            results.filterIsInstance<SourceFetchResult>()
                .count { it.dataSource == DataSource.DISK },
        )
    }

    abstract fun url(path: String): String

    abstract fun newFetcher(
        path: String = "image.jpg",
        responseBody: ByteString = ByteString.EMPTY,
        options: Options = Options(context),
        inFlightRequestStrategy: InFlightRequestStrategy = InFlightRequestStrategy.DEFAULT
    ): NetworkFetcher
}
