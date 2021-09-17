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

import coil.disk.RealDiskCache.Editor
import coil.util.closeQuietly
import coil.util.deleteContents
import coil.util.deleteIfExists
import coil.util.forEachIndices
import okio.BufferedSink
import okio.ExperimentalFileSystem
import okio.FileNotFoundException
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.IOException
import okio.Path
import okio.Sink
import okio.Source
import okio.blackholeSink
import okio.buffer
import java.io.Closeable
import java.io.EOFException
import java.io.Flushable
import java.util.concurrent.Executors

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
 * @param valueCount the number of values per cache entry. Must be positive.
 * @param maxSize the maximum number of bytes this cache should use to store.
 */
@OptIn(ExperimentalFileSystem::class)
internal class RealDiskCache(
    fileSystem: FileSystem,
    val directory: Path,
    private val appVersion: Int,
    private val valueCount: Int,
    private val maxSize: Long
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

    private val journalFile: Path
    private val journalFileTmp: Path
    private val journalFileBackup: Path
    private val lruEntries = LinkedHashMap<String, Entry>(0, 0.75f, true)
    private var size = 0L
    private var redundantOpCount = 0
    private var journalWriter: BufferedSink? = null
    private var hasJournalErrors = false

    // Must be read and written when synchronized on 'this'.
    private var initialized = false
    private var closed = false
    private var mostRecentTrimFailed = false
    private var mostRecentRebuildFailed = false

    /**
     * To differentiate between old and current snapshots, each entry is given a sequence number
     * each time an edit is committed. A snapshot is stale if its sequence number is not equal to
     * its entry's sequence number.
     */
    private var nextSequenceNumber = 0L

    private val cleanupExecutor = Executors.newSingleThreadExecutor {
        Thread().apply { name = "${this@RealDiskCache}" }
    }
    private val cleanupTask = Runnable {
        synchronized(this@RealDiskCache) {
            if (!initialized || closed) return@Runnable
            try {
                trimToSize()
            } catch (_: IOException) {
                mostRecentTrimFailed = true
            }
            try {
                if (journalRebuildRequired()) {
                    rebuildJournal()
                    redundantOpCount = 0
                }
            } catch (_: IOException) {
                mostRecentRebuildFailed = true
                journalWriter = blackholeSink().buffer()
            }
        }
    }

    private val fileSystem = object : ForwardingFileSystem(fileSystem) {
        override fun sink(file: Path): Sink {
            file.parent?.let { if (!exists(it)) createDirectories(it) }
            return super.sink(file)
        }
    }

    init {
        require(maxSize > 0L) { "maxSize <= 0" }
        require(valueCount > 0) { "valueCount <= 0" }

        journalFile = directory / JOURNAL_FILE
        journalFileTmp = directory / JOURNAL_FILE_TEMP
        journalFileBackup = directory / JOURNAL_FILE_BACKUP
    }

    @Synchronized
    fun initialize() {
        if (initialized) return

        // If a bkp file exists, use it instead.
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

            // The cache is corrupted, attempt to delete the contents of the directory. This can
            // throw and we'll let that propagate out as it likely means there is a severe
            // filesystem problem.
            try {
                delete()
            } finally {
                closed = false
            }
        }

        rebuildJournal()

        initialized = true
    }

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
                blank.isNotEmpty()) {
                throw IOException("unexpected journal header: [$magic, $version, $valueCountString, $blank]")
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

            redundantOpCount = lineCount - lruEntries.size

            // If we ended on a truncated line, rebuild the journal before appending to it.
            if (!exhausted()) {
                rebuildJournal()
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

        val entry = lruEntries[key] ?: Entry(key).also { lruEntries[key] = it }
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
     * Computes the initial size and collects garbage as a part of opening the cache.
     * Dirty entries are assumed to be inconsistent and will be deleted.
     */
    private fun processJournal() {
        fileSystem.deleteIfExists(journalFileTmp)
        val i = lruEntries.values.iterator()
        while (i.hasNext()) {
            val entry = i.next()
            if (entry.currentEditor == null) {
                for (t in 0 until valueCount) {
                    size += entry.lengths[t]
                }
            } else {
                entry.currentEditor = null
                for (t in 0 until valueCount) {
                    fileSystem.deleteIfExists(entry.cleanFiles[t])
                    fileSystem.deleteIfExists(entry.dirtyFiles[t])
                }
                i.remove()
            }
        }
    }

    /**
     * Creates a new journal that omits redundant information.
     * This replaces the current journal if it exists.
     */
    @Synchronized
    private fun rebuildJournal() {
        journalWriter?.close()

        fileSystem.write(journalFileTmp) {
            writeUtf8(MAGIC).writeByte('\n'.code)
            writeUtf8(VERSION).writeByte('\n'.code)
            writeDecimalLong(appVersion.toLong()).writeByte('\n'.code)
            writeDecimalLong(valueCount.toLong()).writeByte('\n'.code)
            writeByte('\n'.code)

            for (entry in lruEntries.values) {
                if (entry.currentEditor != null) {
                    writeUtf8(DIRTY).writeByte(' '.code)
                    writeUtf8(entry.key)
                    writeByte('\n'.code)
                } else {
                    writeUtf8(CLEAN).writeByte(' '.code)
                    writeUtf8(entry.key)
                    entry.writeLengths(this)
                    writeByte('\n'.code)
                }
            }
        }

        if (fileSystem.exists(journalFile)) {
            fileSystem.atomicMove(journalFile, journalFileBackup)
            fileSystem.atomicMove(journalFileTmp, journalFile)
            fileSystem.deleteIfExists(journalFileBackup)
        } else {
            fileSystem.atomicMove(journalFileTmp, journalFile)
        }

        journalWriter = newJournalWriter()
        hasJournalErrors = false
        mostRecentRebuildFailed = false
    }

    /**
     * Returns a snapshot of the entry named [key], or null if it doesn't exist is not currently
     * readable. If a value is returned, it is moved to the head of the LRU queue.
     */
    @Synchronized
    operator fun get(key: String): Snapshot? {
        initialize()

        checkNotClosed()
        validateKey(key)
        val entry = lruEntries[key] ?: return null
        val snapshot = entry.snapshot() ?: return null

        redundantOpCount++
        journalWriter!!.apply {
            writeUtf8(READ)
            writeByte(' '.code)
            writeUtf8(key)
            writeByte('\n'.code)
        }
        if (journalRebuildRequired()) {
            cleanupExecutor.submit(cleanupTask)
        }

        return snapshot
    }

    /** Returns an editor for the entry named [key], or null if another edit is in progress. */
    @Synchronized
    fun edit(key: String, expectedSequenceNumber: Long = ANY_SEQUENCE_NUMBER): Editor? {
        initialize()

        checkNotClosed()
        validateKey(key)
        var entry: Entry? = lruEntries[key]
        if (expectedSequenceNumber != ANY_SEQUENCE_NUMBER &&
            (entry == null || entry.sequenceNumber != expectedSequenceNumber)) {
            return null // Snapshot is stale.
        }

        if (entry?.currentEditor != null) {
            return null // Another edit is in progress.
        }

        if (entry != null && entry.lockingSourceCount != 0) {
            return null // We can't write this file because a reader is still reading it.
        }

        if (mostRecentTrimFailed || mostRecentRebuildFailed) {
            // The OS has become our enemy! If the trim job failed, it means we are storing more
            // data than requested by the user. Do not allow edits so we do not go over that limit
            // any further. If the journal rebuild failed, the journal writer will not be active,
            // meaning we will not be able to record the edit, causing file leaks. In both cases,
            // we want to retry the clean up so we can get out of this state!
            cleanupExecutor.submit(cleanupTask)
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

        // If this edit is creating the entry for the first time, every index must have a value.
        if (success && !entry.readable) {
            for (i in 0 until valueCount) {
                if (!editor.written!![i]) {
                    editor.abort()
                    throw IllegalStateException("Newly created entry didn't create value for index $i")
                }
                if (!fileSystem.exists(entry.dirtyFiles[i])) {
                    editor.abort()
                    return
                }
            }
        }

        for (i in 0 until valueCount) {
            val dirty = entry.dirtyFiles[i]
            if (success && !entry.zombie) {
                if (fileSystem.exists(dirty)) {
                    val clean = entry.cleanFiles[i]
                    fileSystem.atomicMove(dirty, clean)
                    val oldLength = entry.lengths[i]
                    val newLength = fileSystem.metadata(clean).size ?: 0
                    entry.lengths[i] = newLength
                    size = size - oldLength + newLength
                }
            } else {
                fileSystem.deleteIfExists(dirty)
            }
        }

        entry.currentEditor = null
        if (entry.zombie) {
            removeEntry(entry)
            return
        }

        redundantOpCount++
        journalWriter!!.apply {
            if (entry.readable || success) {
                entry.readable = true
                writeUtf8(CLEAN).writeByte(' '.code)
                writeUtf8(entry.key)
                entry.writeLengths(this)
                writeByte('\n'.code)
                if (success) {
                    entry.sequenceNumber = nextSequenceNumber++
                }
            } else {
                lruEntries.remove(entry.key)
                writeUtf8(REMOVE).writeByte(' '.code)
                writeUtf8(entry.key)
                writeByte('\n'.code)
            }
            flush()
        }

        if (size > maxSize || journalRebuildRequired()) {
            cleanupExecutor.submit(cleanupTask)
        }
    }

    /**
     * We only rebuild the journal when it will halve the size of the journal and eliminate at
     * least 2000 ops.
     */
    private fun journalRebuildRequired(): Boolean {
        return redundantOpCount >= 2000 && redundantOpCount >= lruEntries.size
    }

    /**
     * Drops the entry for [key] if it exists and can be removed. If the entry for [key] is
     * currently being edited, that edit will complete normally but its value will not be stored.
     *
     * @return true if an entry was removed.
     */
    @Synchronized
    fun remove(key: String): Boolean {
        initialize()

        checkNotClosed()
        validateKey(key)
        val entry = lruEntries[key] ?: return false
        val removed = removeEntry(entry)
        if (removed && size <= maxSize) mostRecentTrimFailed = false
        return removed
    }

    private fun removeEntry(entry: Entry): Boolean {
        // Prevent the edit from completing normally.
        entry.currentEditor?.detach()

        for (i in 0 until valueCount) {
            fileSystem.deleteIfExists(entry.cleanFiles[i])
            size -= entry.lengths[i]
            entry.lengths[i] = 0
        }

        redundantOpCount++
        journalWriter?.let {
            it.writeUtf8(REMOVE)
            it.writeByte(' '.code)
            it.writeUtf8(entry.key)
            it.writeByte('\n'.code)
        }
        lruEntries.remove(entry.key)

        if (journalRebuildRequired()) {
            cleanupExecutor.submit(cleanupTask)
        }

        return true
    }

    @Synchronized
    private fun checkNotClosed() {
        check(!closed) { "cache is closed" }
    }

    /** Force buffered operations to the filesystem. */
    @Synchronized
    override fun flush() {
        if (!initialized) return

        checkNotClosed()
        trimToSize()
        journalWriter!!.flush()
    }

    @Synchronized
    fun isClosed(): Boolean = closed

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
                entry.currentEditor?.detach() // Prevent the edit from completing normally.
            }
        }

        trimToSize()
        journalWriter!!.close()
        journalWriter = null
        closed = true
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
    fun delete() {
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

    private fun validateKey(key: String) {
        require(LEGAL_KEY_PATTERN.matches(key)) {
            "keys must match regex [a-z0-9_-]{1,120}: \"$key\""
        }
    }

    /**
     * Returns an iterator over the cache's current entries. This iterator doesn't throw
     * `ConcurrentModificationException`, but if new entries are added while iterating, those new
     * entries will not be returned by the iterator. If existing entries are removed during
     * iteration, they will be absent (unless they were already returned).
     *
     * If there are I/O problems during iteration, this iterator fails silently. For example, if
     * the hosting filesystem becomes unreachable, the iterator will omit elements rather than
     * throwing exceptions.
     *
     * **The caller must [Snapshot.close]** for each snapshot returned by [Iterator.next].
     * Failing to do so leaks open files!
     */
    @Synchronized
    fun snapshots(): MutableIterator<Snapshot> {
        initialize()
        return object : MutableIterator<Snapshot> {
            /** Iterate a copy of the entries to defend against concurrent modification errors. */
            private val delegate = lruEntries.values.toList().iterator()

            /** The snapshot to return from [next]. Null if we haven't computed that yet. */
            private var nextSnapshot: Snapshot? = null

            /** The snapshot to remove with [remove]. Null if removal is illegal. */
            private var removeSnapshot: Snapshot? = null

            override fun hasNext(): Boolean {
                if (nextSnapshot != null) return true

                synchronized(this@RealDiskCache) {
                    // If the cache is closed, truncate the iterator.
                    if (closed) return false

                    while (delegate.hasNext()) {
                        nextSnapshot = delegate.next().snapshot() ?: continue
                        return true
                    }
                }

                return false
            }

            override fun next(): Snapshot {
                if (!hasNext()) throw NoSuchElementException()
                removeSnapshot = nextSnapshot
                nextSnapshot = null
                return removeSnapshot!!
            }

            override fun remove() {
                val removeSnapshot = checkNotNull(removeSnapshot) { "remove() before next()" }
                try {
                    remove(removeSnapshot.key)
                } catch (_: IOException) {
                    // Nothing useful to do here. We failed to remove from the cache. Most likely
                    // that's because we couldn't update the journal, but the cached entry will
                    // still be gone.
                } finally {
                    this.removeSnapshot = null
                }
            }
        }
    }

    /** A snapshot of the values for an entry. */
    inner class Snapshot(
        val key: String,
        private val sequenceNumber: Long,
        private val sources: List<Source>,
        private val lengths: LongArray
    ) : Closeable {

        /**
         * Returns an editor for this snapshot's entry, or null if either the entry has changed
         * since this snapshot was created or if another edit is in progress.
         */
        fun edit(): Editor? = edit(key, sequenceNumber)

        /** Returns the unbuffered stream with the value for [index]. */
        fun getSource(index: Int): Source = sources[index]

        /** Returns the byte length of the value for [index]. */
        fun getLength(index: Int): Long = lengths[index]

        override fun close() {
            sources.forEachIndices { it.closeQuietly() }
        }
    }

    /** Edits the values for an entry. */
    inner class Editor(val entry: Entry) {

        private var done = false
        val written = if (entry.readable) null else BooleanArray(valueCount)

        /**
         * Prevents this editor from completing normally. This is necessary either when the edit
         * causes an I/O error, or if the target entry is evicted while this editor is active. In
         * either case we delete the editor's created files and prevent new files from being
         * created. Note that once an editor has been detached it is possible for another editor to
         * edit the entry.
         */
        fun detach() {
            if (entry.currentEditor == this) {
                completeEdit(this, false) // Delete it now.
            }
        }

        /**
         * Returns an unbuffered input stream to read the last committed value, or null if no value
         * has been committed.
         */
        fun newSource(index: Int): Source? {
            synchronized(this@RealDiskCache) {
                check(!done)
                if (!entry.readable || entry.currentEditor != this || entry.zombie) {
                    return null
                }
                return try {
                    fileSystem.source(entry.cleanFiles[index])
                } catch (_: FileNotFoundException) {
                    null
                }
            }
        }

        /**
         * Returns a new unbuffered output stream to write the value at [index]. If the underlying
         * output stream encounters errors when writing to the filesystem, this edit will be aborted
         * when [commit] is called. The returned output stream does not throw IOExceptions.
         */
        fun newSink(index: Int): Sink {
            synchronized(this@RealDiskCache) {
                check(!done)
                if (entry.currentEditor != this) {
                    return blackholeSink()
                }
                if (!entry.readable) {
                    written!![index] = true
                }
                val dirtyFile = entry.dirtyFiles[index]
                val sink = try {
                    fileSystem.sink(dirtyFile)
                } catch (_: FileNotFoundException) {
                    return blackholeSink()
                }
                return FaultHidingSink(sink) {
                    synchronized(this@RealDiskCache) {
                        detach()
                    }
                }
            }
        }

        /**
         * Commits this edit so it is visible to readers. This releases the edit lock so another
         * edit may be started on the same key.
         */
        fun commit() {
            synchronized(this@RealDiskCache) {
                check(!done)
                if (entry.currentEditor == this) {
                    completeEdit(this, true)
                }
                done = true
            }
        }

        /**
         * Aborts this edit. This releases the edit lock so another edit may be started on the
         * same key.
         */
        fun abort() {
            synchronized(this@RealDiskCache) {
                check(!done)
                if (entry.currentEditor == this) {
                    completeEdit(this, false)
                }
                done = true
            }
        }
    }

    inner class Entry(val key: String) {

        /** Lengths of this entry's files. */
        val lengths = LongArray(valueCount)
        val cleanFiles = mutableListOf<Path>()
        val dirtyFiles = mutableListOf<Path>()

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
         * Sources currently reading this entry before a write or delete can proceed. When
         * decrementing this to zero, the entry must be removed if it is a zombie.
         */
        var lockingSourceCount = 0

        /** The sequence number of the most recently committed edit to this entry. */
        var sequenceNumber = 0L

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

        /**
         * Returns a snapshot of this entry. This opens all streams eagerly to guarantee that we
         * see a single published snapshot. If we opened streams lazily then the streams could come
         * from different edits.
         */
        fun snapshot(): Snapshot? {
            if (!readable) return null

            val sources = mutableListOf<Source>()
            val lengths = lengths.clone() // Defensive copy since these can be zeroed out.
            try {
                for (i in 0 until valueCount) {
                    sources += newSource(i)
                }
                return Snapshot(key, sequenceNumber, sources, lengths)
            } catch (_: FileNotFoundException) {
                // A file must have been deleted manually!
                sources.forEachIndices { it.closeQuietly() }
                // Since the entry is no longer valid, remove it so the metadata is accurate
                // (i.e. the cache size.)
                try {
                    removeEntry(this)
                } catch (_: IOException) {}
                return null
            }
        }

        private fun newSource(index: Int): Source = fileSystem.source(cleanFiles[index])
    }

    companion object {
        private const val JOURNAL_FILE = "journal"
        private const val JOURNAL_FILE_TEMP = "journal.tmp"
        private const val JOURNAL_FILE_BACKUP = "journal.bkp"
        private const val MAGIC = "libcore.io.DiskLruCache"
        private const val VERSION = "1"
        private const val ANY_SEQUENCE_NUMBER = -1L
        private const val CLEAN = "CLEAN"
        private const val DIRTY = "DIRTY"
        private const val REMOVE = "REMOVE"
        private const val READ = "READ"
        private val LEGAL_KEY_PATTERN = "[a-z0-9_-]{1,120}".toRegex()
    }
}
