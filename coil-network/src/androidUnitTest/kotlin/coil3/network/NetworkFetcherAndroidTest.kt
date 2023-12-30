package coil3.network

import android.os.NetworkOnMainThreadException
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.fetch.NetworkFetcher
import coil3.fetch.SourceFetchResult
import coil3.request.CachePolicy
import coil3.request.Options
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.test.utils.runTestMain
import coil3.toUri
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import okio.Buffer
import okio.blackholeSink
import okio.fakefilesystem.FakeFileSystem
import okio.use
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class NetworkFetcherAndroidTest : RobolectricTest() {

    private lateinit var fileSystem: FakeFileSystem
    private lateinit var diskCache: DiskCache

    @Before
    fun before() {
        fileSystem = FakeFileSystem()
        diskCache = DiskCache.Builder()
            .directory(fileSystem.workingDirectory)
            .fileSystem(fileSystem)
            .maxSizeBytes(Long.MAX_VALUE)
            .build()
    }

    @After
    fun after() {
        diskCache.shutdown()
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun `request on main thread throws NetworkOnMainThreadException`() = runTestMain {
        val fetcher = newFetcher()

        assertFailsWith<NetworkOnMainThreadException> { fetcher.fetch() }
    }

    @Test
    @Ignore("Figure out if/how we can support this use case with Ktor.")
    fun `request on main thread with network cache policy disabled executes without throwing`() = runTestMain {
        val expectedSize = 1_000
        val url = "https://example.com/image.jpg"

        // Write the image in the disk cache.
        val editor = diskCache.openEditor(url)!!
        fileSystem.write(editor.data) {
            writeAll(Buffer().write(ByteArray(expectedSize)))
        }
        editor.commit()

        // Load it from the disk cache on the main thread.
        val engine = MockEngine {
            respond(ByteArray(expectedSize))
        }
        val result = newFetcher(
            url = url,
            engine = engine,
            options = Options(
                context = context,
                networkCachePolicy = CachePolicy.DISABLED,
            ),
        ).fetch()

        assertIs<SourceFetchResult>(result)
        assertNotNull(result.source.fileOrNull())
        val actualSize = result.source.use { it.source().readAll(blackholeSink()) }
        assertEquals(expectedSize.toLong(), actualSize)
    }

    private fun newFetcher(
        url: String = "https://example.com/image.jpg",
        engine: MockEngine = MockEngine { respondOk() },
        cacheStrategy: CacheStrategy = CacheStrategy(),
        options: Options = Options(context),
        diskCache: DiskCache? = this.diskCache,
    ): NetworkFetcher {
        val factory = NetworkFetcher.Factory(
            httpClient = lazyOf(HttpClient(engine)),
            cacheStrategy = lazyOf(cacheStrategy),
        )
        val imageLoader = ImageLoader.Builder(context)
            .diskCache(diskCache)
            .apply { diskCache?.fileSystem?.let(::fileSystem) }
            .build()
        return assertIs(factory.create(url.toUri(), options, imageLoader))
    }
}
