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
    private val png = (
        "iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAIAAAACUFjqAAAAAXNSR0IB2cksfwAAAARnQU1BAACxjwv8" +
            "YQUAAAAgY0hSTQAAeiYAAICEAAD6AAAAgOgAAHUwAADqYAAAOpgAABdwnLpRPAAAAAlwSFlzAAAuIwAA" +
            "LiMBeKU/dgAAAAd0SU1FB+oCCwkxF5yV/98AAABdSURBVBjTtc6xDcIwFATQ58iFC6QwhSX2iJiBnuUy" +
            "ARWTZAhkOpcpEihsRJfX/K+75gJudrNWwJORN1NXD9u5+C3ixcMRQh+d7lKGuoh9nbLzFcp3uT/LG3VR" +
            "Ps8KSNENWM+pI9YAAAAASUVORK5CYII="
        ).decodeBase64()!!.toByteArray()
    private val webp = (
        "UklGRqIAAABXRUJQVlA4IJYAAADwAgCdASoKAAoAAMASJbACdLoAkQDpAPRJB8C1GwNIEAD+/6IdbkpT" +
            "r3YsZ4X/1vOP/VLJh8T8lz82YxIyGV47BcfOmEaVCS1fRi4yFYwxUOO1HZ89ljeH9gTrmUf/FaTKevz1" +
            "/G/8YFv+Dfdfb0/Jwsv/hyn3h7/R9ttfrBnO/3I985yfjf+MC2qOZeXV+Tcq/+CCAAA="
        ).decodeBase64()!!.toByteArray()

    @Test
    fun testPngSizeExtraction() {
        assertNull(getPngSizeOrNull(jpeg))
        assertNull(getPngSizeOrNull(webp))
        assertEquals(10 to 10, getPngSizeOrNull(png))
    }

    @Test
    fun testJpegSizeExtraction() {
        assertNull(getJpegSizeOrNull(png))
        assertNull(getJpegSizeOrNull(webp))
        assertEquals(10 to 10, getJpegSizeOrNull(jpeg))
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    @Test
    fun testSizeExtraction() = runTest {
        assertEquals(10 to 10, getOriginalSize(jpeg))
        assertEquals(10 to 10, getOriginalSize(png))

        awaitSkiko()
        assertEquals(10 to 10, getOriginalSize(webp))
    }
}
