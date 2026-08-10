@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.decode

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlinx.coroutines.await

internal actual suspend fun awaitSkiko(): JsAny = awaitSkiko.await()
