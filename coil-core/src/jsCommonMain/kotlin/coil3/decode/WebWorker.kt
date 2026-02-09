@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.decode

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js
import kotlin.js.unsafeCast
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.impl.NativePointer
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toInt8Array
import org.w3c.dom.MessageEvent
import org.w3c.dom.Worker
import org.w3c.dom.events.Event
import org.w3c.dom.url.URL
import org.w3c.files.Blob

private const val WebWorkerJs = """
let canvas = null;
let context = null;
let cw = 0, ch = 0;

function ensureCanvas(w, h) {
  if (!canvas || w > cw || h > ch) {
    cw = w; ch = h;
    canvas = new OffscreenCanvas(w, h);
    context = canvas.getContext("2d", { willReadFrequently: true });
    context.setTransform(1, 0, 0, 1, 0, 0);
  }
  return context;
}

self.onmessage = async (e) => {
    const { id, data, w, h } = e.data;
    try {
        var blob = new Blob([data]);
        const bmp = await createImageBitmap(blob, {
            resizeWidth: w,
            resizeHeight: h,
            resizeQuality: 'high'
        });
        const ctx = ensureCanvas(w, h);
        ctx.clearRect(0, 0, w, h);
        ctx.drawImage(bmp, 0, 0);
        bmp.close();

        const imgData = ctx.getImageData(0, 0, w, h);
        const rawBuffer = imgData.data.buffer;
        self.postMessage(
            { kind: "result", id: id, buffer: rawBuffer },
            [rawBuffer]
        );
    } catch (err) {
        self.postMessage(
            { kind: "error", id: id, message: err?.message ?? String(err), }
        );
    }
};
"""

private fun blob(code: String): Blob = js("new Blob([code], { type: 'application/javascript' })")

private fun startWorker(code: String): Worker {
    val url = URL.createObjectURL(blob(code))
    val worker = Worker(url)
    URL.revokeObjectURL(url)
    return worker
}

private val worker by lazy { startWorker(WebWorkerJs) }

@OptIn(ExperimentalSkikoApi::class)
internal suspend fun decodeImageAsync(
    bytes: ByteArray,
    width: Int,
    height: Int,
): Bitmap {
    // async decodes an image to a bitmap on a special web worker. doesn't block UI thread :)
    val webBitmap = decodeBytesToBitmap(bytes, width, height)

    // pass bitmap ArrayBuffer to the skiko memory
    val skikoData = webBitmap.passToSkiko()

    val colorInfo = ColorInfo(
        ColorType.RGBA_8888,
        ColorAlphaType.UNPREMUL,
        ColorSpace.sRGB,
    )
    val imageInfo = ImageInfo(colorInfo, width, height)
    val image = Image.makeRaster(imageInfo, skikoData, imageInfo.minRowBytes)
    return Bitmap.makeFromImage(image)
}

private suspend fun ArrayBuffer.passToSkiko(): Data {
    val data = Data.makeUninitialized(byteLength)
    val skikoMemory = getSkikoMemory(awaitSkiko())
    skikoMemory.set(this, data.writableData())
    return data
}

internal expect suspend fun awaitSkiko(): JsAny
private fun getSkikoMemory(skikoWasm: JsAny): ArrayBuffer =
    js("skikoWasm.wasmExports.memory.buffer")

private fun ArrayBuffer.set(data: ArrayBuffer, offset: NativePointer) {
    Int8Array(this).set(Int8Array(data), offset)
}

@OptIn(ExperimentalUuidApi::class)
private suspend fun decodeBytesToBitmap(
    bytes: ByteArray,
    width: Int,
    height: Int,
): ArrayBuffer = suspendCancellableCoroutine { continuation ->
    val id = Uuid.random().toString()
    var responseListener: ((Event) -> Unit)? = null
    var errorListener: ((Event) -> Unit)? = null
    responseListener = { event ->
        val response = (event as? MessageEvent)?.data?.unsafeCast<WebWorkerResponse>()
        if (response != null && response.kind == "result" && response.id == id) {
            worker.removeEventListener("message", responseListener)
            continuation.resume(response.buffer)
        }
    }
    errorListener = { event ->
        val err = event.unsafeCast<WebWorkerError>()
        if (err.id == id) {
            worker.removeEventListener("error", errorListener)
            continuation.resumeWithException(Error("WebWorker error: ${err.message}"))
        }
    }
    worker.addEventListener("message", responseListener)
    worker.addEventListener("error", errorListener)

    worker.postMessage(WebWorkerRequest(id, bytes.toInt8Array(), width, height))

    continuation.invokeOnCancellation {
        worker.removeEventListener("message", responseListener)
        worker.removeEventListener("error", errorListener)
    }
}

private fun WebWorkerRequest(
    id: String,
    arr: Int8Array,
    width: Int,
    height: Int,
): JsAny = js("({ id: id, data: arr, w: width, h: height })")

internal external interface WebWorkerResponse : JsAny {
    val id: String
    val kind: String
    val buffer: ArrayBuffer
}

internal external interface WebWorkerError : JsAny {
    val id: String
    val message: String
}
