@file:JvmName("FakeDiskCaches")

package coil.disk

import coil.annotation.ExperimentalCoilApi
import coil.disk.DiskCache.Editor
import coil.disk.DiskCache.Snapshot
import coil.disk.FakeDiskCache.State
import coil.disk.FakeDiskCache.Value
import coil.toImmutableMap
import coil.toImmutableSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.fakefilesystem.FakeFileSystem

@ExperimentalCoilApi
class FakeDiskCache private constructor(
    override val maxSize: Long,
    override val directory: Path,
    override val fileSystem: FileSystem,
) : DiskCache {

    private val cache = LinkedHashMap<String, Value>(0, 0.75f, true)

    // Don't expose this file system in the public API.
    private val internalFileSystem = object : ForwardingFileSystem(fileSystem) {
        override fun sink(file: Path, mustCreate: Boolean): Sink {
            // Ensure the parent directory exists.
            file.parent?.let(::createDirectories)
            return super.sink(file, mustCreate)
        }
    }

    private val _gets = MutableSharedFlow<String>()
    private val _edits = MutableSharedFlow<String>()
    private val _removes = MutableSharedFlow<String>()
    private val _evicts = MutableSharedFlow<String>()

    /** Returns a [Flow] that emits when [get] is called. */
    val gets: Flow<String> = _gets.asSharedFlow()

    /** Returns a [Flow] that emits when [edit] is called. */
    val edits: Flow<String> = _edits.asSharedFlow()

    /** Returns a [Flow] that emits when [remove] is called. */
    val removes: Flow<String> = _removes.asSharedFlow()

    /** Returns a [Flow] that emits when an entry is evicted due to the cache exceeding [maxSize]. */
    val evicts: Flow<String> = _evicts.asSharedFlow()

    /** Returns an immutable snapshot of the keys in this cache. */
    @get:Synchronized
    val keys: Set<String> get() = cache.keys.toImmutableSet()

    /** Returns an immutable snapshot of the values in this cache. */
    @get:Synchronized
    val values: Set<Value> get() = cache.values.toImmutableSet()

    /** Returns an immutable snapshot of the entries in this cache. */
    @get:Synchronized
    val entries: Map<String, Value> get() = cache.toImmutableMap()

    @get:Synchronized
    override var size = 0L

    @Synchronized
    override fun get(key: String): Snapshot? {
        val value = cache[key] ?: return null
        return when (val state = value.state) {
            is State.Read -> FakeSnapshot(key).also { snapshot ->
                cache[key] = value.copy(state = state.copy(snapshots = state.snapshots + snapshot))
            }
            is State.Write -> null
        }.also { _gets.tryEmit(key) }
    }

    @Synchronized
    override fun edit(key: String): Editor? {
        val value = cache[key] ?: DEFAULT_VALUE
        val state = value.state
        return when {
            state is State.Read && state.snapshots.isEmpty() -> FakeEditor(key).also { editor ->
                cache[key] = value.copy(state = State.Write(editor))
            }
            else -> null
        }.also { _edits.tryEmit(key) }
    }

    @Synchronized
    override fun remove(key: String) = delete(key, DELETE_TYPE_REMOVE)

    private fun delete(key: String, type: Int): Boolean {
        val value = cache[key] ?: return false
        val state = value.state
        if (state is State.Read && state.snapshots.isEmpty()) {
            deleteEntry(key, value)
        } else {
            cache[key] = value.copy(pendingDeletion = true)
        }
        when (type) {
            DELETE_TYPE_REMOVE -> _removes.tryEmit(key)
            DELETE_TYPE_EVICT -> _evicts.tryEmit(key)
        }
        return true
    }

    @Synchronized
    override fun clear() {
        cache.keys.forEach { delete(it, DELETE_TYPE_CLEAR) }
    }

    @Synchronized
    private fun close(snapshot: FakeSnapshot) {
        val value = cache[snapshot.key]
        val state = value?.state
        check(state is State.Read && snapshot in state.snapshots) {
            "unexpected state (key=${snapshot.key}): $state"
        }

        val newSnapshots = state.snapshots - snapshot
        if (value.pendingDeletion && newSnapshots.isEmpty()) {
            deleteEntry(snapshot.key, value)
        } else {
            cache[snapshot.key] = value.copy(state = state.copy(snapshots = newSnapshots))
        }
    }

    @Synchronized
    private fun closeAndEdit(snapshot: FakeSnapshot): Editor? {
        close(snapshot)
        return edit(snapshot.key)
    }

    @Synchronized
    private fun commit(editor: FakeEditor) = completeEdit(editor, success = true)

    @Synchronized
    private fun commitAndGet(editor: FakeEditor): Snapshot? {
        commit(editor)
        return get(editor.key)
    }

    @Synchronized
    private fun abort(editor: FakeEditor) = completeEdit(editor, success = false)

    private fun completeEdit(editor: FakeEditor, success: Boolean) {
        val value = cache[editor.key]
        val state = value?.state
        check(state is State.Write && state.editor === editor) {
            "unexpected state (key=${editor.key}): $state"
        }

        when {
            value.pendingDeletion -> { // Deleted
                internalFileSystem.delete(editor.metadata)
                internalFileSystem.delete(editor.data)
                deleteEntry(editor.key, value)
            }
            success -> { // Committed
                var newSize = 0L
                if (internalFileSystem.exists(editor.metadata)) {
                    newSize = internalFileSystem.metadata(editor.metadata).size ?: 0
                    internalFileSystem.atomicMove(editor.metadata, metadata(editor.key))
                }
                if (internalFileSystem.exists(editor.data)) {
                    newSize += internalFileSystem.metadata(editor.data).size ?: 0
                    internalFileSystem.atomicMove(editor.data, data(editor.key))
                }
                cache[editor.key] = value.copy(state = State.Read(setOf()), size = newSize)
                size += newSize

                // Trim the entries until we're below the maxSize or all entries are pending eviction.
                while (size > maxSize) {
                    if (!deleteOldestEntry()) break
                }
            }
            else -> { // Aborted
                internalFileSystem.delete(editor.metadata)
                internalFileSystem.delete(editor.data)

                if (value.size == -1L) {
                    // Delete this entry as the initial edit was aborted.
                    cache.remove(editor.key)
                } else {
                    cache[editor.key] = value.copy(state = State.Read(setOf()))
                }
            }
        }
    }

    private fun deleteOldestEntry(): Boolean {
        for ((key, value) in cache) {
            if (!value.pendingDeletion) {
                delete(key, DELETE_TYPE_EVICT)
                return true
            }
        }
        return false
    }

    /** Deletes the entry completely from the file system and cache. */
    private fun deleteEntry(key: String, value: Value) {
        internalFileSystem.delete(metadata(key))
        internalFileSystem.delete(data(key))
        cache.remove(key)
        size -= value.size
    }

    private fun metadata(key: String, tmp: Boolean = false): Path {
        return directory.resolve("$key.$ENTRY_METADATA" + (if (tmp) ".tmp" else ""))
    }

    private fun data(key: String, tmp: Boolean = false): Path {
        return directory.resolve("$key.$ENTRY_DATA" + (if (tmp) ".tmp" else ""))
    }

    @Synchronized
    override fun toString(): String {
        return "FakeDiskCache(entries=$cache)"
    }

    private inner class FakeSnapshot(val key: String) : Snapshot {
        override val metadata = metadata(key)
        override val data = data(key)
        override fun close() = close(this)
        override fun closeAndEdit() = closeAndEdit(this)
    }

    private inner class FakeEditor(val key: String) : Editor {
        override val metadata = metadata(key, tmp = true)
        override val data = data(key, tmp = true)
        override fun commit() = commit(this)
        override fun commitAndGet() = commitAndGet(this)
        override fun abort() = abort(this)
    }

    sealed interface State {
        data class Read(val snapshots: Set<Snapshot>) : State
        data class Write(val editor: Editor) : State
    }

    data class Value(
        /**
         * The current read/write state of this entry.
         */
        val state: State,

        /**
         * The size of the entry on disk in bytes or -1 if the first edit has not completed yet.
         */
        val size: Long,

        /**
         * True if this entry has been removed/evicted, but there's an active snapshot or editor
         * preventing its deletion from the file system.
         */
        val pendingDeletion: Boolean,
    )

    class Builder {

        private var maxSize: Long = Long.MAX_VALUE
        private var directory: Path = "/$DEFAULT_DIRECTORY".toPath()
        private var fileSystem: FileSystem = FakeFileSystem()

        fun maxSize(size: Long) = apply {
            this.maxSize = size
        }

        fun directory(directory: Path) = apply {
            this.directory = directory
        }

        fun fileSystem(fileSystem: FileSystem) = apply {
            this.fileSystem = fileSystem
        }

        fun build() = FakeDiskCache(
            maxSize = maxSize,
            directory = directory,
            fileSystem = fileSystem,
        )
    }

    private companion object {
        private const val DEFAULT_DIRECTORY = "image_cache"
        private const val ENTRY_METADATA = 0
        private const val ENTRY_DATA = 1
        private const val DELETE_TYPE_REMOVE = 0
        private const val DELETE_TYPE_EVICT = 1
        private const val DELETE_TYPE_CLEAR = 2
        private val DEFAULT_VALUE = Value(State.Read(setOf()), -1L, false)
    }
}

/**
 * Create a new [FakeDiskCache] without configuration.
 */
@JvmName("create")
fun FakeDiskCache(): FakeDiskCache {
    return FakeDiskCache.Builder().build()
}

/**
 * Assert the [FakeDiskCache] contains an entry that matches [predicate].
 */
fun FakeDiskCache.assertContains(predicate: (key: String, value: Value) -> Boolean) {
    entries.forEach { (key, value) ->
        if (predicate(key, value)) return
    }
    throw AssertionError("No entries matched the predicate: $this")
}

/**
 * Assert the [FakeDiskCache] does not contain any entries.
 */
fun FakeDiskCache.assertEmpty() {
    if (size > 0L) {
        throw AssertionError("The disk cache is not empty: $this")
    }
}

/**
 * Assert the [FakeDiskCache] does not have any open reads or writes.
 */
fun FakeDiskCache.assertNotReadingOrWriting() {
    val readingOrWriting = values.any { value ->
        val state = value.state
        state !is State.Read || state.snapshots.isNotEmpty()
    }
    if (readingOrWriting) {
        throw AssertionError("The disk cache has open reads and/or writes: $this")
    }
}
