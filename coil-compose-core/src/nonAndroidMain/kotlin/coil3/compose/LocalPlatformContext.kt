package coil3.compose

import androidx.compose.runtime.staticCompositionLocalOf
import coil3.PlatformContext

actual val LocalPlatformContext = staticCompositionLocalOf { PlatformContext.INSTANCE }
