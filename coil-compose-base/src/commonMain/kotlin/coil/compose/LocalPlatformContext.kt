package coil.compose

import androidx.compose.runtime.ProvidableCompositionLocal
import coil.PlatformContext

expect val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext>
