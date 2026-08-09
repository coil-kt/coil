@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.decode

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlinx.coroutines.await

@JsModule("./js-reexport-symbols.mjs")
@JsNonModule
private external val skikoModule: SkikoModule

private external interface SkikoModule {
    val api: SkikoApi
}

private external interface SkikoApi {
    val awaitSkiko: Promise<JsAny>
}

internal actual suspend fun awaitSkiko(): JsAny = skikoModule.api.awaitSkiko.await()
