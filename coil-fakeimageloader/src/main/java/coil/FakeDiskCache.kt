@file:JvmName("FakeDiskCaches")

package coil

import coil.FakeDiskCache.State
import coil.annotation.ExperimentalCoilApi
import coil.disk.DiskCache
import coil.disk.DiskCache.Editor
import coil.disk.DiskCache.Snapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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

    private val states = LinkedHashMap<String, State>()

    private val _gets = MutableSharedFlow<String>()
    val gets: Flow<String> get() = _gets

    private val _edits = MutableSharedFlow<String>()
    val edits: Flow<String> get() = _edits

    private val _removes = MutableSharedFlow<String>()
    val removes: Flow<String> get() = _removes

    override var size = 0L

    override val fileSystem = object : ForwardingFileSystem(fileSystem) {
        override fun sink(file: Path, mustCreate: Boolean): Sink {
            // Ensure the parent directory exists.
            file.parent?.let(::createDirectories)
            return super.sink(file, mustCreate)
        }
    }

    val keys: Set<String> get() = states.keys.toImmutableSet()

    val snapshot: Map<String, State> get() = states.toImmutableMap()

    @Synchronized
    override fun get(key: String): Snapshot? {
        _gets.tryEmit(key)
        return when (val state = states[key]) {
            null -> FakeSnapshot(key).also { snapshot ->
                states[key] = State.Read(setOf(snapshot))
            }
            is State.Read -> FakeSnapshot(key).also { snapshot ->
                states[key] = state.copy(snapshots = state.snapshots + snapshot)
            }
            is State.Write -> null
        }
    }

    @Synchronized
    override fun edit(key: String): Editor? {
        _edits.tryEmit(key)
        return when (states[key]) {
            null -> FakeEditor(key).also { editor ->
                states[key] = State.Write(editor)
            }
            else -> null
        }
    }

    @Synchronized
    override fun remove(key: String): Boolean {
        _removes.tryEmit(key)
        return remove(metadata(key)) || remove(data(key))
    }

    private fun remove(path: Path): Boolean {
        val exists = fileSystem.exists(path)
        if (exists) {
            val size = fileSystem.metadata(path).size
            fileSystem.delete(path)
            size?.let { this.size -= it }
        }
        return exists
    }

    @Synchronized
    override fun clear() {
        fileSystem.deleteRecursively(directory)
    }

    @Synchronized
    private fun close(snapshot: FakeSnapshot) {
        val state = states[snapshot.key]
        check(state is State.Read && snapshot in state.snapshots) {
            "unexpected state: ${snapshot.key} = $state"
        }
        val snapshots = state.snapshots - snapshot
        if (snapshots.isEmpty()) {
            states -= snapshot.key
        } else {
            states[snapshot.key] = state.copy(snapshots = snapshots)
        }
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

    private fun complete(editor: FakeEditor) {
        val state = states[editor.key]
        check(state is State.Write && state.editor === editor) {
            "unexpected state: ${editor.key} = $state"
        }
        states -= editor.key
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

    override fun toString(): String {
        return "FakeDiskCache(states=$states)"
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
        data class Read(val snapshots: Set<Snapshot>): State
        data class Write(val editor: Editor): State
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
    val size = size
    if (size != 0L) {
        throw AssertionError("The disk cache is not empty: $this")
    }
}
