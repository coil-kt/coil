package coil.util

import kotlin.js.Date

internal actual fun getTimeMillis() = Date.now().toLong()
