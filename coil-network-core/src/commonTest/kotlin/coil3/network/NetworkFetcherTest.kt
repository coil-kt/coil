package coil3.network

import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.test.utils.runTestAsync
import coil3.toUri
import coil3.util.ServiceLoaderComponentRegistry
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import okio.blackholeSink
import okio.fakefilesystem.FakeFileSystem
import okio.use

class NetworkFetcherTest : RobolectricTest() {

    private lateinit var fileSystem: FakeFileSystem
    private lateinit var diskCache: DiskCache

    @BeforeTest
    fun before() {
        fileSystem = FakeFileSystem()
        diskCache = DiskCache.Builder()
            .directory(fileSystem.workingDirectory)
            .fileSystem(fileSystem)
            .maxSizeBytes(Long.MAX_VALUE)
            .build()
    }

    @AfterTest
    fun after() {
        diskCache.shutdown()
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun basicNetworkFetch() = runTestAsync {
        val expectedSize = 1_000
        val engine = MockEngine {
            respond(ByteArray(expectedSize))
        }
        val result = newFetcher(engine = engine).fetch()

        assertIs<SourceFetchResult>(result)
        val actualSize = result.source.use { it.source().readAll(blackholeSink()) }
        assertEquals(expectedSize.toLong(), actualSize)
    }

    @Test
    fun mimeTypeIsParsedCorrectlyFromContentType() {
        val fetcher = newFetcher()

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
        val engine = MockEngine {
            respond(ByteArray(expectedSize))
        }
        val result = newFetcher(engine = engine, diskCache = null).fetch()

        assertIs<SourceFetchResult>(result)
        val actualSize = result.source.use { it.source().readAll(blackholeSink()) }
        assertEquals(expectedSize.toLong(), actualSize)
    }

    @Test
    fun noCachedFile_fetcherReturnsTheFile() = runTestAsync {
        val expectedSize = 1_000
        val url = "https://example.com/image.jpg"
        val engine = MockEngine {
            respond(ByteArray(expectedSize))
        }
        val result = newFetcher(url, engine = engine).fetch()

        assertIs<SourceFetchResult>(result)
        val file = assertNotNull(result.source.fileOrNull())

        // Ensure we can read the source.
        val actualSize = result.source.use { it.source().readAll(blackholeSink()) }
        assertEquals(expectedSize.toLong(), actualSize)

        // Ensure the result file is present.
        diskCache.openSnapshot(url)!!.use { snapshot ->
            assertContains(fileSystem.list(diskCache.directory), snapshot.data)
            assertEquals(snapshot.data, file)
        }
    }

    @Test
    fun existingCachedFile_fetcherReturnsTheFile() = runTestAsync {
        val expectedSize = 1_000
        val url = "https://example.com/image.jpg"
        val engine = MockEngine {
            respond(ByteArray(expectedSize))
        }

        // Run the fetcher once to create the disk cache file.
        var result = newFetcher(url, engine = engine).fetch()
        assertIs<SourceFetchResult>(result)
        assertNotNull(result.source.fileOrNull())
        var actualSize = result.source.use { it.source().readAll(blackholeSink()) }
        assertEquals(expectedSize.toLong(), actualSize)

        // Run the fetcher a second time.
        result = newFetcher(url, engine = engine).fetch()
        assertIs<SourceFetchResult>(result)
        val file = assertNotNull(result.source.fileOrNull())
        actualSize = result.source.use { it.source().readAll(blackholeSink()) }
        assertEquals(expectedSize.toLong(), actualSize)

        // Ensure the result file is present.
        diskCache.openSnapshot(url)!!.use { snapshot ->
            assertContains(fileSystem.list(diskCache.directory), snapshot.data)
            assertEquals(snapshot.data, file)
        }
    }

    private fun newFetcher(
        url: String = "https://example.com/image.jpg",
        engine: MockEngine = MockEngine { respondOk() },
        cacheStrategy: CacheStrategy = CacheStrategy(),
        options: Options = Options(context),
        diskCache: DiskCache? = this.diskCache,
    ): NetworkFetcher {
        val factory = NetworkFetcher.Factory(
            networkClient = lazyOf(HttpClient(engine)),
            cacheStrategy = lazyOf(cacheStrategy),
        )
        val imageLoader = ImageLoader.Builder(context)
            .diskCache(diskCache)
            .apply { diskCache?.fileSystem?.let(::fileSystem) }
            .build()
        return assertIs(factory.create(url.toUri(), options, imageLoader))
    }
}
