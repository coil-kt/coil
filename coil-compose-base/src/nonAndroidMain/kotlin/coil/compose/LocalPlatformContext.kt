package coil.compose

import androidx.compose.runtime.staticCompositionLocalOf
import coil.PlatformContext

actual val LocalPlatformContext = staticCompositionLocalOf { PlatformContext.INSTANCE }
