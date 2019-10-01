package coil.memory

import android.graphics.Bitmap
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.os.Build.VERSION_CODES.O_MR1
import android.util.Log
import androidx.annotation.WorkerThread
import coil.memory.HardwareBitmapBlacklist.IS_BLACKLISTED
import coil.size.PixelSize
import coil.size.Size
import coil.util.log
import java.io.File

/** Decides if a request can use [Bitmap.Config.HARDWARE]. */
internal sealed class HardwareBitmapService {

    companion object {
        operator fun invoke() = when {
            SDK_INT < O -> ImmutableHardwareBitmapService(true)
            IS_BLACKLISTED -> ImmutableHardwareBitmapService(false)
            SDK_INT == O || SDK_INT == O_MR1 -> LimitedFileDescriptorHardwareBitmapService
            else -> ImmutableHardwareBitmapService(true)
        }
    }

    /** Return true if we can currently use [Bitmap.Config.HARDWARE]. */
    abstract fun allowHardware(size: Size): Boolean
}

/** Returns a fixed value for [allowHardware]. */
private class ImmutableHardwareBitmapService(private val allowHardware: Boolean) : HardwareBitmapService() {

    override fun allowHardware(size: Size) = allowHardware
}

/**
 * Android O and O_MR1 have a limited number of file descriptors (1024) per-process.
 * This limit was increased to a safe number in Android P (32768). Each hardware bitmap
 * allocation consumes, on average, 2 file descriptors. In addition, other non-image loading
 * operations can use file descriptors, which increases competition for these resources.
 * This class exists to disable [Bitmap.Config.HARDWARE] allocation if this process gets
 * too close to the limit, as passing the limit can cause crashes and/or rendering issues.
 *
 * NOTE: This must be a singleton since it tracks global file descriptor usage state for the entire process.
 *
 * Modified from Glide's HardwareConfigState.
 */
private object LimitedFileDescriptorHardwareBitmapService : HardwareBitmapService() {

    private const val TAG = "LimitedFileDescriptorHardwareBitmapService"

    private const val MIN_SIZE_DIMENSION = 100
    private const val FILE_DESCRIPTOR_LIMIT = 750
    private const val FILE_DESCRIPTOR_CHECK_INTERVAL = 50

    private val fileDescriptorList = File("/proc/self/fd")

    @Volatile private var decodesSinceLastFileDescriptorCheck = 0
    @Volatile private var hasAvailableFileDescriptors = true

    override fun allowHardware(size: Size): Boolean {
        // Don't use up file descriptors on small bitmaps.
        if (size is PixelSize && (size.width < MIN_SIZE_DIMENSION || size.height < MIN_SIZE_DIMENSION)) {
            return false
        }

        return hasAvailableFileDescriptors()
    }

    @Synchronized
    @WorkerThread
    private fun hasAvailableFileDescriptors(): Boolean {
        // Only check if we have available file descriptors after a
        // set amount of decodes since it's expensive (1-2 milliseconds).
        if (decodesSinceLastFileDescriptorCheck++ >= FILE_DESCRIPTOR_CHECK_INTERVAL) {
            decodesSinceLastFileDescriptorCheck = 0

            val numUsedFileDescriptors = fileDescriptorList.list().orEmpty().count()
            hasAvailableFileDescriptors = numUsedFileDescriptors < FILE_DESCRIPTOR_LIMIT

            if (hasAvailableFileDescriptors) {
                log(TAG, Log.WARN) {
                    "Unable to allocate more hardware bitmaps. Number of used file descriptors: $numUsedFileDescriptors"
                }
            }
        }

        return hasAvailableFileDescriptors
    }
}

/** Maintains a list of devices with broken/incomplete hardware bitmap implementations. */
private object HardwareBitmapBlacklist {

    val IS_BLACKLISTED = isBlacklisted()

    /** Modified from Glide's HardwareConfigState. */
    private fun isBlacklisted(): Boolean {
        val model = Build.MODEL
            ?.takeIf { it.count() >= 7 }
            ?.substring(0, 7)
            ?: return false

        if (model == "LG-Q710") {
            return true
        }

        if (SDK_INT == O) {
            val models = arrayOf("SM-N935", "SM-J720", "SM-G960", "SM-G965", "SM-G935", "SM-G930", "SM-A520")
            return models.contains(model)
        }

        return false
    }
}
