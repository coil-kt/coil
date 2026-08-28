@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.decode

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SkikoModuleTest {
    @Test
    fun awaitSkikoReturnsInitializedModule() = runTest {
        assertTrue(hasInitializedMemory(awaitSkiko()))
    }
}

private fun hasInitializedMemory(skikoWasm: JsAny): Boolean =
    js("skikoWasm.wasmExports.memory.buffer.byteLength > 0")
