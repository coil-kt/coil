package sample.common

import android.util.TypedValue
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import coil3.Extras
import coil3.video.videoFrameMicros

actual fun assetTypes(): List<AssetType> {
    return AssetType.entries
}

actual val Extras.Key.Companion.videoFrameMicros: Extras.Key<Long>
    get() = videoFrameMicros

fun ComponentActivity.enableEdgeToEdge() {
    val typedValue = TypedValue()
    theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(
            lightScrim = typedValue.data,
            darkScrim = typedValue.data,
        ),
    )
}
