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

import okio.Buffer
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.ForwardingSink
import okio.Path
import okio.Sink
import java.io.IOException

class FaultyFileSystem(delegate: FileSystem) : ForwardingFileSystem(delegate) {

    private val writeFaults = mutableSetOf<Path>()
    private val deleteFaults = mutableSetOf<Path>()
    private val renameFaults = mutableSetOf<Path>()

    fun setFaultyWrite(file: Path, faulty: Boolean) {
        if (faulty) {
            writeFaults += file
        } else {
            writeFaults -= file
        }
    }

    fun setFaultyDelete(file: Path, faulty: Boolean) {
        if (faulty) {
            deleteFaults += file
        } else {
            deleteFaults -= file
        }
    }

    fun setFaultyRename(file: Path, faulty: Boolean) {
        if (faulty) {
            renameFaults += file
        } else {
            renameFaults -= file
        }
    }

    override fun atomicMove(source: Path, target: Path) {
        if (source in renameFaults || target in renameFaults) throw IOException("boom!")
        super.atomicMove(source, target)
    }

    override fun delete(path: Path, mustExist: Boolean) {
        if (path in deleteFaults) throw IOException("boom!")
        super.delete(path, mustExist)
    }

    override fun deleteRecursively(fileOrDirectory: Path, mustExist: Boolean) {
        if (fileOrDirectory in deleteFaults) throw IOException("boom!")
        super.deleteRecursively(fileOrDirectory, mustExist)
    }

    override fun appendingSink(file: Path, mustExist: Boolean): Sink =
        FaultySink(super.appendingSink(file, mustExist), file)

    override fun sink(file: Path, mustCreate: Boolean): Sink =
        FaultySink(super.sink(file, mustCreate), file)

    inner class FaultySink(sink: Sink, private val file: Path) : ForwardingSink(sink) {
        override fun write(source: Buffer, byteCount: Long) {
            if (file in writeFaults) throw IOException("boom!")
            super.write(source, byteCount)
        }
    }
}
