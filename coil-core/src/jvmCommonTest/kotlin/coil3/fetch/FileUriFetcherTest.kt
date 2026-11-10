package coil3.fetch

import coil3.ImageLoader
import coil3.request.Options
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.toUri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.encodeUtf8
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import okio.use

class FileUriFetcherTest : RobolectricTest() {

    @Test
    fun opensFileWithWindowsDriveUri() = runTest {
        val fileSystem = FakeFileSystem().apply { emulateWindows() }
        val file = "C:\\Users\\me\\image.svg".toPath()
        fileSystem.createDirectories(file.parent!!)
        val contents = "image data".encodeUtf8()
        fileSystem.write(file) { write(contents) }

        val uri = "file:/C:/Users/me/image.svg".toUri(separator = "\\")
        val options = Options(context, fileSystem = fileSystem)
        val fetcher = assertNotNull(FileUriFetcher.Factory().create(uri, options, ImageLoader(context)))
        val result = assertIs<SourceFetchResult>(fetcher.fetch())

        result.source.use {
            assertEquals(contents, it.source().readByteString())
            assertEquals(file, it.file())
        }
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun opensFileWithUnixUri() = runTest {
        val fileSystem = FakeFileSystem().apply { emulateUnix() }
        val file = "/images/my image.svg".toPath()
        fileSystem.createDirectories(file.parent!!)
        val contents = "image data".encodeUtf8()
        fileSystem.write(file) { write(contents) }

        val uri = "file:///images/my%20image.svg?v=1#preview".toUri(separator = "/")
        val options = Options(context, fileSystem = fileSystem)
        val fetcher = assertNotNull(FileUriFetcher.Factory().create(uri, options, ImageLoader(context)))
        val result = assertIs<SourceFetchResult>(fetcher.fetch())

        result.source.use {
            assertEquals(contents, it.source().readByteString())
            assertEquals(file, it.file())
        }
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun opensFileWithRelativeWindowsPath() = runTest {
        val fileSystem = FakeFileSystem().apply { emulateWindows() }
        val file = "images\\image.svg".toPath()
        fileSystem.createDirectories(file.parent!!)
        val contents = "image data".encodeUtf8()
        fileSystem.write(file) { write(contents) }

        val uri = "images\\image.svg".toUri(separator = "\\")
        val options = Options(context, fileSystem = fileSystem)
        val fetcher = assertNotNull(FileUriFetcher.Factory().create(uri, options, ImageLoader(context)))
        val result = assertIs<SourceFetchResult>(fetcher.fetch())

        result.source.use {
            assertEquals(contents, it.source().readByteString())
            assertEquals(file, it.file())
        }
        fileSystem.checkNoOpenFiles()
    }
}
