// ktlint-disable filename
@file:JvmName("-Deprecated")
@file:Suppress("unused")

package coil

@Deprecated(
    message = "Replace with ImageLoader.Builder.",
    replaceWith = ReplaceWith("ImageLoader.Builder", "coil.ImageLoader")
)
typealias ImageLoaderBuilder = ImageLoader.Builder
