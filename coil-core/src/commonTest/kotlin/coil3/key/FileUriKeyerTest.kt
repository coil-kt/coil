package coil3.key

import coil3.request.Options
import coil3.test.utils.FakeClock
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.toUri
import coil3.util.createFile
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import okio.Path
import okio.fakefilesystem.FakeFileSystem
import okio.use

class FileUriKeyerTest : RobolectricTest() {

    private lateinit var clock: FakeClock
    private lateinit var fileSystem: FakeFileSystem
    private lateinit var options: Options

    @BeforeTest
    fun before() {
        clock = FakeClock()
        fileSystem = FakeFileSystem(clock)
        options = Options(context, fileSystem = fileSystem)
    }

    @Test
    fun fileCacheKeyWithLastModified() {
        val file = fileSystem.workingDirectory / "image.jpg"
        fileSystem.createFile(file, mustCreate = true)

        val keyer = FileUriKeyer(addLastModifiedToFileCacheKey = true)

        file.setLastModified(1234L)
        val firstKey = keyer.key("file://$file".toUri(), options)

        file.setLastModified(4321L)
        val secondKey = keyer.key("file://$file".toUri(), options)

        assertNotEquals(secondKey, firstKey)
    }

    @Test
    fun fileCacheKeyWithoutLastModified() {
        val file = fileSystem.workingDirectory / "image.jpg"
        fileSystem.createFile(file, mustCreate = true)

        val keyer = FileUriKeyer(addLastModifiedToFileCacheKey = false)

        file.setLastModified(1234L)
        val actual = keyer.key("file://$file".toUri(), options)

        assertNull(actual)
    }

    private fun Path.setLastModified(epochMillis: Long) {
        clock.epochMillis = epochMillis
        fileSystem.openReadWrite(this).use { /* Do nothing. */ }
    }
}
