package coil3.network.httpengine.internal

import coil3.network.NetworkRequestBody
import okio.Buffer

internal suspend fun NetworkRequestBody.readBuffer(): Buffer = Buffer().also { writeTo(it) }
