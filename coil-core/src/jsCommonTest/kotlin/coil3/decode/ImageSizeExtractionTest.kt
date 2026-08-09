package coil3.decode

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.decodeBase64

class ImageSizeExtractionTest {
    private val jpeg = (
        "/9j/4AAQSkZJRgABAQEBLAEsAAD/2wBDABoSExcTEBoXFRcdGxofJ0AqJyMjJ084PC9AXVJiYVxSWlln" +
            "dJR+Z22Mb1laga+CjJmepqemZHy2w7ShwZSjpp//2wBDARsdHSciJ0wqKkyfalpqn5+fn5+fn5+fn5+f" +
            "n5+fn5+fn5+fn5+fn5+fn5+fn5+fn5+fn5+fn5+fn5+fn5+fn5//wgARCAAKAAoDAREAAhEBAxEB/8QA" +
            "FwAAAwEAAAAAAAAAAAAAAAAAAAEEBf/EABYBAQEBAAAAAAAAAAAAAAAAAAADAv/aAAwDAQACEAMQAAAB" +
            "zc6aYUTn/8QAGRAAAQUAAAAAAAAAAAAAAAAAAAECEjEy/9oACAEBAAEFArIqN0f/xAAaEQACAgMAAAAA" +
            "AAAAAAAAAAAAAgEQEiEx/9oACAEDAQE/AWbRlA3K/8QAGREAAgMBAAAAAAAAAAAAAAAAADEBAhAS/9oA" +
            "CAECAQE/AUdQWWf/xAAXEAADAQAAAAAAAAAAAAAAAAAAEBEx/9oACAEBAAY/AoY//8QAGhABAAEFAAAA" +
            "AAAAAAAAAAAAAQAQESExQf/aAAgBAQABPyHkGabXFbz/2gAMAwEAAgADAAAAEN+//8QAGREAAwADAAAA" +
            "AAAAAAAAAAAAAAERITFh/9oACAEDAQE/EEtMSHQ2CR//xAAZEQEBAAMBAAAAAAAAAAAAAAABABExQWH/" +
            "2gAIAQIBAT8Q6VvS2QGL/8QAGhAAAwEAAwAAAAAAAAAAAAAAAAEhETFBUf/aAAgBAQABPxCvLSdjW8bD" +
            "41SmhXz6f//Z"
        ).decodeBase64()!!.toByteArray()
    private val exifOrientation6 = intArrayOf(
        0x4D, 0x4D, // Big endian.
        0x00, 0x2A, // TIFF magic number.
        0x00, 0x00, 0x00, 0x08, // IFD0 offset.
        0x00, 0x01, // Entry count.
        0x01, 0x12, // Orientation tag.
        0x00, 0x03, // SHORT.
        0x00, 0x00, 0x00, 0x01, // Value count.
        0x00, 0x06, 0x00, 0x00, // Orientation 6.
        0x00, 0x00, 0x00, 0x00, // Next IFD offset.
    )
    private val jpegWithExifOrientation6 = intArrayOf(
        0xFF, 0xD8, // SOI.
        0xFF, 0xE1, // APP1.
        0x00, 0x22, // Segment length.
        0x45, 0x78, 0x69, 0x66, 0x00, 0x00, // Exif header.
        *exifOrientation6,
        0xFF, 0xC0, // SOF0.
        0x00, 0x11, // Segment length.
        0x08, // Precision.
        0x00, 0x14, // Height.
        0x00, 0x0A, // Width.
        0x03, // Component count.
        0x01, 0x11, 0x00,
        0x02, 0x11, 0x00,
        0x03, 0x11, 0x00,
        0xFF, 0xD9, // EOI.
    ).map { it.toByte() }.toByteArray()
    private val png = (
        "iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAIAAAACUFjqAAAAAXNSR0IB2cksfwAAAARnQU1BAACxjwv8" +
            "YQUAAAAgY0hSTQAAeiYAAICEAAD6AAAAgOgAAHUwAADqYAAAOpgAABdwnLpRPAAAAAlwSFlzAAAuIwAA" +
            "LiMBeKU/dgAAAAd0SU1FB+oCCwkxF5yV/98AAABdSURBVBjTtc6xDcIwFATQ58iFC6QwhSX2iJiBnuUy" +
            "ARWTZAhkOpcpEihsRJfX/K+75gJudrNWwJORN1NXD9u5+C3ixcMRQh+d7lKGuoh9nbLzFcp3uT/LG3VR" +
            "Ps8KSNENWM+pI9YAAAAASUVORK5CYII="
        ).decodeBase64()!!.toByteArray()
    private val pngWithExifOrientation6 = intArrayOf(
        0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature.
        0x00, 0x00, 0x00, 0x0D, // Chunk length.
        0x49, 0x48, 0x44, 0x52, // IHDR.
        0x00, 0x00, 0x00, 0x0A, // Width.
        0x00, 0x00, 0x00, 0x14, // Height.
        0x08, 0x02, 0x00, 0x00, 0x00, // Remaining IHDR data.
        0x00, 0x00, 0x00, 0x00, // CRC.
        0x00, 0x00, 0x00, 0x1A, // Chunk length.
        0x65, 0x58, 0x49, 0x66, // eXIf.
        *exifOrientation6,
        0x00, 0x00, 0x00, 0x00, // CRC.
        0x00, 0x00, 0x00, 0x00, // Chunk length.
        0x49, 0x45, 0x4E, 0x44, // IEND.
        0x00, 0x00, 0x00, 0x00, // CRC.
    ).map { it.toByte() }.toByteArray()
    private val webp = (
        "UklGRqIAAABXRUJQVlA4IJYAAADwAgCdASoKAAoAAMASJbACdLoAkQDpAPRJB8C1GwNIEAD+/6IdbkpT" +
            "r3YsZ4X/1vOP/VLJh8T8lz82YxIyGV47BcfOmEaVCS1fRi4yFYwxUOO1HZ89ljeH9gTrmUf/FaTKevz1" +
            "/G/8YFv+Dfdfb0/Jwsv/hyn3h7/R9ttfrBnO/3I985yfjf+MC2qOZeXV+Tcq/+CCAAA="
        ).decodeBase64()!!.toByteArray()
    private val webp_VP8L = (
        "UklGRswAAABXRUJQVlA4TL8AAAAvCAACAE/BNpIkJ/NaWwIN+QeIy5EG20aSFPUxPpP9+uw++/OJUSRJ" +
            "iqqXQcL697e/4+v5DyAICAJgYAiAIIAwJcAIqAamgCTRn/zZtAuUCGT6pJG41LBDeCgnb7+oUvV3sr35" +
            "nodOn+ffzvqK0+T4beXQjyKC/CzNRlkCDCRJMpzttfds7vki+h/FNNY2+hirZe0QfQjlvA0iUMi87RtN" +
            "CVeZgXQEhDIIjv04E4UUON7vjkFKnN//xCAVSfdzAQA="
        ).decodeBase64()!!.toByteArray()
    private val webp_VP8X = (
        "UklGRiQDAABXRUJQVlA4WAoAAAAgAAAABwAABwAASUNDUKACAAAAAAKgbGNtcwRAAABtbnRyUkdCIFhZ" +
            "WiAH6gADAAMAFQA7ACBhY3NwQVBQTAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA9tYAAQAAAADTLWxj" +
            "bXMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1kZXNjAAABIAAA" +
            "AEBjcHJ0AAABYAAAADZ3dHB0AAABmAAAABRjaGFkAAABrAAAACxyWFlaAAAB2AAAABRiWFlaAAAB7AAA" +
            "ABRnWFlaAAACAAAAABRyVFJDAAACFAAAACBnVFJDAAACFAAAACBiVFJDAAACFAAAACBjaHJtAAACNAAA" +
            "ACRkbW5kAAACWAAAACRkbWRkAAACfAAAACRtbHVjAAAAAAAAAAEAAAAMZW5VUwAAACQAAAAcAEcASQBN" +
            "AFAAIABiAHUAaQBsAHQALQBpAG4AIABzAFIARwBCbWx1YwAAAAAAAAABAAAADGVuVVMAAAAaAAAAHABQ" +
            "AHUAYgBsAGkAYwAgAEQAbwBtAGEAaQBuAABYWVogAAAAAAAA9tYAAQAAAADTLXNmMzIAAAAAAAEMQgAA" +
            "Bd7///MlAAAHkwAA/ZD///uh///9ogAAA9wAAMBuWFlaIAAAAAAAAG+gAAA49QAAA5BYWVogAAAAAAAA" +
            "JJ8AAA+EAAC2xFhZWiAAAAAAAABilwAAt4cAABjZcGFyYQAAAAAAAwAAAAJmZgAA8qcAAA1ZAAAT0AAA" +
            "CltjaHJtAAAAAAADAAAAAKPXAABUfAAATM0AAJmaAAAmZwAAD1xtbHVjAAAAAAAAAAEAAAAMZW5VUwAA" +
            "AAgAAAAcAEcASQBNAFBtbHVjAAAAAAAAAAEAAAAMZW5VUwAAAAgAAAAcAHMAUgBHAEJWUDggXgAAADAC" +
            "AJ0BKggACAAAwBIlsAJ0ugH4AALPcx3gAP7/uxe17up9/+aTPv/y6r/41ak0v5gXaD6ln/ZB83KC/1/p" +
            "vpt+76Zy/Bl2/uXv+ZqHfL4BJ6v8lOMxG+DLt/bsAAA="
        ).decodeBase64()!!.toByteArray()
    private val webpWithExifOrientation6 = intArrayOf(
        0x52, 0x49, 0x46, 0x46, // RIFF.
        0x38, 0x00, 0x00, 0x00, // File size.
        0x57, 0x45, 0x42, 0x50, // WEBP.
        0x56, 0x50, 0x38, 0x58, // VP8X.
        0x0A, 0x00, 0x00, 0x00, // Chunk length.
        0x08, 0x00, 0x00, 0x00, // EXIF flag and reserved bytes.
        0x09, 0x00, 0x00, // Canvas width minus one.
        0x13, 0x00, 0x00, // Canvas height minus one.
        0x45, 0x58, 0x49, 0x46, // EXIF.
        0x1A, 0x00, 0x00, 0x00, // Chunk length.
        *exifOrientation6,
    ).map { it.toByte() }.toByteArray()
    private val webpWithExifPrefixOrientation6 = intArrayOf(
        0x52, 0x49, 0x46, 0x46, // RIFF.
        0x3E, 0x00, 0x00, 0x00, // File size.
        0x57, 0x45, 0x42, 0x50, // WEBP.
        0x56, 0x50, 0x38, 0x58, // VP8X.
        0x0A, 0x00, 0x00, 0x00, // Chunk length.
        0x08, 0x00, 0x00, 0x00, // EXIF flag and reserved bytes.
        0x09, 0x00, 0x00, // Canvas width minus one.
        0x13, 0x00, 0x00, // Canvas height minus one.
        0x45, 0x58, 0x49, 0x46, // EXIF.
        0x20, 0x00, 0x00, 0x00, // Chunk length.
        0x45, 0x78, 0x69, 0x66, 0x00, 0x00, // Exif header.
        *exifOrientation6,
    ).map { it.toByte() }.toByteArray()
    private val bmp = (
        "Qk1uAgAAAAAAAIoAAAB8AAAACwAAAAsAAAABACAAAwAAAOQBAAATCwAAEwsAAAAAAAAAAAAAAAD/AAD/" +
            "AAD/AAAAAAAA/0JHUnMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
            "AAACAAAAAAAAAAAAAAAAAAAAAAIA/wABAP8AAAL/AAAC/wIAAf8BAAD/AEQF/wJ5Ev8Fkhn/A4wT/wB0" +
            "A/8AAgD/AAEA/wAAAv8AAAL/AQAB/wAAAP8FTQ3/GY8o/yWxN/8cqC7/A4wT/wEAAP8AAAD/AAAC/wAA" +
            "Av8AAAH/AAAA/whQEP8ekyv/K7g8/yWxN/8Fkhn/AgAA/wAAAP8AAAL/AAAC/wAAAf8AAAD/B0MO/xZ4" +
            "Iv8ekyv/GY8o/wJ5Ef8CAAH/AAAB/wAAAP8AAQD/AQAB/wEAAf8CJQX/B0MO/whQEP8FTQ3/AEMF/wEA" +
            "C/8AAAX/AAAB/wAAAP8BAAP/AgEE/wEAAf8AAAD/AAAA/wEAAP8CAAD/AQEl/wkKQv8HCVf/BgtW/wMF" +
            "Nv8BAAT/AgAB/wEAAP8BAAD/AgAB/wIAAf8CAjn/ExVq/xMXkP8RF4z/BwxZ/wAAAv8BAAD/AgAA/wIA" +
            "AP8AAAH/AAAC/wICPv8VFXX/GRyi/xUYm/8ICmH/AAAB/wACAP8BAQD/AgAA/wAAAP8AAAH/AwI8/xoX" +
            "bv8YFoz/FBWG/wkKVf8AAAH/AAIA/wECAP8CAQD/AAEA/wABAP8BADf/CAJV/wQCbf8CAmj/AQFB/wAB" +
            "Bv8AAgD/AQIA/wIBAP8AAgD/AAIA/w=="
        ).decodeBase64()!!.toByteArray()

    @Test
    fun testPngSizeExtraction() {
        assertNull(getPngSizeOrNull(jpeg))
        assertNull(getPngSizeOrNull(webp))
        assertNull(getPngSizeOrNull(webp_VP8X))
        assertNull(getPngSizeOrNull(webp_VP8L))
        assertNull(getPngSizeOrNull(bmp))
        assertEquals(10 to 10, getPngSizeOrNull(png))
        assertEquals(20 to 10, getPngSizeOrNull(pngWithExifOrientation6))
    }

    @Test
    fun testJpegSizeExtraction() {
        assertNull(getJpegSizeOrNull(png))
        assertNull(getJpegSizeOrNull(webp))
        assertNull(getPngSizeOrNull(webp_VP8X))
        assertNull(getPngSizeOrNull(webp_VP8L))
        assertNull(getPngSizeOrNull(bmp))
        assertEquals(10 to 10, getJpegSizeOrNull(jpeg))
        assertEquals(20 to 10, getJpegSizeOrNull(jpegWithExifOrientation6))
    }

    @Test
    fun testWebpSizeExtraction() {
        assertNull(getWebpSizeOrNull(png))
        assertNull(getWebpSizeOrNull(jpeg))
        assertNull(getWebpSizeOrNull(bmp))
        assertEquals(10 to 10, getWebpSizeOrNull(webp))
        assertEquals(9 to 9, getWebpSizeOrNull(webp_VP8L))
        assertEquals(8 to 8, getWebpSizeOrNull(webp_VP8X))
        assertEquals(20 to 10, getWebpSizeOrNull(webpWithExifOrientation6))
        assertEquals(20 to 10, getWebpSizeOrNull(webpWithExifPrefixOrientation6))
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    @Test
    fun testSizeExtraction() = runTest {
        assertEquals(10 to 10, getOriginalSize(jpeg))
        assertEquals(10 to 10, getOriginalSize(png))
        assertEquals(10 to 10, getOriginalSize(webp))
        assertEquals(9 to 9, getOriginalSize(webp_VP8L))
        assertEquals(8 to 8, getOriginalSize(webp_VP8X))
        assertEquals(20 to 10, getOriginalSize(pngWithExifOrientation6))
        assertEquals(20 to 10, getOriginalSize(webpWithExifOrientation6))

        awaitSkiko()
        assertEquals(11 to 11, getOriginalSize(bmp))
    }
}
