package coil3.fetch

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.request.Options
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.toUri
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import okio.sink
import okio.use

class JarFileFetcherTest : RobolectricTest() {
    private val imageLoader = ImageLoader(context)
    private val factory = JarFileFetcher.Factory()
    private val fileSystem = FakeFileSystem()

    @Test
    fun handlesCorrectScheme() {
        val uri = "jar:file:/fake/path/image.jpg".toUri()
        assertIs<JarFileFetcher>(factory.create(uri, Options(context), imageLoader))
    }

    @Test
    fun doesntHandleIncorrectScheme() {
        val uri = "file:/fake/path/image.jpg".toUri()
        assertNull(factory.create(uri, Options(context), imageLoader))
    }

    @Test
    fun opensFileInsideJarCorrectly() = runTest {
        val zipFile = fileSystem.workingDirectory / "one" / "two" / "three" / "base.apk"
        fileSystem.createDirectories(zipFile.parent!!)

        // We don't need to write full files to assert this works correctly.
        val files = mapOf(
            "entry_1" to "The quick brown fox jumps over the lazy dog happily.".encodeUtf8(),
            "entry_2" to "Bright stars shine above the quiet, peaceful town at night.".encodeUtf8(),
            "entry_3" to "Every morning, she enjoys a hot cup of strong coffee.".encodeUtf8(),
            "entry_4" to "Learning new languages opens doors to diverse cultures and experiences.".encodeUtf8(),
        )

        createZip(zipFile, files)
        fileSystem.workingDirectory = "/working".toPath()
        fileSystem.createDirectories(fileSystem.workingDirectory)

        for ((name, contents) in files) {
            val uri = "jar:file:$zipFile!/four/$name".toUri(separator = "/")
            val fetcher = factory.create(uri, Options(context, fileSystem = fileSystem), imageLoader)!!
            val result = assertIs<SourceFetchResult>(fetcher.fetch())

            result.source.use {
                assertEquals(contents, it.source().readByteString())
            }
        }
    }

    @Test
    fun opensFileInsideJarWithAbsoluteWindowsPath() = runTest {
        val fileSystem = FakeFileSystem().apply { emulateWindows() }
        val zipFile = "C:\\Users\\me\\app.jar".toPath()
        fileSystem.createDirectories(zipFile.parent!!)
        val contents = "The five boxing wizards jump quickly.".encodeUtf8()
        createZip(zipFile, mapOf("entry_1" to contents), fileSystem)

        val uri = Uri(
            scheme = "jar:file",
            path = "C:\\Users\\me\\app.jar!/four/entry_1",
            separator = "\\",
        )
        val fetcher = factory.create(uri, Options(context, fileSystem = fileSystem), imageLoader)!!
        val result = assertIs<SourceFetchResult>(fetcher.fetch())

        result.source.use {
            assertEquals(contents, it.source().readByteString())
        }
    }

    @Test
    fun opensFileInsideJarWithRelativeWindowsPath() = runTest {
        val fileSystem = FakeFileSystem().apply {
            emulateWindows()
            workingDirectory = "F:\\working".toPath()
            createDirectories(workingDirectory)
        }
        val zipFile = "Users\\me\\app.jar".toPath()
        fileSystem.createDirectories(zipFile.parent!!)
        val contents = "The five boxing wizards jump quickly.".encodeUtf8()
        createZip(zipFile, mapOf("entry_1" to contents), fileSystem)

        val uri = Uri(
            scheme = "jar:file",
            path = "Users\\me\\app.jar!/four/entry_1",
            separator = "\\",
        )
        val fetcher = factory.create(uri, Options(context, fileSystem = fileSystem), imageLoader)!!
        val result = assertIs<SourceFetchResult>(fetcher.fetch())

        result.source.use {
            assertEquals(contents, it.source().readByteString())
        }
    }

    @Test
    fun opensFileInsideJarWithRootedWindowsPath() = runTest {
        val fileSystem = FakeFileSystem().apply { emulateWindows() }
        val zipFile = "\\Users\\me\\app.jar".toPath()
        fileSystem.createDirectories(zipFile.parent!!)
        val contents = "image data".encodeUtf8()
        createZip(zipFile, mapOf("entry_1" to contents), fileSystem)

        val uri = Uri(
            scheme = "jar:file",
            path = "\\Users\\me\\app.jar!/four/entry_1",
            separator = "\\",
        )
        val fetcher = assertNotNull(factory.create(uri, Options(context, fileSystem = fileSystem), imageLoader))
        val result = assertIs<SourceFetchResult>(fetcher.fetch())

        result.source.use {
            assertEquals(contents, it.source().readByteString())
        }
    }

    @Test
    fun opensFileInsideJarWithDriveLikeUnixPath() = runTest {
        fileSystem.workingDirectory = "/working".toPath()
        fileSystem.createDirectories(fileSystem.workingDirectory)
        val zipFile = "/C:/Users/me/app.jar".toPath()
        fileSystem.createDirectories(zipFile.parent!!)
        val contents = "image data".encodeUtf8()
        createZip(zipFile, mapOf("entry_1" to contents))

        val uri = "jar:file:/C:/Users/me/app.jar!/four/entry_1".toUri(separator = "/")
        val fetcher = assertNotNull(factory.create(uri, Options(context, fileSystem = fileSystem), imageLoader))
        val result = assertIs<SourceFetchResult>(fetcher.fetch())

        result.source.use {
            assertEquals(contents, it.source().readByteString())
        }
    }

    @Test
    fun opensFileInsideJarWithWindowsDriveUri() = runTest {
        val fileSystem = FakeFileSystem().apply { emulateWindows() }
        val zipFile = "C:\\Users\\me\\app.jar".toPath()
        fileSystem.createDirectories(zipFile.parent!!)
        val contents = "image data".encodeUtf8()
        createZip(zipFile, mapOf("entry_1.svg" to contents), fileSystem)

        val uri = "jar:file:/C:/Users/me/app.jar!/four/entry_1.svg".toUri(separator = "\\")
        val fetcher = assertNotNull(factory.create(uri, Options(context, fileSystem = fileSystem), imageLoader))
        val result = assertIs<SourceFetchResult>(fetcher.fetch())

        result.source.use {
            assertEquals(contents, it.source().readByteString())
        }
        assertEquals("image/svg+xml", result.mimeType)
        assertEquals(DataSource.DISK, result.dataSource)
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun opensFileInsideJarWithWindowsDriveUriAndEmptyAuthority() = runTest {
        val fileSystem = FakeFileSystem().apply { emulateWindows() }
        val zipFile = "c:\\Users\\me\\app.jar".toPath()
        fileSystem.createDirectories(zipFile.parent!!)
        val contents = "image data".encodeUtf8()
        createZip(zipFile, mapOf("entry_1" to contents), fileSystem)

        val uri = "jar:file:///c:/Users/me/app.jar!/four/entry_1".toUri(separator = "\\")
        val fetcher = assertNotNull(factory.create(uri, Options(context, fileSystem = fileSystem), imageLoader))
        val result = assertIs<SourceFetchResult>(fetcher.fetch())

        result.source.use {
            assertEquals(contents, it.source().readByteString())
        }
    }

    @Test
    fun opensFileInsideJarWithEncodedWindowsUri() = runTest {
        val fileSystem = FakeFileSystem().apply { emulateWindows() }
        val zipFile = "C:\\Program Files\\app%20.jar".toPath()
        fileSystem.createDirectories(zipFile.parent!!)
        val contents = "image data".encodeUtf8()
        createZip(zipFile, mapOf("icon +%20.svg" to contents), fileSystem)

        val uri = "jar:file:/C:/Program%20Files/app%2520.jar!/four/icon%20+%2520.svg?v=1#preview"
            .toUri(separator = "\\")
        val fetcher = assertNotNull(factory.create(uri, Options(context, fileSystem = fileSystem), imageLoader))
        val result = assertIs<SourceFetchResult>(fetcher.fetch())

        result.source.use {
            assertEquals(contents, it.source().readByteString())
        }
    }

    @Test
    fun opensFileInsideJarWithWindowsUncPath() = runTest {
        val fileSystem = FakeFileSystem().apply { emulateWindows() }
        val zipFile = "\\\\server\\share\\app.jar".toPath()
        fileSystem.createDirectories(zipFile.parent!!)
        val contents = "image data".encodeUtf8()
        createZip(zipFile, mapOf("entry_1" to contents), fileSystem)

        val uri = Uri(
            scheme = "jar:file",
            path = "\\\\server\\share\\app.jar!/four/entry_1",
            separator = "\\",
        )
        val fetcher = assertNotNull(factory.create(uri, Options(context, fileSystem = fileSystem), imageLoader))
        val result = assertIs<SourceFetchResult>(fetcher.fetch())

        result.source.use {
            assertEquals(contents, it.source().readByteString())
        }
    }

    @Test
    fun opensFileInsideJarWithWindowsUncUri() = runTest {
        val fileSystem = FakeFileSystem().apply { emulateWindows() }
        val zipFile = "\\\\server\\share\\app.jar".toPath()
        fileSystem.createDirectories(zipFile.parent!!)
        val contents = "image data".encodeUtf8()
        createZip(zipFile, mapOf("entry_1" to contents), fileSystem)

        val uri = "jar:file:////server/share/app.jar!/four/entry_1".toUri(separator = "\\")
        val fetcher = assertNotNull(factory.create(uri, Options(context, fileSystem = fileSystem), imageLoader))
        val result = assertIs<SourceFetchResult>(fetcher.fetch())

        result.source.use {
            assertEquals(contents, it.source().readByteString())
        }
    }

    @Test
    fun opensFileInsideJarWithExclamationMarkInFileName() = runTest {
        val zipFile = "/app!1.jar".toPath()
        val contents = "image data".encodeUtf8()
        createZip(zipFile, mapOf("entry_1" to contents))

        val uri = "jar:file:/app!1.jar!/four/entry_1".toUri(separator = "/")
        val fetcher = assertNotNull(factory.create(uri, Options(context, fileSystem = fileSystem), imageLoader))
        val result = assertIs<SourceFetchResult>(fetcher.fetch())

        result.source.use {
            assertEquals(contents, it.source().readByteString())
        }
    }

    @Test
    fun opensFileInsideJarWithExclamationMarkInEntryDirectory() = runTest {
        val zipFile = "/app.jar".toPath()
        val contents = "image data".encodeUtf8()
        createZip(zipFile, mapOf("icons!/entry_1" to contents))

        val uri = "jar:file:/app.jar!/four/icons!/entry_1".toUri(separator = "/")
        val fetcher = assertNotNull(factory.create(uri, Options(context, fileSystem = fileSystem), imageLoader))
        val result = assertIs<SourceFetchResult>(fetcher.fetch())

        result.source.use {
            assertEquals(contents, it.source().readByteString())
        }
    }

    @Test
    fun opensFileInsideRelativeJarWithColonInEntryPath() = runTest {
        fileSystem.workingDirectory = "/working".toPath()
        fileSystem.createDirectories(fileSystem.workingDirectory)
        val zipFile = "app.jar".toPath()
        val contents = "image data".encodeUtf8()
        createZip(zipFile, mapOf("icon:1.svg" to contents))

        val uri = "jar:file:app.jar!/four/icon:1.svg".toUri(separator = "/")
        val fetcher = assertNotNull(factory.create(uri, Options(context, fileSystem = fileSystem), imageLoader))
        val result = assertIs<SourceFetchResult>(fetcher.fetch())

        result.source.use {
            assertEquals(contents, it.source().readByteString())
        }
    }

    @Test
    fun rejectsMissingJarPath() = runTest {
        val uri = "jar:file:!/four/entry_1".toUri(separator = "/")
        val fetcher = assertNotNull(factory.create(uri, Options(context, fileSystem = fileSystem), imageLoader))

        val exception = assertFailsWith<IllegalStateException> { fetcher.fetch() }

        assertEquals("Invalid jar:file URI: $uri", exception.message)
    }

    @Test
    fun rejectsMissingEntrySeparator() = runTest {
        val uri = "jar:file:/app!1.jar".toUri(separator = "/")
        val fetcher = assertNotNull(factory.create(uri, Options(context, fileSystem = fileSystem), imageLoader))

        val exception = assertFailsWith<IllegalStateException> { fetcher.fetch() }

        assertEquals("Invalid jar:file URI: $uri", exception.message)
    }

    private fun createZip(
        zipFile: Path,
        files: Map<String, ByteString>,
        fileSystem: FakeFileSystem = this.fileSystem,
    ) {
        ZipOutputStream(fileSystem.sink(zipFile).buffer().outputStream()).use { zip ->
            val directory = ZipEntry("four")
            zip.putNextEntry(directory)
            zip.closeEntry()

            for ((name, contents) in files.entries) {
                val zipEntry = ZipEntry("${directory.name}/$name")
                zip.putNextEntry(zipEntry)
                zip.sink().buffer().apply {
                    write(contents)
                    flush()
                }
                zip.closeEntry()
            }
        }
    }
}
