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
            SDK_INT < O || IS_BLACKLISTED -> ImmutableHardwareBitmapService(false)
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

/**
 * Maintains a list of devices with broken/incomplete/unstable hardware bitmap implementations.
 *
 * Model names are retrieved from [Google's official device list](https://support.google.com/googleplay/answer/1727131?hl=en).
 */
private object HardwareBitmapBlacklist {

    val IS_BLACKLISTED = isBlacklisted()

    private fun isBlacklisted(): Boolean {
        val model = Build.MODEL ?: return false

        if (SDK_INT == O) {
            return model in arrayOf(
                // Moto G6 Play
                "Moto G Play", "moto g(6) play",

                // Moto E5 Play/Cruise
                "Moto E", "moto e5 cruise", "moto e5 play",

                // Samsung Galaxy Amp Prime 3
                "SM-J337AZ",

                // Samsung Galaxy A5 (2017)
                "SM-A520F", "SM-A520X", "SM-A520W", "SM-A520K", "SM-A520L", "SM-A520S",

                // Samsung Galaxy A8 (2018)
                "SM-A530F", "SM-A530X", "SM-A530W", "SM-A530N",

                // Samsung Galaxy J7 Duo
                "SM-J720F", "SM-J720M",

                // Samsung Galaxy J7 Crown
                "SM-S767VL", "SM-S757BL",

                // Samsung Galaxy S7
                "SM-G930F", "SM-G930X", "SM-G930W8", "SM-G930K", "SM-G930L", "SM-G930S", "SM-G930R7", "SAMSUNG-SM-G930AZ",
                "SAMSUNG-SM-G930A", "SM-G930VC", "SM-G9300", "SM-G9308", "SM-G930R6", "SM-G930T1", "SM-G930P", "SM-G930VL",
                "SM-G930T", "SM-G930U", "SM-G930R4", "SM-G930V",

                // Samsung Galaxy S7 Edge
                "SC-02H", "SCV33", "SM-G935X", "SM-G935W8", "SM-G935K", "SM-G935S", "SAMSUNG-SM-G935A", "SM-G935VC", "SM-G935P",
                "SM-G935T", "SM-G935R4", "SM-G935V", "SM-G935F", "SM-G935L", "SM-G9350", "SM-G935U",

                // Samsung Galaxy Note 7 FE
                "SM-N935F", "SM-N935K", "SM-N935L", "SM-N935S",

                // Samsung Galaxy Note 8
                "GT-N5100", "GT-N5105", "GT-N5120", "SAMSUNG-SGH-I467", "SGH-I467M", "GT-N5110", "SHW-M500W",

                // Samsung Galaxy S9
                "SC-02K", "SCV38", "SM-G960F", "SM-G960N", "SM-G9600", "SM-G9608", "SM-G960W", "SM-G960U", "SM-G960U1",

                // Samsung Galaxy S9+
                "SC-03K", "SCV39", "SM-G965F", "SM-G965N", "SM-G9650", "SM-G965W", "SM-G965U", "SM-G965U1"
            )
        }

        if (SDK_INT == O_MR1) {
            return model in arrayOf(
                // Nuu A7L
                "N5002L",

                // Cubot King Kong 3
                "KING_KONG_3",

                // BLU VIVO XI+
                "Vivo XI PLUS", "Vivo XI+",

                // Revvl 2 Plus
                "REVVL 2 PLUS", "6062W",

                // LG Stylo 4
                "LM-Q710(FGN)", "LM-Q710.FG", "LM-Q710.FGN", "LML713DL", "LG-Q710AL", "LG-Q710PL",

                // LG Tribute Empire
                "LM-X220PM",

                // LG K11
                "LM-X410(FN)", "LM-X410.F", "LM-X410.FN",

                // LG Q7
                "LM-Q610.FG", "LM-Q610.FGN", "LM-Q617.FGN",

                // Alcatel A30/3T
                "9027W",

                // Alcatel Tetra
                "5041C",

                // Alcatel 1X
                "5059Z",

                // Alcatel TCL A1
                "A501DL",

                // Alcatel TCL LX
                "A502DL",

                // Alcatel ONYX
                "Alcatel_5008R", "5008R",

                // RCA Atlas 10
                "RCT6B03W13",

                // HTC One Max
                "HTC One max", "HTC_One_max", "HTC0P3P7",

                // Wiko Life
                "C210AE"
            )
        }

        return false
    }
}
