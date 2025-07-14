package sample.common

import coil3.Extras
import coil3.video.videoFrameMicros

actual fun assetTypes(): List<AssetType> {
    return AssetType.entries
}

actual val Extras.Key.Companion.videoFrameMicros: Extras.Key<Long>
    get() = videoFrameMicros
