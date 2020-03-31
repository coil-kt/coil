package coil.util

import android.content.ComponentCallbacks2
import android.content.res.Configuration

internal interface ComponentCallbacks : ComponentCallbacks2 {

    override fun onConfigurationChanged(newConfig: Configuration) {}

    override fun onLowMemory() = onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)

    override fun onTrimMemory(level: Int) {}
}
