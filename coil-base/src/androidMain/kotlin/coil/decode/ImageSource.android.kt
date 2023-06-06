package coil.decode

import android.net.Uri
import androidx.annotation.DrawableRes
import coil.annotation.ExperimentalCoilApi

/**
 * Metadata containing the [filePath] of an Android asset.
 */
@ExperimentalCoilApi
class AssetMetadata(
    val filePath: String,
) : ImageSource.Metadata()

/**
 * Metadata containing the [uri] of a `content` URI.
 */
@ExperimentalCoilApi
class ContentMetadata(
    val uri: Uri,
) : ImageSource.Metadata()

/**
 * Metadata containing the [packageName], [resId], and [density] of an Android resource.
 */
@ExperimentalCoilApi
class ResourceMetadata(
    val packageName: String,
    @DrawableRes val resId: Int,
    val density: Int,
) : ImageSource.Metadata()
