// ktlint-disable filename
@file:JvmName("-Deprecated")
@file:Suppress("PackageDirectoryMismatch", "unused")

package coil.bitmappool

import coil.bitmap.BitmapPool

@Deprecated(
    message = "BitmapPool moved to a different package.",
    replaceWith = ReplaceWith("BitmapPool", "coil.bitmap.BitmapPool")
)
typealias BitmapPool = BitmapPool
