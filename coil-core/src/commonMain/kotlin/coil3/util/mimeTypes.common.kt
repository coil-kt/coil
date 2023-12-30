package coil3.util

import coil3.annotation.InternalCoilApi

@InternalCoilApi
object MimeTypeMap {
    fun getMimeTypeFromUrl(url: String): String? {
        if (url.isBlank()) {
            return null
        }

        val extension = url
            .substringBeforeLast('#') // Strip the fragment.
            .substringBeforeLast('?') // Strip the query.
            .substringAfterLast('/') // Get the last path segment.
            .substringAfterLast('.', missingDelimiterValue = "") // Get the file extension.

        return getMimeTypeFromExtension(extension)
    }

    fun getMimeTypeFromExtension(extension: String): String? {
        if (extension.isBlank()) {
            return null
        }

        val lowerExtension = extension.lowercase()
        return extensionFromMimeTypeMap(lowerExtension) ?: mimeTypeData[lowerExtension]
    }
}

internal expect fun extensionFromMimeTypeMap(extension: String): String?

// https://mimetype.io/all-types
private val mimeTypeData = buildMap {
    put("avif", "image/avif")
    put("avifs", "image/avif")
    put("bmp", "image/bmp")
    put("cgm", "image/cgm")
    put("g3", "image/g3fax")
    put("gif", "image/gif")
    put("heif", "image/heic")
    put("heic", "image/heic")
    put("ief", "image/ief")
    put("jpe", "image/jpeg")
    put("jpeg", "image/jpeg")
    put("jpg", "image/jpeg")
    put("pjpg", "image/jpeg")
    put("jfif", "image/jpeg")
    put("jfif-tbnl", "image/jpeg")
    put("jif", "image/jpeg")
    put("jpe", "image/pjpeg")
    put("jpeg", "image/pjpeg")
    put("jpg", "image/pjpeg")
    put("pjpg", "image/pjpeg")
    put("jfi", "image/pjpeg")
    put("jfif", "image/pjpeg")
    put("jfif-tbnl", "image/pjpeg")
    put("jif", "image/pjpeg")
    put("png", "image/png")
    put("btif", "image/prs.btif")
    put("svg", "image/svg+xml")
    put("svgz", "image/svg+xml")
    put("tif", "image/tiff")
    put("tiff", "image/tiff")
    put("psd", "image/vnd.adobe.photoshop")
    put("djv", "image/vnd.djvu")
    put("djvu", "image/vnd.djvu")
    put("dwg", "image/vnd.dwg")
    put("dxf", "image/vnd.dxf")
    put("fbs", "image/vnd.fastbidsheet")
    put("fpx", "image/vnd.fpx")
    put("fst", "image/vnd.fst")
    put("mmr", "image/vnd.fujixerox.edmics-mmr")
    put("rlc", "image/vnd.fujixerox.edmics-rlc")
    put("mdi", "image/vnd.ms-modi")
    put("npx", "image/vnd.net-fpx")
    put("wbmp", "image/vnd.wap.wbmp")
    put("xif", "image/vnd.xiff")
    put("webp", "image/webp")
    put("dng", "image/x-adobe-dng")
    put("cr2", "image/x-canon-cr2")
    put("crw", "image/x-canon-crw")
    put("ras", "image/x-cmu-raster")
    put("cmx", "image/x-cmx")
    put("erf", "image/x-epson-erf")
    put("fh", "image/x-freehand")
    put("fh4", "image/x-freehand")
    put("fh5", "image/x-freehand")
    put("fh7", "image/x-freehand")
    put("fhc", "image/x-freehand")
    put("raf", "image/x-fuji-raf")
    put("icns", "image/x-icns")
    put("ico", "image/x-icon")
    put("dcr", "image/x-kodak-dcr")
    put("k25", "image/x-kodak-k25")
    put("kdc", "image/x-kodak-kdc")
    put("mrw", "image/x-minolta-mrw")
    put("nef", "image/x-nikon-nef")
    put("orf", "image/x-olympus-orf")
    put("raw", "image/x-panasonic-raw")
    put("rw2", "image/x-panasonic-raw")
    put("rwl", "image/x-panasonic-raw")
    put("pcx", "image/x-pcx")
    put("pef", "image/x-pentax-pef")
    put("ptx", "image/x-pentax-pef")
    put("pct", "image/x-pict")
    put("pic", "image/x-pict")
    put("pnm", "image/x-portable-anymap")
    put("pbm", "image/x-portable-bitmap")
    put("pgm", "image/x-portable-graymap")
    put("ppm", "image/x-portable-pixmap")
    put("rgb", "image/x-rgb")
    put("x3f", "image/x-sigma-x3f")
    put("arw", "image/x-sony-arw")
    put("sr2", "image/x-sony-sr2")
    put("srf", "image/x-sony-srf")
    put("xbm", "image/x-xbitmap")
    put("xpm", "image/x-xpixmap")
    put("xwd", "image/x-xwindowdump")

    put("3gp", "video/3gpp")
    put("3g2", "video/3gpp2")
    put("h261", "video/h261")
    put("h263", "video/h263")
    put("h264", "video/h264")
    put("jpgv", "video/jpeg")
    put("jpgm", "video/jpm")
    put("jpm", "video/jpm")
    put("mj2", "video/mj2")
    put("mjp2", "video/mj2")
    put("ts", "video/mp2t")
    put("mp4", "video/mp4")
    put("mp4v", "video/mp4")
    put("mpg4", "video/mp4")
    put("m1v", "video/mpeg")
    put("m2v", "video/mpeg")
    put("mpa", "video/mpeg")
    put("mpe", "video/mpeg")
    put("mpeg", "video/mpeg")
    put("mpg", "video/mpeg")
    put("ogv", "video/ogg")
    put("mov", "video/quicktime")
    put("qt", "video/quicktime")
    put("fvt", "video/vnd.fvt")
    put("m4u", "video/vnd.mpegurl")
    put("mxu", "video/vnd.mpegurl")
    put("pyv", "video/vnd.ms-playready.media.pyv")
    put("viv", "video/vnd.vivo")
    put("webm", "video/webm")
    put("f4v", "video/x-f4v")
    put("fli", "video/x-fli")
    put("flv", "video/x-flv")
    put("m4v", "video/x-m4v")
    put("mkv", "video/x-matroska")
    put("asf", "video/x-ms-asf")
    put("asx", "video/x-ms-asf")
    put("wm", "video/x-ms-wm")
    put("wmv", "video/x-ms-wmv")
    put("wmx", "video/x-ms-wmx")
    put("wvx", "video/x-ms-wvx")
    put("avi", "video/x-msvideo")
    put("movie", "video/x-sgi-movie")
}
