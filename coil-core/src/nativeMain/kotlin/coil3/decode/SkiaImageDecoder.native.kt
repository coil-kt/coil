package coil3.decode

import coil3.request.Options

internal actual suspend fun decodeBitmap(
    options: Options,
    bytes: ByteArray,
): DecodeResult = decodeBitmapSync(options, bytes)
