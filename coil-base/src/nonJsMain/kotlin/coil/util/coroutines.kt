package coil.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

internal actual fun ioCoroutineDispatcher() = Dispatchers.IO
