package coil.fetch

import android.graphics.drawable.Drawable
import coil.decode.DataSource
import coil.decode.Decoder
import coil.decode.ImageSource
import okio.BufferedSource

/** The result of [Fetcher.fetch]. */
sealed class FetchResult

/**
 * An [ImageSource] result, which will be consumed by the relevant [Decoder].
 *
 * @param source The [ImageSource] to read from.
 * @param mimeType An optional MIME type for the [source].
 * @param dataSource The source that [source] was fetched from.
 */
data class SourceResult(
    val source: ImageSource,
    val mimeType: String?,
    val dataSource: DataSource,
) : FetchResult()

/**
 * A direct [Drawable] result. Return this from a [Fetcher] if its data cannot
 * be converted into a [BufferedSource].
 *
 * @param drawable The fetched [Drawable].
 * @param isSampled 'true' if [drawable] is sampled (i.e. loaded into memory
 *  at less than its original size).
 * @param dataSource The source that [drawable] was fetched from.
 */
data class DrawableResult(
    val drawable: Drawable,
    val isSampled: Boolean,
    val dataSource: DataSource,
) : FetchResult()
