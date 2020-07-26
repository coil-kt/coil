@file:JvmName("-Deprecated")
@file:Suppress("unused")

package coil.request

@Deprecated(
    message = "Replace with ImageRequest.",
    replaceWith = ReplaceWith("ImageRequest", "coil.request.ImageRequest")
)
typealias LoadRequest = ImageRequest

@Deprecated(
    message = "Replace with ImageRequest.Builder.",
    replaceWith = ReplaceWith("ImageRequest.Builder", "coil.request.ImageRequest")
)
typealias LoadRequestBuilder = ImageRequest.Builder

@Deprecated(
    message = "Replace with ImageRequest.",
    replaceWith = ReplaceWith("ImageRequest", "coil.request.ImageRequest")
)
typealias GetRequest = ImageRequest

@Deprecated(
    message = "Replace with ImageRequest.Builder.",
    replaceWith = ReplaceWith("ImageRequest.Builder", "coil.request.ImageRequest")
)
typealias GetRequestBuilder = ImageRequest.Builder

@Deprecated(
    message = "Replace with ImageRequest.",
    replaceWith = ReplaceWith("ImageRequest", "coil.request.ImageRequest")
)
typealias Request = ImageRequest

@Deprecated(
    message = "Replace with Disposable.",
    replaceWith = ReplaceWith("Disposable", "coil.request.Disposable")
)
typealias RequestDisposable = Disposable

@Deprecated(
    message = "Replace with ImageResult.",
    replaceWith = ReplaceWith("ImageResult", "coil.request.ImageResult")
)
typealias RequestResult = ImageResult
