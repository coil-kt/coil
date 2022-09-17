@file:JvmName("FakeDiskCaches")

package coil.disk

import coil.annotation.ExperimentalCoilApi
import coil.disk.DiskCache.Editor
import coil.disk.DiskCache.Snapshot
import coil.disk.FakeDiskCache.State
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
    fileSystem: FileSystem,
) : DiskCache {

    private val entries = LinkedHashMap<String, State>(0, 0.75f, true)

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

    override var size = 0L

    override val fileSystem = object : ForwardingFileSystem(fileSystem) {
        override fun sink(file: Path, mustCreate: Boolean): Sink {
            // Ensure the parent directory exists.
            file.parent?.let(::createDirectories)
            return super.sink(file, mustCreate)
        }
    }

    val keys: Set<String> get() = entries.keys.toImmutableSet()

    val snapshot: Map<String, State> get() = entries.toImmutableMap()

    @Synchronized
    override fun get(key: String): Snapshot? {
        val state = entries.getOrElse(key) { State.Readable(emptySet()) }
        return when (state) {
            is State.Readable -> FakeSnapshot(key).also { snapshot ->
                entries[key] = state.copy(snapshots = state.snapshots + snapshot)
            }
            is State.Writing -> null
        }.also { _gets.tryEmit(key) }
    }

    @Synchronized
    override fun edit(key: String): Editor? {
        val state = entries.getOrElse(key) { State.Readable(emptySet()) }
        return if (state is State.Readable && state.snapshots.isEmpty()) {
            FakeEditor(key).also { editor ->
                entries[key] = State.Writing(editor)
            }
        } else {
            null
        }.also { _edits.tryEmit(key) }
    }

    @Synchronized
    override fun remove(key: String) = delete(key, eviction = false)

    private fun delete(key: String, eviction: Boolean): Boolean {
        val isRemoved = delete(metadata(key)) || delete(data(key))
        if (eviction) {
            _evicts.tryEmit(key)
        } else {
            _removes.tryEmit(key)
        }
        return isRemoved
    }

    private fun delete(path: Path): Boolean {
        val exists = fileSystem.exists(path)
        if (exists) {
            val fileSize = fileSystem.metadata(path).size
            fileSystem.delete(path)
            fileSize?.let { size -= it }
        }
        return exists
    }

    @Synchronized
    override fun clear() {
        fileSystem.deleteRecursively(directory)
    }

    @Synchronized
    private fun close(snapshot: FakeSnapshot) {
        val state = entries[snapshot.key]
        check(state is State.Readable && snapshot in state.snapshots) {
            "unexpected state: $state"
        }
        entries[snapshot.key] = state.copy(snapshots = state.snapshots - snapshot)
    }

    @Synchronized
    private fun closeAndEdit(snapshot: FakeSnapshot): Editor? {
        close(snapshot)
        return edit(snapshot.key)
    }

    @Synchronized
    private fun commit(editor: FakeEditor) {
        val metadata = metadata(editor.key)
        val metadataSize = fileSystem.metadata(metadata).size
        fileSystem.atomicMove(editor.metadata, metadata)
        metadataSize?.let { size += it }

        val data = data(editor.key)
        val dataSize = fileSystem.metadata(data).size
        fileSystem.atomicMove(editor.data, data)
        dataSize?.let { size += it }

        // Trim the entries until we're below the maxSize or all entries are pending eviction.
        while (size > maxSize) {
            if (!removeOldestEntry()) break
        }

        complete(editor)
    }

    @Synchronized
    private fun commitAndGet(editor: FakeEditor): Snapshot? {
        commit(editor)
        return get(editor.key)
    }

    @Synchronized
    private fun abort(editor: FakeEditor) {
        fileSystem.delete(editor.metadata)
        fileSystem.delete(editor.data)
        complete(editor)
    }

    private fun removeOldestEntry(): Boolean {
        for ((key, value) in entries) {
            if (!value.pendingEviction) {
                delete(key, eviction = true)
                return true
            }
        }
        return false
    }

    private fun complete(editor: FakeEditor) {
        val state = entries[editor.key]
        check(state is State.Writing && state.editor === editor) {
            "unexpected state: $state"
        }
        entries[editor.key] = State.Readable(emptySet())
    }

    private fun metadata(key: String, tmp: Boolean = false): Path {
        return if (tmp) {
            directory.resolve("$key.$ENTRY_METADATA.tmp")
        } else {
            directory.resolve("$key.$ENTRY_METADATA")
        }
    }

    private fun data(key: String, tmp: Boolean = false): Path {
        return if (tmp) {
            directory.resolve("$key.$ENTRY_DATA.tmp")
        } else {
            directory.resolve("$key.$ENTRY_DATA")
        }
    }

    @Synchronized
    override fun toString(): String {
        return "FakeDiskCache(entries=$entries)"
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

        /**
         * True if this entry has been evicted, but there's an active snapshot or editor
         * preventing its deletion from the file system.
         */
        val pendingEviction: Boolean

        data class Readable(
            val snapshots: Set<Snapshot>,
            override val pendingEviction: Boolean = false
        ) : State

        data class Writing(
            val editor: Editor,
            override val pendingEviction: Boolean = false
        ) : State
    }

    class Builder {

        private var maxSize: Long = 0L
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
fun FakeDiskCache.assertContains(predicate: (key: String, state: State) -> Boolean) {
    snapshot.entries.forEach { (key, state) ->
        if (predicate(key, state)) return
    }
    throw AssertionError("No entries matched the predicate: $this")
}

/**
 * Assert the [FakeDiskCache] does not contain any entries.
 */
fun FakeDiskCache.assertEmpty() {
    if (size != 0L) {
        throw AssertionError("The disk cache is not empty: $this")
    }
}

/**
 * Assert the [FakeDiskCache] does not have any open reads or writes.
 */
fun FakeDiskCache.assertNotReadingOrWriting() {
    if (snapshot.values.any { it !is State.Readable || it.snapshots.isNotEmpty() }) {
        throw AssertionError("The disk cache has open reads and/or writes: $this")
    }
}
