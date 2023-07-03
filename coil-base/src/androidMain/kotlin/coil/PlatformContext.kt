package coil

import android.content.Context
import coil.memory.totalAvailableMemoryBytes

internal class AndroidPlatformContext(
    val context: Context,
) : PlatformContext {

    override val application: PlatformContext
        get() = context.applicationContext.asPlatformContext()

    override val totalAvailableMemory: Long
        get() = totalAvailableMemoryBytes()
}

fun PlatformContext.asAndroidContext(): Context = (this as AndroidPlatformContext).context

fun Context.asPlatformContext(): PlatformContext = AndroidPlatformContext(this)
