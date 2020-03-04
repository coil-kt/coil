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
 * Adapted from [Glide](https://github.com/bumptech/glide)'s HardwareConfigState.
 * Glide's license information is available [here](https://github.com/bumptech/glide/blob/master/LICENSE).
 */
private object LimitedFileDescriptorHardwareBitmapService : HardwareBitmapService() {

    private const val TAG = "LimitedFileDescriptorHardwareBitmapService"

    private const val MIN_SIZE_DIMENSION = 80
    private const val FILE_DESCRIPTOR_LIMIT = 800
    private const val FILE_DESCRIPTOR_CHECK_INTERVAL = 40

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

    val IS_BLACKLISTED = when (SDK_INT) {
        26 -> run {
            val model = Build.MODEL ?: return@run false

            // Samsung Galaxy (ALL)
            if (model.removePrefix("SAMSUNG-").startsWith("SM-")) return@run true

            // Moto E5
            if (model == "Moto E" || model == "XT1924-9" ||
                model.startsWith("moto e5", true)) return@run true

            // Moto G6
            if (model == "Moto G Play" || model == "XT1925-10" ||
                model.startsWith("moto g(6)", true)) return@run true

            return@run false
        }
        27 -> run {
            val device = Build.DEVICE ?: return@run false

            return@run device in arrayOf(
                "A3", // BenQ A3
                "A30ATMO", // T-Mobile REVVL 2
                "A3_Pro", // Umidigi A3 Pro
                "A3A_8_4G_TMO", // Alcatel 9027W
                "A6L-C", // Nuu A6L-C
                "A70AXLTMO", // T-Mobile REVVL 2 PLUS
                "Armor_3", // Ulefone Armor 3
                "Armor_6", // Ulefone Armor 6
                "ASUS_X018_4", // Asus ZenFone Max Plus M1 (ZB570TL)
                "Blackview", // Blackview BV6000
                "BV9500", // Blackview BV9500
                "BV9500Pro", // Blackview BV9500Pro
                "C210AE", // Wiko Life
                "Edison_CKT", // Alcatel ONYX
                "EDISON_TF", // TCL XL2
                "FERMI_TF", // Alcatel A501DL
                "ILA_X1", // iLA X1
                "Infinix-X605_sprout", // Infinix NOTE 5 Stylus
                "j7maxlte", // Samsung Galaxy J7 Max
                "KING_KONG_3", // Cubot King Kong 3
                "M10500", // Packard Bell M10500
                "mcv1s", // LG Tribute Empire
                "mcv3", // LG K11
                "mcv5a", // LG Q7
                "mcv7a", // LG Stylo 4
                "N5002LA", // Nuu A7L
                "N5501LA", // Nuu A5L
                "One", // Umidigi One
                "One_Max", // Umidigi One Max
                "One_Pro", // Umidigi One Pro
                "Power_5", // Leagoo Power 5
                "RCT6513W87DK5e", // RCA Galileo Pro
                "RCT6873W42BMF9A", // RCA Voyager
                "RCT6A03W13", // RCA 10 Viking
                "RCT6B03W12", // RCA Atlas 10 Pro
                "RCT6B03W13", // RCA Atlas 10 Pro+
                "RCT6T06E13", // RCA Artemis 10
                "S70", // Altice ALTICE S70
                "S80Lite", // Doogee S80Lite
                "SGINO6", // SGiNO 6
                "st18c10bnn", // Barnes and Noble BNTV650
                "TECNO-CA8", // Tecno CAMON X Pro
                "U50A_ATT", // Alcatel TETRA
                "U50A_PLUS_ATT", // Alcatel 5059R
                "U50A_PLUS_TF", // Alcatel TCL LX
                "U50APLUSTMO", // Alcatel 5059Z
                "U5A_PLUS_4G", // Alcatel 1X
                "V0310WW", // Blu VIVO VI+
                "V0330WW", // Blu VIVO XI
                "Z2", // Umidigi Z2
                "Z2_PRO", // Umidigi Z2 Pro
                "Z9" // Leagoo Z9
            )
        }
        else -> false
    }
}
