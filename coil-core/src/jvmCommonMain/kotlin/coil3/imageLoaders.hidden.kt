@file:JvmName("ImageLoaders_nonJsCommonKt")

package coil3

import coil3.request.ImageRequest

@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
fun ImageLoader.executeBlocking(request: ImageRequest) = executeBlocking(request)
