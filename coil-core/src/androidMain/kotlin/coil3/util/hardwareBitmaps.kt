package coil3.util

import android.graphics.Bitmap
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.SystemClock
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import coil3.size.Size
import coil3.size.pxOrElse
import java.io.File

/** Create a new [HardwareBitmapService]. */
internal fun HardwareBitmapService(logger: Logger?): HardwareBitmapService = when {
    SDK_INT < 26 || IS_DEVICE_BLOCKED -> ImmutableHardwareBitmapService(false)
    SDK_INT == 26 || SDK_INT == 27 -> LimitedFileDescriptorHardwareBitmapService(logger)
    else -> ImmutableHardwareBitmapService(true)
}

/** Decides if a request can use [Bitmap.Config.HARDWARE]. */
internal sealed interface HardwareBitmapService {

    /** Return 'true' if we are currently able to create [Bitmap.Config.HARDWARE]. */
    @MainThread
    fun allowHardwareMainThread(size: Size): Boolean

    /** Perform any hardware bitmap allocation checks that cannot be done on the main thread. */
    @WorkerThread
    fun allowHardwareWorkerThread(): Boolean
}

/** Returns a fixed value for [allowHardwareMainThread] and [allowHardwareWorkerThread]. */
private class ImmutableHardwareBitmapService(
    private val allowHardware: Boolean,
) : HardwareBitmapService {
    override fun allowHardwareMainThread(size: Size) = allowHardware
    override fun allowHardwareWorkerThread() = allowHardware
}

/** Guards against running out of file descriptors. */
private class LimitedFileDescriptorHardwareBitmapService(
    private val logger: Logger?,
) : HardwareBitmapService {

    override fun allowHardwareMainThread(size: Size): Boolean {
        return size.width.pxOrElse { Int.MAX_VALUE } > MIN_SIZE_DIMENSION &&
            size.height.pxOrElse { Int.MAX_VALUE } > MIN_SIZE_DIMENSION
    }

    override fun allowHardwareWorkerThread(): Boolean {
        return FileDescriptorCounter.hasAvailableFileDescriptors(logger)
    }

    companion object {
        private const val MIN_SIZE_DIMENSION = 100
    }
}

/**
 * API 26 and 27 have a limited number of file descriptors (1024) per-process.
 * This limit was increased to a safe number in API 28 (32768). Each hardware bitmap
 * allocation consumes, on average, 2 file descriptors. In addition, other non-image loading
 * operations can use file descriptors, which increases competition for these resources.
 * This class exists to disable [Bitmap.Config.HARDWARE] allocation if this process gets
 * too close to the limit, as passing the limit can cause crashes and/or rendering issues.
 *
 * NOTE: This must be a singleton as file descriptor usage is shared for the entire process.
 */
private object FileDescriptorCounter {

    private const val TAG = "FileDescriptorCounter"
    private const val FILE_DESCRIPTOR_LIMIT = 800
    private const val FILE_DESCRIPTOR_CHECK_INTERVAL_DECODES = 30
    private const val FILE_DESCRIPTOR_CHECK_INTERVAL_MILLIS = 30_000

    private val fileDescriptorList = File("/proc/self/fd")
    private var decodesSinceLastFileDescriptorCheck = FILE_DESCRIPTOR_CHECK_INTERVAL_DECODES
    private var lastFileDescriptorCheckTimestamp = SystemClock.uptimeMillis()
    private var hasAvailableFileDescriptors = true

    @Synchronized
    @WorkerThread
    fun hasAvailableFileDescriptors(logger: Logger?): Boolean {
        if (checkFileDescriptors()) {
            decodesSinceLastFileDescriptorCheck = 0
            lastFileDescriptorCheckTimestamp = SystemClock.uptimeMillis()

            val numUsedFileDescriptors = fileDescriptorList.list().orEmpty().count()
            hasAvailableFileDescriptors = numUsedFileDescriptors < FILE_DESCRIPTOR_LIMIT
            if (!hasAvailableFileDescriptors) {
                logger?.log(TAG, Logger.Level.Warn) {
                    "Unable to allocate more hardware bitmaps. " +
                        "Number of used file descriptors: $numUsedFileDescriptors"
                }
            }
        }
        return hasAvailableFileDescriptors
    }

    private fun checkFileDescriptors(): Boolean {
        // Only check if we have available file descriptors after a
        // set amount of time/decodes since it's expensive (1-2 milliseconds).
        return decodesSinceLastFileDescriptorCheck++ >= FILE_DESCRIPTOR_CHECK_INTERVAL_DECODES ||
            SystemClock.uptimeMillis() > lastFileDescriptorCheckTimestamp + FILE_DESCRIPTOR_CHECK_INTERVAL_MILLIS
    }
}

/**
 * Maintains a list of devices with broken/incomplete/unstable hardware bitmap implementations.
 *
 * Model names are retrieved from
 * [Google's official device list](https://support.google.com/googleplay/answer/1727131?hl=en).
 */
private val IS_DEVICE_BLOCKED = when (SDK_INT) {
    26 -> run {
        val model = Build.MODEL ?: return@run false

        // Samsung Galaxy (ALL)
        if (model.removePrefix("SAMSUNG-").startsWith("SM-")) return@run true

        val device = Build.DEVICE ?: return@run false

        return@run device in arrayOf(
            "nora", "nora_8917", "nora_8917_n", // Moto E5
            "james", "rjames_f", "rjames_go", "pettyl", // Moto E5 Play
            "hannah", "ahannah", "rhannah", // Moto E5 Plus

            "ali", "ali_n", // Moto G6
            "aljeter", "aljeter_n", "jeter", // Moto G6 Play
            "evert", "evert_n", "evert_nt", // Moto G6 Plus

            "G3112", "G3116", "G3121", "G3123", "G3125", // Xperia XA1
            "G3412", "G3416", "G3421", "G3423", "G3426", // Xperia XA1 Plus
            "G3212", "G3221", "G3223", "G3226", // Xperia XA1 Ultra

            "BV6800Pro", // BlackView BV6800Pro
            "CatS41", // Cat S41
            "Hi9Pro", // CHUWI Hi9 Pro
            "manning", // Lenovo K8 Note
            "N5702L", // NUU Mobile G3
        )
    }

    27 -> run {
        val device = Build.DEVICE ?: return@run false

        return@run device in arrayOf(
            "mcv1s", // LG Tribute Empire
            "mcv3", // LG K11
            "mcv5a", // LG Q7
            "mcv7a", // LG Stylo 4

            "A30ATMO", // T-Mobile REVVL 2
            "A70AXLTMO", // T-Mobile REVVL 2 PLUS

            "A3A_8_4G_TMO", // Alcatel 9027W
            "Edison_CKT", // Alcatel ONYX
            "EDISON_TF", // Alcatel TCL XL2
            "FERMI_TF", // Alcatel A501DL
            "U50A_ATT", // Alcatel TETRA
            "U50A_PLUS_ATT", // Alcatel 5059R
            "U50A_PLUS_TF", // Alcatel TCL LX
            "U50APLUSTMO", // Alcatel 5059Z
            "U5A_PLUS_4G", // Alcatel 1X

            "RCT6513W87DK5e", // RCA Galileo Pro
            "RCT6873W42BMF9A", // RCA Voyager
            "RCT6A03W13", // RCA 10 Viking
            "RCT6B03W12", // RCA Atlas 10 Pro
            "RCT6B03W13", // RCA Atlas 10 Pro+
            "RCT6T06E13", // RCA Artemis 10

            "A3_Pro", // Umidigi A3 Pro
            "One", // Umidigi One
            "One_Max", // Umidigi One Max
            "One_Pro", // Umidigi One Pro
            "Z2", // Umidigi Z2
            "Z2_PRO", // Umidigi Z2 Pro

            "Armor_3", // Ulefone Armor 3
            "Armor_6", // Ulefone Armor 6

            "Blackview", // Blackview BV6000
            "BV9500", // Blackview BV9500
            "BV9500Pro", // Blackview BV9500Pro

            "A6L-C", // Nuu A6L-C
            "N5002LA", // Nuu A7L
            "N5501LA", // Nuu A5L

            "Power_2_Pro", // Leagoo Power 2 Pro
            "Power_5", // Leagoo Power 5
            "Z9", // Leagoo Z9

            "V0310WW", // Blu VIVO VI+
            "V0330WW", // Blu VIVO XI

            "A3", // BenQ A3
            "ASUS_X018_4", // Asus ZenFone Max Plus M1 (ZB570TL)
            "C210AE", // Wiko Life
            "fireball", // DROID Incredible 4G LTE
            "ILA_X1", // iLA X1
            "Infinix-X605_sprout", // Infinix NOTE 5 Stylus
            "j7maxlte", // Samsung Galaxy J7 Max
            "KING_KONG_3", // Cubot King Kong 3
            "M10500", // Packard Bell M10500
            "S70", // Altice ALTICE S70
            "S80Lite", // Doogee S80Lite
            "SGINO6", // SGiNO 6
            "st18c10bnn", // Barnes and Noble BNTV650
            "TECNO-CA8", // Tecno CAMON X Pro,
            "SHIFT6m", // SHIFT 6m
        )
    }

    else -> false
}
