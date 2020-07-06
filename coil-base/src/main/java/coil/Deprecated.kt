@file:JvmName("-Deprecated")
@file:Suppress("unused")

package coil

import coil.request.DefaultRequestOptions

@Deprecated(
    message = "Replace with ImageLoader.Builder.",
    replaceWith = ReplaceWith("ImageLoader.Builder", "coil.ImageLoader")
)
typealias ImageLoaderBuilder = ImageLoader.Builder

@Deprecated(
    message = "DefaultRequestOptions moved to a different package.",
    replaceWith = ReplaceWith("DefaultRequestOptions", "coil.request.DefaultRequestOptions")
)
typealias DefaultRequestOptions = DefaultRequestOptions
