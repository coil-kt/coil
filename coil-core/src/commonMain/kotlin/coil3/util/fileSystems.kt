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
package coil3.util

import kotlin.random.Random
import kotlin.random.nextULong
import okio.FileNotFoundException
import okio.FileSystem
import okio.IOException
import okio.Path

internal expect fun defaultFileSystem(): FileSystem

internal expect fun FileSystem.remainingFreeSpaceBytes(directory: Path): Long

/** Create a new empty file. */
internal fun FileSystem.createFile(file: Path, mustCreate: Boolean = false) {
    if (mustCreate) {
        sink(file, mustCreate = true).closeQuietly()
    } else if (!exists(file)) {
        sink(file).closeQuietly()
    }
}

/** https://github.com/square/okio/issues/1090 */
internal fun FileSystem.createTempFile(): Path {
    var tempFile: Path
    do {
        tempFile = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "tmp_${Random.nextULong()}"
    } while (exists(tempFile))
    createFile(tempFile, mustCreate = true)
    return tempFile
}

/** Tolerant delete, try to clear as many files as possible even after a failure. */
internal fun FileSystem.deleteContents(directory: Path) {
    var exception: IOException? = null
    val files = try {
        list(directory)
    } catch (_: FileNotFoundException) {
        return
    }
    for (file in files) {
        try {
            if (metadata(file).isDirectory) {
                deleteContents(file)
            }
            delete(file)
        } catch (e: IOException) {
            if (exception == null) {
                exception = e
            }
        }
    }
    if (exception != null) {
        throw exception
    }
}

/**
 * Returns the extension of this file (not including the dot), or an empty string
 * if it doesn't have one.
 */
internal val Path.extension: String
    get() = name.substringAfterLast('.', "")
