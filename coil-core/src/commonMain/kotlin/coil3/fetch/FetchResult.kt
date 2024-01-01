package coil3.fetch

import coil3.Image
import coil3.annotation.Data
import coil3.decode.DataSource
import coil3.decode.Decoder
import coil3.decode.ImageSource

/** The result of [Fetcher.fetch]. */
sealed interface FetchResult

/**
 * An [ImageSource] result, which will be consumed by a relevant [Decoder].
 *
 * @param source The [ImageSource] to read from.
 * @param mimeType An optional MIME type for the [source].
 * @param dataSource The source that [source] was fetched from.
 */
@Data
class SourceFetchResult(
    val source: ImageSource,
    val mimeType: String?,
    val dataSource: DataSource,
) : FetchResult

/**
 * An [Image] result. Return this from a [Fetcher] if its data cannot
 * be converted into an [ImageSource].
 *
 * @param image The fetched [Image].
 * @param isSampled 'true' if [image] is sampled (i.e. loaded into memory
 *  at less than its original size).
 * @param dataSource The source that [image] was fetched from.
 */
@Data
class ImageFetchResult(
    val image: Image,
    val isSampled: Boolean,
    val dataSource: DataSource,
) : FetchResult
