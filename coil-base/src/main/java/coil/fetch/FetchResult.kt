package coil.fetch

import android.graphics.drawable.Drawable
import coil.decode.DataSource
import coil.decode.Decoder
import okio.BufferedSource

/** The result of [Fetcher.fetch]. */
public sealed class FetchResult

/**
 * A raw [BufferedSource] result, which will be consumed by the relevant [Decoder].
 *
 * @param source An unconsumed [BufferedSource] that will be decoded by a [Decoder].
 * @param mimeType An optional MIME type for the [source].
 * @param dataSource Where [source] was loaded from.
 */
public data class SourceResult(
    val source: BufferedSource,
    val mimeType: String?,
    val dataSource: DataSource
) : FetchResult()

/**
 * A direct [Drawable] result. Return this from a [Fetcher] if its data cannot be converted into a [BufferedSource].
 *
 * @param drawable The loaded [Drawable].
 * @param isSampled True if [drawable] is sampled (i.e. not loaded into memory at full size).
 * @param dataSource The source that [drawable] was fetched from.
 */
public data class DrawableResult(
    val drawable: Drawable,
    val isSampled: Boolean,
    val dataSource: DataSource
) : FetchResult()
