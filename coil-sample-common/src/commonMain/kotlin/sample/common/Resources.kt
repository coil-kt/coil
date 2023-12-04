// https://youtrack.jetbrains.com/issue/KTIJ-7642
@file:Suppress("FUN_INTERFACE_WITH_SUSPEND_FUNCTION")

package sample.common

import okio.Source

fun interface Resources {
    suspend fun open(path: String): Source
}
