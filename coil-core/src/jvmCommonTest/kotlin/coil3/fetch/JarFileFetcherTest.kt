package coil3.fetch

import coil3.ImageLoader
import coil3.request.Options
import coil3.test.utils.context
import coil3.toUri
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.Path
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import okio.sink
import okio.use

class JarFileFetcherTest {
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

        val baseUri = "jar:file:$zipFile!/four"

        for ((name, contents) in files) {
            val uri = "$baseUri/$name".toUri()
            val fetcher = factory.create(uri, Options(context, fileSystem = fileSystem), imageLoader)!!
            val result = assertIs<SourceFetchResult>(fetcher.fetch())

            assertEquals(contents, result.source.source().readByteString())
        }
    }

    private fun createZip(zipFile: Path, files: Map<String, ByteString>) {
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
