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

import androidx.annotation.VisibleForTesting
import coil.disk.DiskLruCache.Editor
import coil.util.createFile
import coil.util.deleteContents
import coil.util.forEachIndices
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okio.BufferedSink
import okio.Closeable
import okio.EOFException
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.IOException
import okio.Path
import okio.Sink
import okio.blackholeSink
import okio.buffer
import java.io.Flushable

/**
 * A cache that uses a bounded amount of space on a filesystem. Each cache entry has a string key
 * and a fixed number of values. Each key must match the regex `[a-z0-9_-]{1,64}`. Values are byte
 * sequences, accessible as streams or files. Each value must be between `0` and `Int.MAX_VALUE`
 * bytes in length.
 *
 * The cache stores its data in a directory on the filesystem. This directory must be exclusive to
 * the cache; the cache may delete or overwrite files from its directory. It is an error for
 * multiple processes to use the same cache directory at the same time.
 *
 * This cache limits the number of bytes that it will store on the filesystem. When the number of
 * stored bytes exceeds the limit, the cache will remove entries in the background until the limit
 * is satisfied. The limit is not strict: the cache may temporarily exceed it while waiting for
 * files to be deleted. The limit does not include filesystem overhead or the cache journal so
 * space-sensitive applications should set a conservative limit.
 *
 * Clients call [edit] to create or update the values of an entry. An entry may have only one editor
 * at one time; if a value is not available to be edited then [edit] will return null.
 *
 *  * When an entry is being **created** it is necessary to supply a full set of values; the empty
 *    value should be used as a placeholder if necessary.
 *
 *  * When an entry is being **edited**, it is not necessary to supply data for every value; values
 *    default to their previous value.
 *
 * Every [edit] call must be matched by a call to [Editor.commit] or [Editor.abort]. Committing is
 * atomic: a read observes the full set of values as they were before or after the commit, but never
 * a mix of values.
 *
 * Clients call [get] to read a snapshot of an entry. The read will observe the value at the time
 * that [get] was called. Updates and removals after the call do not impact ongoing reads.
 *
 * This class is tolerant of some I/O errors. If files are missing from the filesystem, the
 * corresponding entries will be dropped from the cache. If an error occurs while writing a cache
 * value, the edit will fail silently. Callers should handle other problems by catching
 * `IOException` and responding appropriately.
 *
 * @constructor Create a cache which will reside in [directory]. This cache is lazily initialized on
 *  first access and will be created if it does not exist.
 * @param directory a writable directory.
 * @param cleanupDispatcher the dispatcher to run cache size trim operations on.
 * @param valueCount the number of values per cache entry. Must be positive.
 * @param maxSize the maximum number of bytes this cache should use to store.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class DiskLruCache(
    fileSystem: FileSystem,
    private val directory: Path,
    cleanupDispatcher: CoroutineDispatcher,
    private val maxSize: Long,
    private val appVersion: Int,
    private val valueCount: Int,
) : Closeable, Flushable {

    /*
     * This cache uses a journal file named "journal". A typical journal file looks like this:
     *
     *     libcore.io.DiskLruCache
     *     1
     *     100
     *     2
     *
     *     CLEAN 3400330d1dfc7f3f7f4b8d4d803dfcf6 832 21054
     *     DIRTY 335c4c6028171cfddfbaae1a9c313c52
     *     CLEAN 335c4c6028171cfddfbaae1a9c313c52 3934 2342
     *     REMOVE 335c4c6028171cfddfbaae1a9c313c52
     *     DIRTY 1ab96a171faeeee38496d8b330771a7a
     *     CLEAN 1ab96a171faeeee38496d8b330771a7a 1600 234
     *     READ 335c4c6028171cfddfbaae1a9c313c52
     *     READ 3400330d1dfc7f3f7f4b8d4d803dfcf6
     *
     * The first five lines of the journal form its header. They are the constant string
     * "libcore.io.DiskLruCache", the disk cache's version, the application's version, the value
     * count, and a blank line.
     *
     * Each of the subsequent lines in the file is a record of the state of a cache entry. Each line
     * contains space-separated values: a state, a key, and optional state-specific values.
     *
     *   o DIRTY lines track that an entry is actively being created or updated. Every successful
     *     DIRTY action should be followed by a CLEAN or REMOVE action. DIRTY lines without a matching
     *     CLEAN or REMOVE indicate that temporary files may need to be deleted.
     *
     *   o CLEAN lines track a cache entry that has been successfully published and may be read. A
     *     publish line is followed by the lengths of each of its values.
     *
     *   o READ lines track accesses for LRU.
     *
     *   o REMOVE lines track entries that have been deleted.
     *
     * The journal file is appended to as cache operations occur. The journal may occasionally be
     * compacted by dropping redundant lines. A temporary file named "journal.tmp" will be used during
     * compaction; that file should be deleted if it exists when the cache is opened.
     */

    init {
        require(maxSize > 0L) { "maxSize <= 0" }
        require(valueCount > 0) { "valueCount <= 0" }
    }

    private val journalFile = directory / JOURNAL_FILE
    private val journalFileTmp = directory / JOURNAL_FILE_TMP
    private val journalFileBackup = directory / JOURNAL_FILE_BACKUP
    private val lruEntries = LinkedHashMap<String, Entry>(0, 0.75f, true)
    private val cleanupScope = CoroutineScope(SupervisorJob() + cleanupDispatcher.limitedParallelism(1))
    private var size = 0L
    private var operationsSinceRewrite = 0
    private var journalWriter: BufferedSink? = null
    private var hasJournalErrors = false
    private var initialized = false
    private var closed = false
    private var mostRecentTrimFailed = false
    private var mostRecentRebuildFailed = false

    private val fileSystem = object : ForwardingFileSystem(fileSystem) {
        override fun sink(file: Path, mustCreate: Boolean): Sink {
            // Ensure the parent directory exists.
            file.parent?.let(::createDirectories)
            return super.sink(file, mustCreate)
        }
    }

    @Synchronized
    fun initialize() {
        if (initialized) return

        // If a temporary file exists, delete it.
        fileSystem.delete(journalFileTmp)

        // If a backup file exists, use it instead.
        if (fileSystem.exists(journalFileBackup)) {
            // If journal file also exists just delete backup file.
            if (fileSystem.exists(journalFile)) {
                fileSystem.delete(journalFileBackup)
            } else {
                fileSystem.atomicMove(journalFileBackup, journalFile)
            }
        }

        // Prefer to pick up where we left off.
        if (fileSystem.exists(journalFile)) {
            try {
                readJournal()
                processJournal()
                initialized = true
                return
            } catch (_: IOException) {
                // The journal is corrupt.
            }

            // The cache is corrupted; attempt to delete the contents of the directory.
            // This can throw and we'll let that propagate out as it likely means there
            // is a severe filesystem problem.
            try {
                delete()
            } finally {
                closed = false
            }
        }

        writeJournal()
        initialized = true
    }

    /**
     * Reads the journal and initializes [lruEntries].
     */
    private fun readJournal() {
        fileSystem.read(journalFile) {
            val magic = readUtf8LineStrict()
            val version = readUtf8LineStrict()
            val appVersionString = readUtf8LineStrict()
            val valueCountString = readUtf8LineStrict()
            val blank = readUtf8LineStrict()

            if (MAGIC != magic ||
                VERSION != version ||
                appVersion.toString() != appVersionString ||
                valueCount.toString() != valueCountString ||
                blank.isNotEmpty()
            ) {
                throw IOException("unexpected journal header: " +
                    "[$magic, $version, $appVersionString, $valueCountString, $blank]")
            }

            var lineCount = 0
            while (true) {
                try {
                    readJournalLine(readUtf8LineStrict())
                    lineCount++
                } catch (_: EOFException) {
                    break // End of journal.
                }
            }

            operationsSinceRewrite = lineCount - lruEntries.size

            // If we ended on a truncated line, rebuild the journal before appending to it.
            if (!exhausted()) {
                writeJournal()
            } else {
                journalWriter = newJournalWriter()
            }
        }
    }

    private fun newJournalWriter(): BufferedSink {
        val fileSink = fileSystem.appendingSink(journalFile)
        val faultHidingSink = FaultHidingSink(fileSink) {
            hasJournalErrors = true
        }
        return faultHidingSink.buffer()
    }

    private fun readJournalLine(line: String) {
        val firstSpace = line.indexOf(' ')
        if (firstSpace == -1) throw IOException("unexpected journal line: $line")

        val keyBegin = firstSpace + 1
        val secondSpace = line.indexOf(' ', keyBegin)
        val key: String
        if (secondSpace == -1) {
            key = line.substring(keyBegin)
            if (firstSpace == REMOVE.length && line.startsWith(REMOVE)) {
                lruEntries.remove(key)
                return
            }
        } else {
            key = line.substring(keyBegin, secondSpace)
        }

        val entry = lruEntries.getOrPut(key) { Entry(key) }
        when {
            secondSpace != -1 && firstSpace == CLEAN.length && line.startsWith(CLEAN) -> {
                val parts = line.substring(secondSpace + 1).split(' ')
                entry.readable = true
                entry.currentEditor = null
                entry.setLengths(parts)
            }
            secondSpace == -1 && firstSpace == DIRTY.length && line.startsWith(DIRTY) -> {
                entry.currentEditor = Editor(entry)
            }
            secondSpace == -1 && firstSpace == READ.length && line.startsWith(READ) -> {
                // This work was already done by calling lruEntries.get().
            }
            else -> throw IOException("unexpected journal line: $line")
        }
    }

    /**
     * Computes the current size and collects garbage as a part of initializing the cache.
     * Dirty entries are assumed to be inconsistent and will be deleted.
     */
    private fun processJournal() {
        var size = 0L
        val iterator = lruEntries.values.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.currentEditor == null) {
                for (i in 0 until valueCount) {
                    size += entry.lengths[i]
                }
            } else {
                entry.currentEditor = null
                for (i in 0 until valueCount) {
                    fileSystem.delete(entry.cleanFiles[i])
                    fileSystem.delete(entry.dirtyFiles[i])
                }
                iterator.remove()
            }
        }
        this.size = size
    }

    /**
     * Writes [lruEntries] to a new journal file. This replaces the current journal if it exists.
     */
    @Synchronized
    private fun writeJournal() {
        journalWriter?.close()

        fileSystem.write(journalFileTmp) {
            writeUtf8(MAGIC).writeByte('\n'.code)
            writeUtf8(VERSION).writeByte('\n'.code)
            writeDecimalLong(appVersion.toLong()).writeByte('\n'.code)
            writeDecimalLong(valueCount.toLong()).writeByte('\n'.code)
            writeByte('\n'.code)

            for (entry in lruEntries.values) {
                if (entry.currentEditor != null) {
                    writeUtf8(DIRTY)
                    writeByte(' '.code)
                    writeUtf8(entry.key)
                    writeByte('\n'.code)
                } else {
                    writeUtf8(CLEAN)
                    writeByte(' '.code)
                    writeUtf8(entry.key)
                    entry.writeLengths(this)
                    writeByte('\n'.code)
                }
            }
        }

        if (fileSystem.exists(journalFile)) {
            fileSystem.atomicMove(journalFile, journalFileBackup)
            fileSystem.atomicMove(journalFileTmp, journalFile)
            fileSystem.delete(journalFileBackup)
        } else {
            fileSystem.atomicMove(journalFileTmp, journalFile)
        }

        journalWriter = newJournalWriter()
        operationsSinceRewrite = 0
        hasJournalErrors = false
        mostRecentRebuildFailed = false
    }

    /**
     * Returns a snapshot of the entry named [key], or null if it doesn't exist or is not currently
     * readable. If a value is returned, it is moved to the head of the LRU queue.
     */
    @Synchronized
    operator fun get(key: String): Snapshot? {
        checkNotClosed()
        validateKey(key)
        initialize()

        val snapshot = lruEntries[key]?.snapshot() ?: return null

        operationsSinceRewrite++
        journalWriter!!.apply {
            writeUtf8(READ)
            writeByte(' '.code)
            writeUtf8(key)
            writeByte('\n'.code)
        }

        if (journalRewriteRequired()) {
            launchCleanup()
        }

        return snapshot
    }

    /** Returns an editor for the entry named [key], or null if another edit is in progress. */
    @Synchronized
    fun edit(key: String): Editor? {
        checkNotClosed()
        validateKey(key)
        initialize()

        var entry = lruEntries[key]

        if (entry?.currentEditor != null) {
            return null // Another edit is in progress.
        }

        if (entry != null && entry.lockingSnapshotCount != 0) {
            return null // We can't write this file because a reader is still reading it.
        }

        if (mostRecentTrimFailed || mostRecentRebuildFailed) {
            // The OS has become our enemy! If the trim job failed, it means we are storing more
            // data than requested by the user. Do not allow edits so we do not go over that limit
            // any further. If the journal rebuild failed, the journal writer will not be active,
            // meaning we will not be able to record the edit, causing file leaks. In both cases,
            // we want to retry the clean up so we can get out of this state!
            launchCleanup()
            return null
        }

        // Flush the journal before creating files to prevent file leaks.
        journalWriter!!.apply {
            writeUtf8(DIRTY)
            writeByte(' '.code)
            writeUtf8(key)
            writeByte('\n'.code)
            flush()
        }

        if (hasJournalErrors) {
            return null // Don't edit; the journal can't be written.
        }

        if (entry == null) {
            entry = Entry(key)
            lruEntries[key] = entry
        }
        val editor = Editor(entry)
        entry.currentEditor = editor
        return editor
    }

    /**
     * Returns the number of bytes currently being used to store the values in this cache.
     * This may be greater than the max size if a background deletion is pending.
     */
    @Synchronized
    fun size(): Long {
        initialize()
        return size
    }

    @Synchronized
    private fun completeEdit(editor: Editor, success: Boolean) {
        val entry = editor.entry
        check(entry.currentEditor == editor)

        if (success && !entry.zombie) {
            // Ensure all files that have been written to have an associated dirty file.
            for (i in 0 until valueCount) {
                if (editor.written[i] && !fileSystem.exists(entry.dirtyFiles[i])) {
                    editor.abort()
                    return
                }
            }

            // Replace the clean files with the dirty ones.
            for (i in 0 until valueCount) {
                val dirty = entry.dirtyFiles[i]
                val clean = entry.cleanFiles[i]
                if (fileSystem.exists(dirty)) {
                    fileSystem.atomicMove(dirty, clean)
                } else {
                    // Ensure every entry is complete.
                    fileSystem.createFile(entry.cleanFiles[i])
                }
                val oldLength = entry.lengths[i]
                val newLength = fileSystem.metadata(clean).size ?: 0
                entry.lengths[i] = newLength
                size = size - oldLength + newLength
            }
        } else {
            // Discard any dirty files.
            for (i in 0 until valueCount) {
                fileSystem.delete(entry.dirtyFiles[i])
            }
        }

        entry.currentEditor = null
        if (entry.zombie) {
            removeEntry(entry)
            return
        }

        operationsSinceRewrite++
        journalWriter!!.apply {
            if (success || entry.readable) {
                entry.readable = true
                writeUtf8(CLEAN)
                writeByte(' '.code)
                writeUtf8(entry.key)
                entry.writeLengths(this)
                writeByte('\n'.code)
            } else {
                lruEntries.remove(entry.key)
                writeUtf8(REMOVE)
                writeByte(' '.code)
                writeUtf8(entry.key)
                writeByte('\n'.code)
            }
            flush()
        }

        if (size > maxSize || journalRewriteRequired()) {
            launchCleanup()
        }
    }

    /**
     * We rewrite [lruEntries] to the on-disk journal after a sufficient number of operations.
     */
    private fun journalRewriteRequired() = operationsSinceRewrite >= 2000

    /**
     * Drops the entry for [key] if it exists and can be removed. If the entry for [key] is
     * currently being edited, that edit will complete normally but its value will not be stored.
     *
     * @return true if an entry was removed.
     */
    @Synchronized
    fun remove(key: String): Boolean {
        checkNotClosed()
        validateKey(key)
        initialize()

        val entry = lruEntries[key] ?: return false
        val removed = removeEntry(entry)
        if (removed && size <= maxSize) mostRecentTrimFailed = false
        return removed
    }

    private fun removeEntry(entry: Entry): Boolean {
        // If we can't delete files that are still open, mark this entry as a zombie so its files
        // will be deleted when those files are closed.
        if (entry.lockingSnapshotCount > 0) {
            // Mark this entry as 'DIRTY' so that if the process crashes this entry won't be used.
            journalWriter?.apply {
                writeUtf8(DIRTY)
                writeByte(' '.code)
                writeUtf8(entry.key)
                writeByte('\n'.code)
                flush()
            }
        }
        if (entry.lockingSnapshotCount > 0 || entry.currentEditor != null) {
            entry.zombie = true
            return true
        }

        // Prevent the edit from completing normally.
        entry.currentEditor?.detach()

        for (i in 0 until valueCount) {
            fileSystem.delete(entry.cleanFiles[i])
            size -= entry.lengths[i]
            entry.lengths[i] = 0
        }

        operationsSinceRewrite++
        journalWriter?.apply {
            writeUtf8(REMOVE)
            writeByte(' '.code)
            writeUtf8(entry.key)
            writeByte('\n'.code)
        }
        lruEntries.remove(entry.key)

        if (journalRewriteRequired()) {
            launchCleanup()
        }

        return true
    }

    private fun checkNotClosed() {
        check(!closed) { "cache is closed" }
    }

    /** Closes this cache. Stored values will remain on the filesystem. */
    @Synchronized
    override fun close() {
        if (!initialized || closed) {
            closed = true
            return
        }

        // Copying for concurrent iteration.
        for (entry in lruEntries.values.toTypedArray()) {
            if (entry.currentEditor != null) {
                // Prevent the edit from completing normally.
                entry.currentEditor?.detach()
            }
        }

        trimToSize()
        cleanupScope.cancel()
        journalWriter!!.close()
        journalWriter = null
        closed = true
    }

    @Synchronized
    override fun flush() {
        if (!initialized) return

        checkNotClosed()
        trimToSize()
        journalWriter!!.flush()
    }

    private fun trimToSize() {
        while (size > maxSize) {
            if (!removeOldestEntry()) return
        }
        mostRecentTrimFailed = false
    }

    /** Returns true if an entry was removed. This will return false if all entries are zombies. */
    private fun removeOldestEntry(): Boolean {
        for (toEvict in lruEntries.values) {
            if (!toEvict.zombie) {
                removeEntry(toEvict)
                return true
            }
        }
        return false
    }

    /**
     * Closes the cache and deletes all of its stored values. This will delete all files in the
     * cache directory including files that weren't created by the cache.
     */
    private fun delete() {
        close()
        fileSystem.deleteContents(directory)
    }

    /**
     * Deletes all stored values from the cache. In-flight edits will complete normally but their
     * values will not be stored.
     */
    @Synchronized
    fun evictAll() {
        initialize()
        // Copying for concurrent iteration.
        for (entry in lruEntries.values.toTypedArray()) {
            removeEntry(entry)
        }
        mostRecentTrimFailed = false
    }

    /**
     * Launch an asynchronous operation to trim files from the disk cache and update the journal.
     */
    private fun launchCleanup() {
        cleanupScope.launch {
            synchronized(this@DiskLruCache) {
                if (!initialized || closed) return@launch
                try {
                    trimToSize()
                } catch (_: IOException) {
                    mostRecentTrimFailed = true
                }
                try {
                    if (journalRewriteRequired()) {
                        writeJournal()
                    }
                } catch (_: IOException) {
                    mostRecentRebuildFailed = true
                    journalWriter = blackholeSink().buffer()
                }
            }
        }
    }

    private fun validateKey(key: String) {
        require(LEGAL_KEY_PATTERN matches key) {
            "keys must match regex [a-z0-9_-]{1,120}: \"$key\""
        }
    }

    /** A snapshot of the values for an entry. */
    inner class Snapshot(val entry: Entry) : Closeable {

        private var closed = false

        fun file(index: Int): Path {
            check(!closed) { "snapshot is closed" }
            return entry.cleanFiles[index]
        }

        override fun close() {
            if (!closed) {
                closed = true
                synchronized(this@DiskLruCache) {
                    entry.lockingSnapshotCount--
                    if (entry.lockingSnapshotCount == 0 && entry.zombie) {
                        removeEntry(entry)
                    }
                }
            }
        }

        fun closeAndEdit(): Editor? {
            synchronized(this@DiskLruCache) {
                close()
                return edit(entry.key)
            }
        }
    }

    /** Edits the values for an entry. */
    inner class Editor(val entry: Entry) {

        private var closed = false

        /**
         * True for a given index if that index's file has been written to.
         */
        val written = BooleanArray(valueCount)

        /**
         * Get the file to read from/write to for [index].
         * This file will become the new value for this index if committed.
         */
        fun file(index: Int): Path {
            synchronized(this@DiskLruCache) {
                check(!closed) { "editor is closed" }
                written[index] = true
                return entry.dirtyFiles[index].also(fileSystem::createFile)
            }
        }

        /**
         * Prevents this editor from completing normally.
         * This is necessary if the target entry is evicted while this editor is active.
         */
        fun detach() {
            if (entry.currentEditor == this) {
                entry.zombie = true // We can't delete it until the current edit completes.
            }
        }

        /**
         * Commits this edit so it is visible to readers.
         * This releases the edit lock so another edit may be started on the same key.
         */
        fun commit() = complete(true)

        /**
         * Commit the edit and open a new [Snapshot] atomically.
         */
        fun commitAndGet(): Snapshot? {
            synchronized(this@DiskLruCache) {
                commit()
                return get(entry.key)
            }
        }

        /**
         * Aborts this edit.
         * This releases the edit lock so another edit may be started on the same key.
         */
        fun abort() = complete(false)

        /**
         * Complete this edit either successfully or unsuccessfully.
         */
        private fun complete(success: Boolean) {
            synchronized(this@DiskLruCache) {
                check(!closed) { "editor is closed" }
                if (entry.currentEditor == this) {
                    completeEdit(this, success)
                }
                closed = true
            }
        }
    }

    inner class Entry(val key: String) {

        /** Lengths of this entry's files. */
        val lengths = LongArray(valueCount)
        val cleanFiles = ArrayList<Path>(valueCount)
        val dirtyFiles = ArrayList<Path>(valueCount)

        /** True if this entry has ever been published. */
        var readable = false

        /** True if this entry must be deleted when the current edit or read completes. */
        var zombie = false

        /**
         * The ongoing edit or null if this entry is not being edited. When setting this to null
         * the entry must be removed if it is a zombie.
         */
        var currentEditor: Editor? = null

        /**
         * Snapshots currently reading this entry before a write or delete can proceed. When
         * decrementing this to zero, the entry must be removed if it is a zombie.
         */
        var lockingSnapshotCount = 0

        init {
            // The names are repetitive so re-use the same builder to avoid allocations.
            val fileBuilder = StringBuilder(key).append('.')
            val truncateTo = fileBuilder.length
            for (i in 0 until valueCount) {
                fileBuilder.append(i)
                cleanFiles += directory / fileBuilder.toString()
                fileBuilder.append(".tmp")
                dirtyFiles += directory / fileBuilder.toString()
                fileBuilder.setLength(truncateTo)
            }
        }

        /** Set lengths using decimal numbers like "10123". */
        fun setLengths(strings: List<String>) {
            if (strings.size != valueCount) {
                throw IOException("unexpected journal line: $strings")
            }

            try {
                for (i in strings.indices) {
                    lengths[i] = strings[i].toLong()
                }
            } catch (_: NumberFormatException) {
                throw IOException("unexpected journal line: $strings")
            }
        }

        /** Append space-prefixed lengths to [writer]. */
        fun writeLengths(writer: BufferedSink) {
            for (length in lengths) {
                writer.writeByte(' '.code).writeDecimalLong(length)
            }
        }

        /** Returns a snapshot of this entry. */
        fun snapshot(): Snapshot? {
            if (!readable) return null
            if (currentEditor != null || zombie) return null

            // Ensure that the entry's files still exist.
            cleanFiles.forEachIndices { file ->
                if (!fileSystem.exists(file)) {
                    // Since the entry is no longer valid, remove it so the metadata is accurate
                    // (i.e. the cache size).
                    try {
                        removeEntry(this)
                    } catch (_: IOException) {}
                    return null
                }
            }
            lockingSnapshotCount++
            return Snapshot(this)
        }
    }

    companion object {
        @VisibleForTesting internal const val JOURNAL_FILE = "journal"
        @VisibleForTesting internal const val JOURNAL_FILE_TMP = "journal.tmp"
        @VisibleForTesting internal const val JOURNAL_FILE_BACKUP = "journal.bkp"
        @VisibleForTesting internal const val MAGIC = "libcore.io.DiskLruCache"
        @VisibleForTesting internal const val VERSION = "1"
        private const val CLEAN = "CLEAN"
        private const val DIRTY = "DIRTY"
        private const val REMOVE = "REMOVE"
        private const val READ = "READ"
        private val LEGAL_KEY_PATTERN = "[a-z0-9_-]{1,120}".toRegex()
    }
}
