package coil3.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

internal actual fun ioCoroutineDispatcher() = Dispatchers.IO
