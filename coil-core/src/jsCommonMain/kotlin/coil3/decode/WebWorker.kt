@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.decode

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.asJsException
import kotlin.js.js
import kotlin.js.set
import kotlin.js.unsafeCast
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.webext.installPixelsFromArrayBuffer
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.jetbrains.skiko.InternalSkikoApi
import org.jetbrains.skiko.wasm.awaitSkiko
import org.khronos.webgl.ArrayBuffer
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
const active = new Set();
const cancelled = new Set();

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
    const { kind, id } = e.data;
    if (kind === "cancel") {
        if (active.has(id)) cancelled.add(id);
        return;
    }

    const { data, w, h } = e.data;
    active.add(id);
    try {
        var blob = new Blob([data]);
        var bmp = null;
        try {
            bmp = await createImageBitmap(blob, {
                resizeWidth: w,
                resizeHeight: h,
                resizeQuality: 'high'
            });
            if (cancelled.has(id)) return;

            const ctx = ensureCanvas(w, h);
            ctx.clearRect(0, 0, w, h);
            ctx.drawImage(bmp, 0, 0);
            if (cancelled.has(id)) return;

            const imgData = ctx.getImageData(0, 0, w, h);
            const rawBuffer = imgData.data.buffer;
            self.postMessage(
                { kind: "result", id: id, buffer: rawBuffer },
                [rawBuffer]
            );
        } finally {
            bmp?.close();
        }
    } catch (err) {
        if (!cancelled.has(id)) {
            self.postMessage(
                { kind: "error", id: id, message: err?.message ?? String(err), }
            );
        }
    } finally {
        active.delete(id);
        cancelled.delete(id);
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

private val pendingRequests = mutableMapOf<String, CancellableContinuation<ArrayBuffer>>()

private val workerMessageListener: (Event) -> Unit = { event ->
    val data = (event as? MessageEvent)?.data?.unsafeCast<WebWorkerMessage>()
    if (data != null) {
        val continuation = pendingRequests.remove(data.id)
        if (continuation?.isActive == true) {
            when (data.kind) {
                "result" -> continuation.resume(data.unsafeCast<WebWorkerResponse>().buffer)
                "error" -> {
                    val message = data.unsafeCast<WebWorkerError>().message
                    continuation.resumeWithException(
                        IllegalStateException("WebWorker error: $message"),
                    )
                }
            }
        }
    }
}

private val workerErrorListener: (Event) -> Unit = { event ->
    val continuations = pendingRequests.values.toList()
    pendingRequests.clear()
    continuations.forEach { continuation ->
        if (continuation.isActive) {
            continuation.resumeWithException(IllegalStateException("WebWorker error: $event"))
        }
    }
}

private val worker by lazy {
    startWorker(WebWorkerJs).apply {
        addEventListener("message", workerMessageListener)
        addEventListener("error", workerErrorListener)
    }
}

@OptIn(ExperimentalSkikoApi::class)
internal suspend fun decodeImageAsync(
    bytes: ByteArray,
    width: Int,
    height: Int,
): Bitmap {
    // async decodes an image to a bitmap on a special web worker. doesn't block UI thread :)
    val webBitmap = decodeBytesToBitmap(bytes, width, height)

    suspendAwaitSkiko()
    val colorInfo = ColorInfo(
        ColorType.RGBA_8888,
        ColorAlphaType.UNPREMUL,
        ColorSpace.sRGB,
    )
    val imageInfo = ImageInfo(colorInfo, width, height)
    val bitmap = Bitmap()
    try {
        if (!bitmap.installPixelsFromArrayBuffer(imageInfo, webBitmap, imageInfo.minRowBytes)) {
            error("Failed to install pixels from ArrayBuffer.")
        }
        bitmap.setImmutable()
        return bitmap
    } catch (throwable: Throwable) {
        bitmap.close()
        throw throwable
    }
}

@OptIn(InternalSkikoApi::class)
internal suspend fun suspendAwaitSkiko(): JsAny = suspendCancellableCoroutine { cont ->
    awaitSkiko.then(
        onFulfilled = { cont.resume(it); null },
        onRejected = { cont.resumeWithException(it.asJsException()); null },
    )
}

@OptIn(ExperimentalUuidApi::class)
private suspend fun decodeBytesToBitmap(
    bytes: ByteArray,
    width: Int,
    height: Int,
): ArrayBuffer = suspendCancellableCoroutine { continuation ->
    val id = Uuid.random().toString()
    pendingRequests[id] = continuation
    continuation.invokeOnCancellation {
        if (pendingRequests.remove(id) != null) {
            worker.postMessage(WebWorkerCancelRequest(id))
        }
    }

    val buffer = bytes.toInt8Array().buffer
    val transfer = JsArray<JsAny>().apply { set(0, buffer) }
    try {
        worker.postMessage(WebWorkerRequest(id, buffer, width, height), transfer)
    } catch (throwable: Throwable) {
        pendingRequests.remove(id)
        if (continuation.isActive) {
            continuation.resumeWithException(throwable)
        }
    }
}

private fun WebWorkerRequest(
    id: String,
    buffer: ArrayBuffer,
    width: Int,
    height: Int,
): JsAny = js("({ kind: 'decode', id: id, data: buffer, w: width, h: height })")

private fun WebWorkerCancelRequest(id: String): JsAny = js("({ kind: 'cancel', id: id })")

internal external interface WebWorkerMessage : JsAny {
    val id: String
    val kind: String
}

internal external interface WebWorkerResponse : WebWorkerMessage {
    val buffer: ArrayBuffer
}

internal external interface WebWorkerError : WebWorkerMessage {
    val message: String
}
