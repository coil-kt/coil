package coil3.decode

import coil3.request.Options
import okio.FileSystem
import org.jetbrains.skia.Data
import org.jetbrains.skia.makeFromFileName

internal actual suspend fun decodeBitmap(
    options: Options,
    bytes: ByteArray,
): DecodeResult = decodeBitmapSync(options, bytes)

internal actual fun tryMakeData(source: ImageSource): Data? {
    if (source is FileImageSource && source.fileSystem === FileSystem.SYSTEM) {
        return Data.makeFromFileName("${source.file}")
    }
    return null
}
