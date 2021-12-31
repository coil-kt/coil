/*
 * Copyright (C) 2011 The Android Open Source Project
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
package coil.disk

import coil.disk.DiskLruCache.Editor
import coil.disk.DiskLruCache.Snapshot
import okio.FileNotFoundException
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.Source
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Derived from OkHttp's DiskLruCache tests:
 * https://github.com/square/okhttp/blob/master/okhttp/src/jvmTest/java/okhttp3/internal/cache/DiskLruCacheTest.kt
 */
class DiskLruCacheTest {

    private lateinit var fileSystem: FaultyFileSystem
    private lateinit var dispatcher: SimpleTestDispatcher
    private lateinit var caches: MutableSet<DiskLruCache>
    private lateinit var cache: DiskLruCache

    private val cacheDir = "/cache".toPath()
    private val journalFile = cacheDir / DiskLruCache.JOURNAL_FILE
    private val journalFileBackup = cacheDir / DiskLruCache.JOURNAL_FILE_BACKUP

    @Before
    fun before() {
        fileSystem = FaultyFileSystem(FakeFileSystem().apply { emulateUnix() })
        if (fileSystem.exists(cacheDir)) {
            fileSystem.deleteRecursively(cacheDir)
        }
        dispatcher = SimpleTestDispatcher()
        caches = mutableSetOf()
        createNewCache()
    }

    @After
    fun after() {
        caches.forEach { it.close() }
        (fileSystem.delegate as FakeFileSystem).checkNoOpenFiles()
    }

    private fun createNewCache(maxSize: Long = Long.MAX_VALUE) {
        cache = DiskLruCache(fileSystem, cacheDir, dispatcher, maxSize, 100, 2)
        cache.initialize()
        caches += cache
    }

    @Test
    fun emptyCache() {
        cache.close()
        assertJournalEquals()
    }

    @Test
    fun recoverFromInitializationFailure() {
        // Add an uncommitted entry. This will get detected on initialization, and the cache will
        // attempt to delete the file. Do not explicitly close the cache here so the entry is left
        // as incomplete.
        val creator = cache.edit("k1")!!
        creator.newSink(0).buffer().use {
            it.writeUtf8("Hello")
        }

        // Simulate a severe Filesystem failure on the first initialization.
        fileSystem.setFaultyDelete(cacheDir / "k1.0.tmp", true)
        fileSystem.setFaultyDelete(cacheDir, true)
        cache = DiskLruCache(fileSystem, cacheDir, dispatcher, Long.MAX_VALUE, 100, 2)
        caches += cache
        try {
            cache["k1"]
            fail("")
        } catch (_: IOException) {
        }

        // Now let it operate normally.
        fileSystem.setFaultyDelete(cacheDir / "k1.0.tmp", false)
        fileSystem.setFaultyDelete(cacheDir, false)
        val snapshot = cache["k1"]
        assertNull(snapshot)
    }

    @Test
    fun validateKey() {
        var key: String? = null
        try {
            key = "has_space "
            cache.edit(key)
            fail("Expecting an IllegalArgumentException as the key was invalid.")
        } catch (iae: IllegalArgumentException) {
            assertEquals("keys must match regex [a-z0-9_-]{1,120}: \"$key\"", iae.message)
        }
        try {
            key = "has_CR\r"
            cache.edit(key)
            fail("Expecting an IllegalArgumentException as the key was invalid.")
        } catch (iae: IllegalArgumentException) {
            assertEquals("keys must match regex [a-z0-9_-]{1,120}: \"$key\"", iae.message)
        }
        try {
            key = "has_LF\n"
            cache.edit(key)
            fail("Expecting an IllegalArgumentException as the key was invalid.")
        } catch (iae: IllegalArgumentException) {
            assertEquals("keys must match regex [a-z0-9_-]{1,120}: \"$key\"", iae.message)
        }
        try {
            key = "has_invalid/"
            cache.edit(key)
            fail("Expecting an IllegalArgumentException as the key was invalid.")
        } catch (iae: IllegalArgumentException) {
            assertEquals("keys must match regex [a-z0-9_-]{1,120}: \"$key\"", iae.message)
        }
        try {
            key = "has_invalid\u2603"
            cache.edit(key)
            fail("Expecting an IllegalArgumentException as the key was invalid.")
        } catch (iae: IllegalArgumentException) {
            assertEquals("keys must match regex [a-z0-9_-]{1,120}: \"$key\"", iae.message)
        }
        try {
            key = ("this_is_way_too_long_this_is_way_too_long_this_is_way_too_long_" +
                "this_is_way_too_long_this_is_way_too_long_this_is_way_too_long")
            cache.edit(key)
            fail("Expecting an IllegalArgumentException as the key was too long.")
        } catch (iae: IllegalArgumentException) {
            assertEquals("keys must match regex [a-z0-9_-]{1,120}: \"$key\"", iae.message)
        }

        // Test valid cases.

        // Exactly 120.
        key = ("0123456789012345678901234567890123456789012345678901234567890123456789" +
            "01234567890123456789012345678901234567890123456789")
        cache.edit(key)!!.abort()
        // Contains all valid characters.
        key = "abcdefghijklmnopqrstuvwxyz_0123456789"
        cache.edit(key)!!.abort()
        // Contains dash.
        key = "-20384573948576"
        cache.edit(key)!!.abort()
    }

    @Test
    fun writeAndReadEntry() {
        val creator = cache.edit("k1")!!
        creator.setString(0, "ABC")
        creator.setString(1, "DE")
        creator.file(0) // Access the file to read it.
        creator.file(1) // Access the file to read it.
        creator.commit()
        val snapshot = cache["k1"]!!
        snapshot.assertValue(0, "ABC")
        snapshot.assertValue(1, "DE")
    }

    @Test
    fun readAndWriteEntryAcrossCacheOpenAndClose() {
        val creator = cache.edit("k1")!!
        creator.setString(0, "A")
        creator.setString(1, "B")
        creator.commit()
        cache.close()
        createNewCache()
        val snapshot = cache["k1"]!!
        snapshot.assertValue(0, "A")
        snapshot.assertValue(1, "B")
        snapshot.close()
    }

    @Test
    fun readAndWriteEntryWithoutProperClose() {
        val creator = cache.edit("k1")!!
        creator.setString(0, "A")
        creator.setString(1, "B")
        creator.commit()

        // Simulate a dirty close of 'cache' by opening the cache directory again.
        createNewCache()
        cache["k1"]!!.use {
            it.assertValue(0, "A")
            it.assertValue(1, "B")
        }
    }

    @Test
    fun journalWithEditAndPublish() {
        val creator = cache.edit("k1")!!
        assertJournalEquals("DIRTY k1") // DIRTY must always be flushed.
        creator.setString(0, "AB")
        creator.setString(1, "C")
        creator.commit()
        cache.close()
        assertJournalEquals("DIRTY k1", "CLEAN k1 2 1")
    }

    @Test
    fun revertedNewFileIsRemoveInJournal() {
        val creator = cache.edit("k1")!!
        assertJournalEquals("DIRTY k1") // DIRTY must always be flushed.
        creator.setString(0, "AB")
        creator.setString(1, "C")
        creator.abort()
        cache.close()
        assertJournalEquals("DIRTY k1", "REMOVE k1")
    }

    /** We have to wait until the edit is committed before we can delete its files. */
    @Test
    fun `unterminated edit is reverted on cache close`() {
        val editor = cache.edit("k1")!!
        editor.setString(0, "AB")
        editor.setString(1, "C")
        cache.close()
        val expected = arrayOf("DIRTY k1")
        assertJournalEquals(*expected)
        editor.commit()
        assertJournalEquals(*expected) // 'REMOVE k1' not written because journal is closed.
    }

    @Test
    fun journalDoesNotIncludeReadOfYetUnpublishedValue() {
        val creator = cache.edit("k1")!!
        assertNull(cache["k1"])
        creator.setString(0, "A")
        creator.setString(1, "BC")
        creator.commit()
        cache.close()
        assertJournalEquals("DIRTY k1", "CLEAN k1 1 2")
    }

    @Test
    fun journalWithEditAndPublishAndRead() {
        val k1Creator = cache.edit("k1")!!
        k1Creator.setString(0, "AB")
        k1Creator.setString(1, "C")
        k1Creator.commit()
        val k2Creator = cache.edit("k2")!!
        k2Creator.setString(0, "DEF")
        k2Creator.setString(1, "G")
        k2Creator.commit()
        val k1Snapshot = cache["k1"]!!
        k1Snapshot.close()
        cache.close()
        assertJournalEquals("DIRTY k1", "CLEAN k1 2 1", "DIRTY k2", "CLEAN k2 3 1", "READ k1")
    }

    @Test
    fun cannotOperateOnEditAfterPublish() {
        val editor = cache.edit("k1")!!
        editor.setString(0, "A")
        editor.setString(1, "B")
        editor.commit()
        editor.assertInoperable()
    }

    @Test
    fun cannotOperateOnEditAfterRevert() {
        val editor = cache.edit("k1")!!
        editor.setString(0, "A")
        editor.setString(1, "B")
        editor.abort()
        editor.assertInoperable()
    }

    @Test
    fun explicitRemoveAppliedToDiskImmediately() {
        val editor = cache.edit("k1")!!
        editor.setString(0, "ABC")
        editor.setString(1, "B")
        editor.commit()
        val k1 = getCleanFile("k1", 0)
        assertEquals("ABC", readFile(k1))
        cache.remove("k1")
        assertFalse(fileSystem.exists(k1))
    }

    @Test
    fun removePreventsActiveEditFromStoringAValue() {
        set("a", "a", "a")
        val a = cache.edit("a")!!
        a.setString(0, "a1")
        assertTrue(cache.remove("a"))
        a.setString(1, "a2")
        a.commit()
        assertAbsent("a")
    }

    @Test
    fun openWithDirtyKeyDeletesAllFilesForThatKey() {
        cache.close()
        val cleanFile0 = getCleanFile("k1", 0)
        val cleanFile1 = getCleanFile("k1", 1)
        val dirtyFile0 = getDirtyFile("k1", 0)
        val dirtyFile1 = getDirtyFile("k1", 1)
        writeFile(cleanFile0, "A")
        writeFile(cleanFile1, "B")
        writeFile(dirtyFile0, "C")
        writeFile(dirtyFile1, "D")
        createJournal("CLEAN k1 1 1", "DIRTY k1")
        createNewCache()
        assertFalse(fileSystem.exists(cleanFile0))
        assertFalse(fileSystem.exists(cleanFile1))
        assertFalse(fileSystem.exists(dirtyFile0))
        assertFalse(fileSystem.exists(dirtyFile1))
        assertNull(cache["k1"])
    }

    @Test
    fun openWithInvalidVersionClearsDirectory() {
        cache.close()
        generateSomeGarbageFiles()
        createJournalWithHeader(DiskLruCache.MAGIC, "0", "100", "2", "")
        createNewCache()
        assertGarbageFilesAllDeleted()
    }

    @Test
    fun openWithInvalidAppVersionClearsDirectory() {
        cache.close()
        generateSomeGarbageFiles()
        createJournalWithHeader(DiskLruCache.MAGIC, "1", "101", "2", "")
        createNewCache()
        assertGarbageFilesAllDeleted()
    }

    @Test
    fun openWithInvalidValueCountClearsDirectory() {
        cache.close()
        generateSomeGarbageFiles()
        createJournalWithHeader(DiskLruCache.MAGIC, "1", "100", "1", "")
        createNewCache()
        assertGarbageFilesAllDeleted()
    }

    @Test
    fun openWithInvalidBlankLineClearsDirectory() {
        cache.close()
        generateSomeGarbageFiles()
        createJournalWithHeader(DiskLruCache.MAGIC, "1", "100", "2", "x")
        createNewCache()
        assertGarbageFilesAllDeleted()
    }

    @Test
    fun openWithInvalidJournalLineClearsDirectory() {
        cache.close()
        generateSomeGarbageFiles()
        createJournal("CLEAN k1 1 1", "BOGUS")
        createNewCache()
        assertGarbageFilesAllDeleted()
        assertNull(cache["k1"])
    }

    @Test
    fun openWithInvalidFileSizeClearsDirectory() {
        cache.close()
        generateSomeGarbageFiles()
        createJournal("CLEAN k1 0000x001 1")
        createNewCache()
        assertGarbageFilesAllDeleted()
        assertNull(cache["k1"])
    }

    @Test
    fun openWithTruncatedLineDiscardsThatLine() {
        cache.close()
        writeFile(getCleanFile("k1", 0), "A")
        writeFile(getCleanFile("k1", 1), "B")
        fileSystem.write(journalFile) {
            writeUtf8(
                """
                |${DiskLruCache.MAGIC}
                |${DiskLruCache.VERSION}
                |100
                |2
                |
                |CLEAN k1 1 1""".trimMargin() // no trailing newline
            )
        }
        createNewCache()
        assertNull(cache["k1"])

        // The journal is not corrupt when editing after a truncated line.
        set("k1", "C", "D")
        cache.close()
        createNewCache()
        assertValue("k1", "C", "D")
    }

    @Test
    fun openWithTooManyFileSizesClearsDirectory() {
        cache.close()
        generateSomeGarbageFiles()
        createJournal("CLEAN k1 1 1 1")
        createNewCache()
        assertGarbageFilesAllDeleted()
        assertNull(cache["k1"])
    }

    @Test
    fun keyWithSpaceNotPermitted() {
        try {
            cache.edit("my key")
            fail("")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun keyWithNewlineNotPermitted() {
        try {
            cache.edit("my\nkey")
            fail("")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun keyWithCarriageReturnNotPermitted() {
        try {
            cache.edit("my\rkey")
            fail("")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun createNewEntryWithTooFewValuesSucceeds() {
        val creator = cache.edit("k1")!!
        creator.setString(1, "A")
        creator.commit()
        assertTrue(fileSystem.exists(getCleanFile("k1", 0)))
        assertTrue(fileSystem.exists(getCleanFile("k1", 1)))
        assertFalse(fileSystem.exists(getDirtyFile("k1", 0)))
        assertFalse(fileSystem.exists(getDirtyFile("k1", 1)))
        assertNotNull(cache["k1"]).close()
        val creator2 = cache.edit("k1")!!
        creator2.setString(0, "B")
        creator2.setString(1, "C")
        creator2.commit()
    }

    @Test
    fun revertWithTooFewValues() {
        val creator = cache.edit("k1")!!
        creator.setString(1, "A")
        creator.abort()
        assertFalse(fileSystem.exists(getCleanFile("k1", 0)))
        assertFalse(fileSystem.exists(getCleanFile("k1", 1)))
        assertFalse(fileSystem.exists(getDirtyFile("k1", 0)))
        assertFalse(fileSystem.exists(getDirtyFile("k1", 1)))
        assertNull(cache["k1"])
    }

    @Test
    fun updateExistingEntryWithTooFewValuesReusesPreviousValues() {
        val creator = cache.edit("k1")!!
        creator.setString(0, "A")
        creator.setString(1, "B")
        creator.commit()
        val updater = cache.edit("k1")!!
        updater.setString(0, "C")
        updater.commit()
        val snapshot = cache["k1"]!!
        snapshot.assertValue(0, "C")
        snapshot.assertValue(1, "B")
        snapshot.close()
    }

    @Test
    fun evictOnInsert() {
        cache.close()
        createNewCache(10)
        set("a", "a", "aaa") // size 4
        set("b", "bb", "bbbb") // size 6
        assertEquals(10, cache.size())

        // Cause the size to grow to 12 should evict 'A'.
        set("c", "c", "c")
        cache.flush()
        assertEquals(8, cache.size())
        assertAbsent("a")
        assertValue("b", "bb", "bbbb")
        assertValue("c", "c", "c")

        // Causing the size to grow to 10 should evict nothing.
        set("d", "d", "d")
        cache.flush()
        assertEquals(10, cache.size())
        assertAbsent("a")
        assertValue("b", "bb", "bbbb")
        assertValue("c", "c", "c")
        assertValue("d", "d", "d")

        // Causing the size to grow to 18 should evict 'B' and 'C'.
        set("e", "eeee", "eeee")
        cache.flush()
        assertEquals(10, cache.size())
        assertAbsent("a")
        assertAbsent("b")
        assertAbsent("c")
        assertValue("d", "d", "d")
        assertValue("e", "eeee", "eeee")
    }

    @Test
    fun evictOnUpdate() {
        cache.close()
        createNewCache(10)
        set("a", "a", "aa") // size 3
        set("b", "b", "bb") // size 3
        set("c", "c", "cc") // size 3
        assertEquals(9, cache.size())

        // Causing the size to grow to 11 should evict 'A'.
        set("b", "b", "bbbb")
        cache.flush()
        assertEquals(8, cache.size())
        assertAbsent("a")
        assertValue("b", "b", "bbbb")
        assertValue("c", "c", "cc")
    }

    @Test
    fun evictionHonorsLruFromCurrentSession() {
        cache.close()
        createNewCache(10)
        set("a", "a", "a")
        set("b", "b", "b")
        set("c", "c", "c")
        set("d", "d", "d")
        set("e", "e", "e")
        cache["b"]!!.close() // 'B' is now least recently used.

        // Causing the size to grow to 12 should evict 'A'.
        set("f", "f", "f")
        // Causing the size to grow to 12 should evict 'C'.
        set("g", "g", "g")
        cache.flush()
        assertEquals(10, cache.size())
        assertAbsent("a")
        assertValue("b", "b", "b")
        assertAbsent("c")
        assertValue("d", "d", "d")
        assertValue("e", "e", "e")
        assertValue("f", "f", "f")
    }

    @Test
    fun evictionHonorsLruFromPreviousSession() {
        set("a", "a", "a")
        set("b", "b", "b")
        set("c", "c", "c")
        set("d", "d", "d")
        set("e", "e", "e")
        set("f", "f", "f")
        cache["b"]!!.close() // 'B' is now least recently used.
        assertEquals(12, cache.size())
        cache.close()
        createNewCache(10)
        set("g", "g", "g")
        cache.flush()
        assertEquals(10, cache.size())
        assertAbsent("a")
        assertValue("b", "b", "b")
        assertAbsent("c")
        assertValue("d", "d", "d")
        assertValue("e", "e", "e")
        assertValue("f", "f", "f")
        assertValue("g", "g", "g")
    }

    @Test
    fun cacheSingleEntryOfSizeGreaterThanMaxSize() {
        cache.close()
        createNewCache(10)
        set("a", "aaaaa", "aaaaaa") // size=11
        cache.flush()
        assertAbsent("a")
    }

    @Test
    fun cacheSingleValueOfSizeGreaterThanMaxSize() {
        cache.close()
        createNewCache(10)
        set("a", "aaaaaaaaaaa", "a") // size=12
        cache.flush()
        assertAbsent("a")
    }

    @Test
    fun constructorDoesNotAllowZeroCacheSize() {
        try {
            caches += DiskLruCache(fileSystem, cacheDir, dispatcher, 0, 100, 2)
            fail("")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun constructorDoesNotAllowZeroValuesPerEntry() {
        try {
            caches += DiskLruCache(fileSystem, cacheDir, dispatcher, 10, 100, 0)
            fail("")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun removeAbsentElement() {
        assertFalse(cache.remove("a"))
    }

    @Test
    fun readingTheSameFileMultipleTimes() {
        set("a", "a", "b")
        val snapshot = cache["a"]!!
        assertSame(snapshot.file(0), snapshot.file(0))
        snapshot.close()
    }

    @Test
    fun rebuildJournalOnRepeatedReads() {
        set("a", "a", "a")
        set("b", "b", "b")
        while (dispatcher.isIdle()) {
            assertValue("a", "a", "a")
            assertValue("b", "b", "b")
        }
    }

    @Test
    fun rebuildJournalOnRepeatedEdits() {
        while (dispatcher.isIdle()) {
            set("a", "a", "a")
            set("b", "b", "b")
        }
        dispatcher.runNextTask()

        // Sanity check that a rebuilt journal behaves normally.
        assertValue("a", "a", "a")
        assertValue("b", "b", "b")
    }

    /** https://github.com/JakeWharton/DiskLruCache/issues/28 */
    @Test
    fun rebuildJournalOnRepeatedReadsWithOpenAndClose() {
        set("a", "a", "a")
        set("b", "b", "b")
        while (dispatcher.isIdle()) {
            assertValue("a", "a", "a")
            assertValue("b", "b", "b")
            cache.close()
            createNewCache()
        }
    }

    /** https://github.com/JakeWharton/DiskLruCache/issues/28 */
    @Test
    fun rebuildJournalOnRepeatedEditsWithOpenAndClose() {
        while (dispatcher.isIdle()) {
            set("a", "a", "a")
            set("b", "b", "b")
            cache.close()
            createNewCache()
        }
    }

    @Test
    fun rebuildJournalFailurePreventsEditors() {
        while (dispatcher.isIdle()) {
            set("a", "a", "a")
            set("b", "b", "b")
        }

        // Cause the rebuild action to fail.
        fileSystem.setFaultyRename(journalFileBackup, true)
        dispatcher.runNextTask()

        // Don't allow edits under any circumstances.
        assertNull(cache.edit("a"))
        assertNull(cache.edit("c"))
        cache["a"]!!.use {
            assertNull(cache.edit(it.entry.key))
        }
    }

    @Test
    fun rebuildJournalFailureIsRetried() {
        while (dispatcher.isIdle()) {
            set("a", "a", "a")
            set("b", "b", "b")
        }

        // Cause the rebuild action to fail.
        fileSystem.setFaultyRename(journalFileBackup, true)
        dispatcher.runNextTask()

        // The rebuild is retried on cache hits and on cache edits.
        val snapshot = cache["b"]!!
        snapshot.close()
        assertNull(cache.edit("d"))
        assertFalse(dispatcher.isIdle())

        // On cache misses, no retry job is queued.
        assertNull(cache["c"])
        assertFalse(dispatcher.isIdle())

        // Let the rebuild complete successfully.
        fileSystem.setFaultyRename(journalFileBackup, false)
        dispatcher.runNextTask()
        assertJournalEquals("CLEAN a 1 1", "CLEAN b 1 1")
    }

    @Test
    fun rebuildJournalFailureWithInFlightEditors() {
        while (dispatcher.isIdle()) {
            set("a", "a", "a")
            set("b", "b", "b")
        }
        val commitEditor = cache.edit("c")!!
        val abortEditor = cache.edit("d")!!
        cache.edit("e") // Grab an editor, but don't do anything with it.

        // Cause the rebuild action to fail.
        fileSystem.setFaultyRename(journalFileBackup, true)
        dispatcher.runNextTask()

        // In-flight editors can commit and have their values retained.
        commitEditor.setString(0, "c")
        commitEditor.setString(1, "c")
        commitEditor.commit()
        assertValue("c", "c", "c")
        abortEditor.abort()

        // Let the rebuild complete successfully.
        fileSystem.setFaultyRename(journalFileBackup, false)
        dispatcher.runNextTask()
        assertJournalEquals("CLEAN a 1 1", "CLEAN b 1 1", "DIRTY e", "CLEAN c 1 1")
    }

    @Test
    fun rebuildJournalFailureWithEditorsInFlightThenClose() {
        while (dispatcher.isIdle()) {
            set("a", "a", "a")
            set("b", "b", "b")
        }
        val commitEditor = cache.edit("c")!!
        val abortEditor = cache.edit("d")!!
        cache.edit("e") // Grab an editor, but don't do anything with it.

        // Cause the rebuild action to fail.
        fileSystem.setFaultyRename(journalFileBackup, true)
        dispatcher.runNextTask()
        commitEditor.setString(0, "c")
        commitEditor.setString(1, "c")
        commitEditor.commit()
        assertValue("c", "c", "c")
        abortEditor.abort()
        cache.close()
        createNewCache()

        // Although 'c' successfully committed above, the journal wasn't available to issue a CLEAN op.
        // Because the last state of 'c' was DIRTY before the journal failed, it should be removed
        // entirely on a subsequent open.
        assertEquals(4, cache.size())
        assertAbsent("c")
        assertAbsent("d")
        assertAbsent("e")
    }

    @Test
    fun rebuildJournalFailureAllowsRemovals() {
        while (dispatcher.isIdle()) {
            set("a", "a", "a")
            set("b", "b", "b")
        }

        // Cause the rebuild action to fail.
        fileSystem.setFaultyRename(journalFileBackup, true)
        dispatcher.runNextTask()
        assertTrue(cache.remove("a"))
        assertAbsent("a")

        // Let the rebuild complete successfully.
        fileSystem.setFaultyRename(journalFileBackup, false)
        dispatcher.runNextTask()
        assertJournalEquals("CLEAN b 1 1")
    }

    @Test
    fun rebuildJournalFailureWithRemovalThenClose() {
        while (dispatcher.isIdle()) {
            set("a", "a", "a")
            set("b", "b", "b")
        }

        // Cause the rebuild action to fail.
        fileSystem.setFaultyRename(journalFileBackup, true)
        dispatcher.runNextTask()
        assertTrue(cache.remove("a"))
        assertAbsent("a")
        cache.close()
        createNewCache()

        // The journal will have no record that 'a' was removed. It will have an entry for 'a', but when
        // it tries to read the cache files, it will find they were deleted. Once it encounters an entry
        // with missing cache files, it should remove it from the cache entirely.
        assertEquals(4, cache.size())
        assertNull(cache["a"])
        assertEquals(2, cache.size())
    }

    @Test
    fun rebuildJournalFailureAllowsEvictAll() {
        while (dispatcher.isIdle()) {
            set("a", "a", "a")
            set("b", "b", "b")
        }

        // Cause the rebuild action to fail.
        fileSystem.setFaultyRename(journalFileBackup, true)
        dispatcher.runNextTask()
        cache.evictAll()
        assertEquals(0, cache.size())
        assertAbsent("a")
        assertAbsent("b")
        cache.close()
        createNewCache()

        // The journal has no record that 'a' and 'b' were removed. It will have an entry for both, but
        // when it tries to read the cache files for either entry, it will discover the cache files are
        // missing and remove the entries from the cache.
        assertEquals(4, cache.size())
        assertNull(cache["a"])
        assertNull(cache["b"])
        assertEquals(0, cache.size())
    }

    @Test
    fun rebuildJournalFailureWithCacheTrim() {
        createNewCache(4)
        while (dispatcher.isIdle()) {
            set("a", "aa", "aa")
            set("b", "bb", "bb")
        }

        // Cause the rebuild action to fail.
        fileSystem.setFaultyRename(journalFileBackup, true)
        dispatcher.runNextTask()

        assertAbsent("a")
        assertValue("b", "bb", "bb")
    }

    @Test
    fun restoreBackupFile() {
        val creator = cache.edit("k1")!!
        creator.setString(0, "ABC")
        creator.setString(1, "DE")
        creator.commit()
        cache.close()
        fileSystem.atomicMove(journalFile, journalFileBackup)
        assertFalse(fileSystem.exists(journalFile))
        createNewCache()
        val snapshot = cache["k1"]!!
        snapshot.assertValue(0, "ABC")
        snapshot.assertValue(1, "DE")
        assertFalse(fileSystem.exists(journalFileBackup))
        assertTrue(fileSystem.exists(journalFile))
    }

    @Test
    fun journalFileIsPreferredOverBackupFile() {
        var creator = cache.edit("k1")!!
        creator.setString(0, "ABC")
        creator.setString(1, "DE")
        creator.commit()
        cache.flush()
        fileSystem.copy(journalFile, journalFileBackup)
        creator = cache.edit("k2")!!
        creator.setString(0, "F")
        creator.setString(1, "GH")
        creator.commit()
        cache.close()
        assertTrue(fileSystem.exists(journalFile))
        assertTrue(fileSystem.exists(journalFileBackup))
        createNewCache()
        val snapshotA = cache["k1"]!!
        snapshotA.assertValue(0, "ABC")
        snapshotA.assertValue(1, "DE")
        val snapshotB = cache["k2"]!!
        snapshotB.assertValue(0, "F")
        snapshotB.assertValue(1, "GH")
        assertFalse(fileSystem.exists(journalFileBackup))
        assertTrue(fileSystem.exists(journalFile))
    }

    @Test
    fun openCreatesDirectoryIfNecessary() {
        cache.close()
        val dir = (cacheDir / "testOpenCreatesDirectoryIfNecessary").also { fileSystem.createDirectories(it) }
        cache = DiskLruCache(fileSystem, dir, dispatcher, Long.MAX_VALUE, 100, 2)
        caches += cache
        set("a", "a", "a")
        assertTrue(fileSystem.exists(dir / "a.0"))
        assertTrue(fileSystem.exists(dir / "a.1"))
        assertTrue(fileSystem.exists(dir / "journal"))
    }

    @Test
    fun fileDeletedExternally() {
        set("a", "a", "a")
        fileSystem.delete(getCleanFile("a", 1))
        assertNull(cache["a"])
        assertEquals(0, cache.size())
    }

    @Test
    fun editSameVersion() {
        set("a", "a", "a")
        cache["a"]!!.close()
        val editor = cache.edit("a")!!
        editor.setString(1, "a2")
        editor.commit()
        assertValue("a", "a", "a2")
    }

    @Test
    fun editSnapshotAfterChangeAborted() {
        set("a", "a", "a")
        cache["a"]!!.close()
        val toAbort = cache.edit("a")!!
        toAbort.setString(0, "b")
        toAbort.abort()
        val editor = cache.edit("a")!!
        editor.setString(1, "a2")
        editor.commit()
        assertValue("a", "a", "a2")
    }

    @Test
    fun editSinceEvicted() {
        cache.close()
        createNewCache(10)
        set("a", "aa", "aaa") // size 5
        set("b", "bb", "bbb") // size 5
        set("c", "cc", "ccc") // size 5; will evict 'A'
        cache.flush()
        assertNotNull(cache.edit("a"))
    }

    @Test
    fun editSinceEvictedAndRecreated() {
        cache.close()
        createNewCache(10)
        set("a", "aa", "aaa") // size 5
        cache["a"]!!.close()
        set("b", "bb", "bbb") // size 5
        set("c", "cc", "ccc") // size 5; will evict 'A'
        set("a", "a", "aaaa") // size 5; will evict 'B'
        cache.flush()
        assertNotNull(cache.edit("a"))
    }

    @Test
    fun removeHandlesMissingFile() {
        set("a", "a", "a")
        fileSystem.delete(getCleanFile("a", 0))
        cache.remove("a")
    }

    /** https://github.com/JakeWharton/DiskLruCache/issues/2 */
    @Test
    fun aggressiveClearingHandlesRead() {
        set("a", "a", "a")
        fileSystem.deleteRecursively(cacheDir)
        assertNull(cache["a"])
    }

    /** https://github.com/JakeWharton/DiskLruCache/issues/2 */
    @Test
    fun aggressiveClearingHandlesWrite() {
        fileSystem.deleteRecursively(cacheDir)
        set("a", "a", "a")
        assertValue("a", "a", "a")
    }

    /** https://github.com/JakeWharton/DiskLruCache/issues/2 */
    @Test
    fun aggressiveClearingHandlesEdit() {
        set("a", "a", "a")
        val a = cache.edit("a")!!
        fileSystem.deleteRecursively(cacheDir)
        a.setString(1, "a2")
        a.commit()
        assertValue("a", "", "a2")
    }

    /** https://github.com/JakeWharton/DiskLruCache/issues/2 */
    @Test
    fun aggressiveClearingHandlesPartialEdit() {
        set("a", "a", "a")
        set("b", "b", "b")
        val a = cache.edit("a")!!
        a.setString(0, "a1")
        fileSystem.deleteRecursively(cacheDir)
        a.setString(1, "a2")
        a.commit()
        assertNull(cache["a"])
    }

    /**
     * We had a long-lived bug where [DiskLruCache.trimToSize] could infinite loop if entries
     * being edited required deletion for the operation to complete.
     */
    @Test
    fun trimToSizeWithActiveEdit() {
        val expectedByteCount = 10L
        val afterRemoveFileContents = "a1234"

        createNewCache(8) // Smaller than the sum of active edits!
        set("a", "a1234", "a1234")
        val a = cache.edit("a")!!
        a.setString(0, "a123")
        cache.flush() // Force trimToSize().
        assertEquals(expectedByteCount, cache.size())
        assertEquals(afterRemoveFileContents, readFileOrNull(getCleanFile("a", 0)))
        assertEquals(afterRemoveFileContents, readFileOrNull(getCleanFile("a", 1)))

        // After the edit is completed, its entry is still gone.
        a.setString(1, "a1")
        a.commit()
        assertAbsent("a")
        assertEquals(0, cache.size())
    }

    @Test
    fun evictAll() {
        set("a", "a", "a")
        set("b", "b", "b")
        cache.evictAll()
        assertEquals(0, cache.size())
        assertAbsent("a")
        assertAbsent("b")
    }

    @Test
    fun evictAllWithPartialCreate() {
        val a = cache.edit("a")!!
        a.setString(0, "a1")
        a.setString(1, "a2")
        cache.evictAll()
        assertEquals(0, cache.size())
        a.commit()
        assertAbsent("a")
    }

    @Test
    fun evictAllWithPartialEditDoesNotStoreAValue() {
        val expectedByteCount = 2L

        set("a", "a", "a")
        val a = cache.edit("a")!!
        a.setString(0, "a1")
        a.setString(1, "a2")
        cache.evictAll()
        assertEquals(expectedByteCount, cache.size())
        a.commit()
        assertAbsent("a")
    }

    @Test
    fun evictAllDoesntInterruptPartialRead() {
        val expectedByteCount = 2L
        val afterRemoveFileContents = "a"

        set("a", "a", "a")
        cache["a"]!!.use {
            it.assertValue(0, "a")
            cache.evictAll()
            assertEquals(expectedByteCount, cache.size())
            assertEquals(afterRemoveFileContents, readFileOrNull(getCleanFile("a", 0)))
            assertEquals(afterRemoveFileContents, readFileOrNull(getCleanFile("a", 1)))
            it.assertValue(1, "a")
        }
        assertEquals(0L, cache.size())
    }

    @Test
    fun editSnapshotAfterEvictAllReturnsNullDueToStaleValue() {
        val expectedByteCount = 2L
        val afterRemoveFileContents = "a"

        set("a", "a", "a")
        cache["a"]!!.use { snapshot ->
            cache.evictAll()
            assertEquals(expectedByteCount, cache.size())
            assertEquals(afterRemoveFileContents, readFileOrNull(getCleanFile("a", 0)))
            assertEquals(afterRemoveFileContents, readFileOrNull(getCleanFile("a", 1)))
            assertNull(cache.edit(snapshot.entry.key))
        }
        assertEquals(0L, cache.size())
    }

    @Test
    fun isClosed_uninitializedCache() {
        // Create an uninitialized cache.
        cache = DiskLruCache(fileSystem, cacheDir, dispatcher, Long.MAX_VALUE, 100, 2)
        caches += cache
        assertFalse(cache.isClosed())
        cache.close()
        assertTrue(cache.isClosed())
    }

    @Test
    fun journalWriteFailsDuringEdit() {
        set("a", "a", "a")
        set("b", "b", "b")

        // We can't begin the edit if writing 'DIRTY' fails.
        fileSystem.setFaultyWrite(journalFile, true)
        assertNull(cache.edit("c"))

        // Once the journal has a failure, subsequent writes aren't permitted.
        fileSystem.setFaultyWrite(journalFile, false)
        assertNull(cache.edit("d"))

        // Confirm that the fault didn't corrupt entries stored before the fault was introduced.
        cache.close()
        cache = DiskLruCache(fileSystem, cacheDir, dispatcher, Long.MAX_VALUE, 100, 2)
        caches += cache
        assertValue("a", "a", "a")
        assertValue("b", "b", "b")
        assertAbsent("c")
        assertAbsent("d")
    }

    /**
     * We had a bug where the cache was left in an inconsistent state after a journal write failed.
     * https://github.com/square/okhttp/issues/1211
     */
    @Test
    fun journalWriteFailsDuringEditorCommit() {
        set("a", "a", "a")
        set("b", "b", "b")

        // Create an entry that fails to write to the journal during commit.
        val editor = cache.edit("c")!!
        editor.setString(0, "c")
        editor.setString(1, "c")
        fileSystem.setFaultyWrite(journalFile, true)
        editor.commit()

        // Once the journal has a failure, subsequent writes aren't permitted.
        fileSystem.setFaultyWrite(journalFile, false)
        assertNull(cache.edit("d"))

        // Confirm that the fault didn't corrupt entries stored before the fault was introduced.
        cache.close()
        cache = DiskLruCache(fileSystem, cacheDir, dispatcher, Long.MAX_VALUE, 100, 2)
        caches += cache
        assertValue("a", "a", "a")
        assertValue("b", "b", "b")
        assertAbsent("c")
        assertAbsent("d")
    }

    @Test
    fun journalWriteFailsDuringEditorAbort() {
        set("a", "a", "a")
        set("b", "b", "b")

        // Create an entry that fails to write to the journal during abort.
        val editor = cache.edit("c")!!
        editor.setString(0, "c")
        editor.setString(1, "c")
        fileSystem.setFaultyWrite(journalFile, true)
        editor.abort()

        // Once the journal has a failure, subsequent writes aren't permitted.
        fileSystem.setFaultyWrite(journalFile, false)
        assertNull(cache.edit("d"))

        // Confirm that the fault didn't corrupt entries stored before the fault was introduced.
        cache.close()
        cache = DiskLruCache(fileSystem, cacheDir, dispatcher, Long.MAX_VALUE, 100, 2)
        caches += cache
        assertValue("a", "a", "a")
        assertValue("b", "b", "b")
        assertAbsent("c")
        assertAbsent("d")
    }

    @Test
    fun journalWriteFailsDuringRemove() {
        set("a", "a", "a")
        set("b", "b", "b")

        // Remove, but the journal write will fail.
        fileSystem.setFaultyWrite(journalFile, true)
        assertTrue(cache.remove("a"))

        // Confirm that the entry was still removed.
        fileSystem.setFaultyWrite(journalFile, false)
        cache.close()
        cache = DiskLruCache(fileSystem, cacheDir, dispatcher, Long.MAX_VALUE, 100, 2)
        caches += cache
        assertAbsent("a")
        assertValue("b", "b", "b")
    }

    @Test
    fun cleanupTrimFailurePreventsNewEditors() {
        createNewCache(8)
        set("a", "aa", "aa")
        set("b", "bb", "bbb")

        // Cause the cache trim job to fail.
        fileSystem.setFaultyDelete(cacheDir / "a.0", true)
        dispatcher.runNextTask()

        // Confirm that edits are prevented after a cache trim failure.
        assertNull(cache.edit("a"))
        assertNull(cache.edit("b"))
        assertNull(cache.edit("c"))

        // Allow the test to clean up.
        fileSystem.setFaultyDelete(cacheDir / "a.0", false)
    }

    @Test
    fun cleanupTrimFailureRetriedOnEditors() {
        createNewCache(8)
        set("a", "aa", "aa")
        set("b", "bb", "bbb")

        // Cause the cache trim job to fail.
        fileSystem.setFaultyDelete(cacheDir / "a.0", true)
        dispatcher.runNextTask()

        // An edit should now add a job to clean up if the most recent trim failed.
        assertNull(cache.edit("b"))
        dispatcher.runNextTask()

        // Confirm a successful cache trim now allows edits.
        fileSystem.setFaultyDelete(cacheDir / "a.0", false)
        assertNull(cache.edit("c"))
        dispatcher.runNextTask()
        set("c", "cc", "cc")
        assertValue("c", "cc", "cc")
    }

    @Test
    fun cleanupTrimFailureWithInFlightEditor() {
        createNewCache(8)
        set("a", "aa", "aaa")
        set("b", "bb", "bb")
        val inFlightEditor = cache.edit("c")!!

        // Cause the cache trim job to fail.
        fileSystem.setFaultyDelete(cacheDir / "a.0", true)
        dispatcher.runNextTask()

        // The in-flight editor can still write after a trim failure.
        inFlightEditor.setString(0, "cc")
        inFlightEditor.setString(1, "cc")
        inFlightEditor.commit()

        // Confirm the committed values are present after a successful cache trim.
        fileSystem.setFaultyDelete(cacheDir / "a.0", false)
        dispatcher.runNextTask()
        assertValue("c", "cc", "cc")
    }

    @Test
    fun cleanupTrimFailureAllowsSnapshotReads() {
        createNewCache(8)
        set("a", "aa", "aa")
        set("b", "bb", "bbb")

        // Cause the cache trim job to fail.
        fileSystem.setFaultyDelete(cacheDir / "a.0", true)
        dispatcher.runNextTask()

        // Confirm we still allow snapshot reads after a trim failure.
        assertValue("a", "aa", "aa")
        assertValue("b", "bb", "bbb")

        // Allow the test to clean up.
        fileSystem.setFaultyDelete(cacheDir / "a.0", false)
    }

    @Test
    fun cleanupTrimFailurePreventsSnapshotWrites() {
        createNewCache(8)
        set("a", "aa", "aa")
        set("b", "bb", "bbb")

        // Cause the cache trim job to fail.
        fileSystem.setFaultyDelete(cacheDir / "a.0", true)
        dispatcher.runNextTask()

        // Confirm snapshot writes are prevented after a trim failure.
        cache["a"]!!.use {
            assertNull(cache.edit(it.entry.key))
        }
        cache["b"]!!.use {
            assertNull(cache.edit(it.entry.key))
        }

        // Allow the test to clean up.
        fileSystem.setFaultyDelete(cacheDir / "a.0", false)
    }

    @Test
    fun evictAllAfterCleanupTrimFailure() {
        createNewCache(8)
        set("a", "aa", "aa")
        set("b", "bb", "bbb")

        // Cause the cache trim job to fail.
        fileSystem.setFaultyDelete(cacheDir / "a.0", true)
        dispatcher.runNextTask()

        // Confirm we prevent edits after a trim failure.
        assertNull(cache.edit("c"))

        // A successful eviction should allow new writes.
        fileSystem.setFaultyDelete(cacheDir / "a.0", false)
        cache.evictAll()
        set("c", "cc", "cc")
        assertValue("c", "cc", "cc")
    }

    @Test
    fun manualRemovalAfterCleanupTrimFailure() {
        createNewCache(8)
        set("a", "aa", "aa")
        set("b", "bb", "bbb")

        // Cause the cache trim job to fail.
        fileSystem.setFaultyDelete(cacheDir / "a.0", true)
        dispatcher.runNextTask()

        // Confirm we prevent edits after a trim failure.
        assertNull(cache.edit("c"))

        // A successful removal which trims the cache should allow new writes.
        fileSystem.setFaultyDelete(cacheDir / "a.0", false)
        cache.remove("a")
        set("c", "cc", "cc")
        assertValue("c", "cc", "cc")
    }

    @Test
    fun flushingAfterCleanupTrimFailure() {
        createNewCache(8)
        set("a", "aa", "aa")
        set("b", "bb", "bbb")

        // Cause the cache trim job to fail.
        fileSystem.setFaultyDelete(cacheDir / "a.0", true)
        dispatcher.runNextTask()

        // Confirm we prevent edits after a trim failure.
        assertNull(cache.edit("c"))

        // A successful flush trims the cache and should allow new writes.
        fileSystem.setFaultyDelete(cacheDir / "a.0", false)
        cache.flush()
        set("c", "cc", "cc")
        assertValue("c", "cc", "cc")
    }

    @Test
    fun cleanupTrimFailureWithPartialSnapshot() {
        createNewCache(8)
        set("a", "aa", "aa")
        set("b", "bb", "bbb")

        // Cause the cache trim to fail on the second value leaving a partial snapshot.
        fileSystem.setFaultyDelete(cacheDir / "a.1", true)
        dispatcher.runNextTask()

        // Confirm the partial snapshot is not returned.
        assertNull(cache["a"])

        // Confirm we prevent edits after a trim failure.
        assertNull(cache.edit("a"))

        // Confirm the partial snapshot is not returned after a successful trim.
        fileSystem.setFaultyDelete(cacheDir / "a.1", false)
        dispatcher.runNextTask()
        assertNull(cache["a"])
    }

    @Test
    fun `edit discarded after editor detached`() {
        set("k1", "a", "a")

        // Create an editor, then detach it.
        val editor = cache.edit("k1")!!
        editor.newSink(0).buffer().use { sink ->
            cache.evictAll()

            // Complete the original edit. It goes into a black hole.
            sink.writeUtf8("bb")
        }
        assertNull(cache["k1"])
    }

    @Test
    fun abortAfterDetach() {
        set("k1", "a", "a")
        val editor = cache.edit("k1")!!
        cache.evictAll()
        editor.abort()
        assertEquals(0, cache.size())
        assertAbsent("k1")
    }

    @Test
    fun dontRemoveUnfinishedEntryWhenCreatingSnapshot() {
        val creator = cache.edit("k1")!!
        creator.setString(0, "ABC")
        creator.setString(1, "DE")
        val snapshotWhileEditing = cache["k1"]
        assertNull(snapshotWhileEditing) // entry still is being created/edited
        creator.commit()
        val snapshotAfterCommit = cache["k1"]
        assertNotNull(snapshotAfterCommit, "Entry has been removed during creation.")
    }

    @Test
    fun `cannot read while writing`() {
        set("k1", "a", "a")
        val editor = cache.edit("k1")!!
        assertNull(cache["k1"])
        editor.commit()
    }

    @Test
    fun `cannot write while reading`() {
        set("k1", "a", "a")
        val snapshot = cache["k1"]!!
        assertNull(cache.edit("k1"))
        snapshot.close()
    }

    @Test
    fun `can read while reading`() {
        set("k1", "a", "a")
        cache["k1"]!!.use { snapshot1 ->
            snapshot1.assertValue(0, "a")
            cache["k1"]!!.use { snapshot2 ->
                snapshot2.assertValue(0, "a")
                snapshot1.assertValue(1, "a")
                snapshot2.assertValue(1, "a")
            }
        }
    }

    @Test
    fun `remove while reading creates zombie that is removed when read finishes`() {
        val afterRemoveFileContents = "a"

        set("k1", "a", "a")
        cache["k1"]!!.use { snapshot ->
            cache.remove("k1")

            // On Windows files still exist with open with 2 open sources.
            assertEquals(afterRemoveFileContents, readFileOrNull(getCleanFile("k1", 0)))
            assertNull(readFileOrNull(getDirtyFile("k1", 0)))

            // On Windows files still exist with open with 1 open source.
            snapshot.assertValue(0, "a")
            assertEquals(afterRemoveFileContents, readFileOrNull(getCleanFile("k1", 0)))
            assertNull(readFileOrNull(getDirtyFile("k1", 0)))

            // On all platforms files are deleted when all sources are closed.
            snapshot.assertValue(1, "a")
            snapshot.close()
            assertNull(readFileOrNull(getCleanFile("k1", 0)))
            assertNull(readFileOrNull(getDirtyFile("k1", 0)))
        }
    }

    @Test
    fun `remove while writing creates zombie that is removed when write finishes`() {
        val afterRemoveFileContents = "a"

        set("k1", "a", "a")
        val editor = cache.edit("k1")!!
        cache.remove("k1")
        assertNull(cache["k1"])

        // On Windows files still exist while being edited.
        assertEquals(afterRemoveFileContents, readFileOrNull(getCleanFile("k1", 0)))
        assertNull(readFileOrNull(getDirtyFile("k1", 0)))

        // On all platforms files are deleted when the edit completes.
        editor.commit()
        assertNull(readFileOrNull(getCleanFile("k1", 0)))
        assertNull(readFileOrNull(getDirtyFile("k1", 0)))
    }

    @Test
    fun `cannot read zombie entry`() {
        set("k1", "a", "a")
        cache["k1"]!!.use {
            cache.remove("k1")
            assertNull(cache["k1"])
        }
    }

    @Test
    fun `cannot write zombie entry`() {
        set("k1", "a", "a")
        cache["k1"]!!.use {
            cache.remove("k1")
            assertNull(cache.edit("k1"))
        }
    }

    @Test
    fun `close with zombie read`() {
        val afterRemoveFileContents = "a"

        set("k1", "a", "a")
        cache["k1"]!!.use { snapshot ->
            cache.remove("k1")

            // After we close the cache the files continue to exist!
            cache.close()
            assertEquals(afterRemoveFileContents, readFileOrNull(getCleanFile("k1", 0)))
            assertNull(readFileOrNull(getDirtyFile("k1", 0)))

            // But they disappear when the sources are closed.
            snapshot.assertValue(0, "a")
            snapshot.assertValue(1, "a")
            snapshot.close()
            assertNull(readFileOrNull(getCleanFile("k1", 0)))
            assertNull(readFileOrNull(getDirtyFile("k1", 0)))
        }
    }

    @Test
    fun `close with zombie write`() {
        val afterRemoveCleanFileContents = "a"
        val afterRemoveDirtyFileContents = ""

        set("k1", "a", "a")
        val editor = cache.edit("k1")!!
        val sink0 = editor.newSink(0)
        cache.remove("k1")

        // After we close the cache the files continue to exist!
        cache.close()
        assertEquals(afterRemoveCleanFileContents, readFileOrNull(getCleanFile("k1", 0)))
        assertEquals(afterRemoveDirtyFileContents, readFileOrNull(getDirtyFile("k1", 0)))

        // But they disappear when the edit completes.
        sink0.close()
        editor.commit()
        assertNull(readFileOrNull(getCleanFile("k1", 0)))
        assertNull(readFileOrNull(getDirtyFile("k1", 0)))
    }

    @Test
    fun `close with completed zombie write`() {
        val afterRemoveCleanFileContents = "a"
        val afterRemoveDirtyFileContents = "b"

        set("k1", "a", "a")
        val editor = cache.edit("k1")!!
        editor.setString(0, "b")
        cache.remove("k1")

        // After we close the cache the files continue to exist!
        cache.close()
        assertEquals(afterRemoveCleanFileContents, readFileOrNull(getCleanFile("k1", 0)))
        assertEquals(afterRemoveDirtyFileContents, readFileOrNull(getDirtyFile("k1", 0)))

        // But they disappear when the edit completes.
        editor.commit()
        assertNull(readFileOrNull(getCleanFile("k1", 0)))
        assertNull(readFileOrNull(getDirtyFile("k1", 0)))
    }

    private fun assertJournalEquals(vararg expectedBodyLines: String) {
        val actual = readJournalLines()
        val expected = listOf(DiskLruCache.MAGIC, DiskLruCache.VERSION, "100", "2", "") + expectedBodyLines
        assertEquals(expected, actual)
    }

    private fun createJournal(vararg bodyLines: String) {
        createJournalWithHeader(DiskLruCache.MAGIC, DiskLruCache.VERSION, "100",
            "2", "", *bodyLines)
    }

    @Suppress("SameParameterValue")
    private fun createJournalWithHeader(
        magic: String,
        version: String,
        appVersion: String,
        valueCount: String,
        blank: String,
        vararg bodyLines: String
    ) {
        fileSystem.write(journalFile) {
            writeUtf8(
                """
                |$magic
                |$version
                |$appVersion
                |$valueCount
                |$blank
                |""".trimMargin()
            )
            for (line in bodyLines) {
                writeUtf8(line)
                writeUtf8("\n")
            }
        }
    }

    private fun readJournalLines(): List<String> {
        val result = mutableListOf<String>()
        fileSystem.read(journalFile) {
            while (true) {
                val line = readUtf8Line() ?: break
                result.add(line)
            }
        }
        return result
    }

    private fun getCleanFile(key: String, index: Int) = cacheDir / "$key.$index"

    private fun getDirtyFile(key: String, index: Int) = cacheDir / "$key.$index.tmp"

    private fun readFile(file: Path): String {
        return fileSystem.read(file) {
            readUtf8()
        }
    }

    private fun readFileOrNull(file: Path): String? {
        return try {
            fileSystem.read(file) {
                readUtf8()
            }
        } catch (_: FileNotFoundException) {
            null
        }
    }

    fun writeFile(file: Path, content: String) {
        file.parent?.let {
            fileSystem.createDirectories(it)
        }
        fileSystem.write(file) {
            writeUtf8(content)
        }
    }

    private fun generateSomeGarbageFiles() {
        val dir1 = cacheDir / "dir1"
        val dir2 = dir1 / "dir2"
        writeFile(getCleanFile("g1", 0), "A")
        writeFile(getCleanFile("g1", 1), "B")
        writeFile(getCleanFile("g2", 0), "C")
        writeFile(getCleanFile("g2", 1), "D")
        writeFile(getCleanFile("g2", 1), "D")
        writeFile(cacheDir / "otherFile0", "E")
        writeFile(dir2 / "otherFile1", "F")
    }

    private fun assertGarbageFilesAllDeleted() {
        assertFalse(fileSystem.exists(getCleanFile("g1", 0)))
        assertFalse(fileSystem.exists(getCleanFile("g1", 1)))
        assertFalse(fileSystem.exists(getCleanFile("g2", 0)))
        assertFalse(fileSystem.exists(getCleanFile("g2", 1)))
        assertFalse(fileSystem.exists(cacheDir / "otherFile0"))
        assertFalse(fileSystem.exists(cacheDir / "dir1"))
    }

    private fun set(key: String, value0: String, value1: String) {
        val editor = cache.edit(key)!!
        editor.setString(0, value0)
        editor.setString(1, value1)
        editor.commit()
    }

    private fun assertAbsent(key: String) {
        val snapshot = cache[key]
        if (snapshot != null) {
            snapshot.close()
            fail("")
        }
        assertFalse(fileSystem.exists(getCleanFile(key, 0)))
        assertFalse(fileSystem.exists(getCleanFile(key, 1)))
        assertFalse(fileSystem.exists(getDirtyFile(key, 0)))
        assertFalse(fileSystem.exists(getDirtyFile(key, 1)))
    }

    private fun assertValue(key: String, value0: String, value1: String) {
        cache[key]!!.use { snapshot ->
            snapshot.assertValue(0, value0)
            snapshot.assertValue(1, value1)
            assertTrue(fileSystem.exists(getCleanFile(key, 0)))
            assertTrue(fileSystem.exists(getCleanFile(key, 1)))
        }
    }

    private fun Snapshot.assertValue(index: Int, value: String) {
        getSource(index).use { source ->
            assertEquals(value, sourceAsString(source))
            assertEquals(value.length.toLong(), entry.lengths[index])
        }
    }

    private fun sourceAsString(source: Source) = source.buffer().readUtf8()

    private fun Editor.assertInoperable() {
        try {
            setString(0, "A")
            fail("")
        } catch (_: IllegalStateException) {
        }
        try {
            newSource(0)
            fail("")
        } catch (_: IllegalStateException) {
        }
        try {
            newSink(0)
            fail("")
        } catch (_: IllegalStateException) {
        }
        try {
            commit()
            fail("")
        } catch (_: IllegalStateException) {
        }
        try {
            abort()
            fail("")
        } catch (_: IllegalStateException) {
        }
    }

    private fun Editor.setString(index: Int, value: String) {
        newSink(index).buffer().use { it.writeUtf8(value) }
    }

    private fun DiskLruCache.Editor.newSink(index: Int) = fileSystem.sink(file(index))

    private fun DiskLruCache.Editor.newSource(index: Int) = fileSystem.source(file(index))

    private fun DiskLruCache.Snapshot.getSource(index: Int) = fileSystem.source(file(index))

    private fun DiskLruCache.isClosed(): Boolean {
        try {
            get("") // Intentionally use an invalid key.
            return false
        } catch (e: Exception) {
            return e.message == "cache is closed"
        }
    }
}
