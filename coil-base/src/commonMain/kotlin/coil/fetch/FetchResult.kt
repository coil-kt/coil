package coil.fetch

import coil.Image
import coil.decode.DataSource
import coil.decode.Decoder
import coil.decode.ImageSource

/** The result of [Fetcher.fetch]. */
sealed class FetchResult

/**
 * An [ImageSource] result, which will be consumed by a relevant [Decoder].
 *
 * @param source The [ImageSource] to read from.
 * @param mimeType An optional MIME type for the [source].
 * @param dataSource The source that [source] was fetched from.
 */
data class SourceFetchResult(
    val source: ImageSource,
    val mimeType: String?,
    val dataSource: DataSource,
) : FetchResult()

/**
 * An [Image] result. Return this from a [Fetcher] if its data cannot
 * be converted into an [ImageSource].
 *
 * @param image The fetched [Image].
 * @param isSampled 'true' if [image] is sampled (i.e. loaded into memory
 *  at less than its original size).
 * @param dataSource The source that [image] was fetched from.
 */
data class ImageFetchResult(
    val image: Image,
    val isSampled: Boolean,
    val dataSource: DataSource,
) : FetchResult()
