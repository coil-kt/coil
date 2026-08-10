@file:OptIn(ExperimentalWasmJsInterop::class)
@file:JsModule("./js-reexport-symbols.mjs")
@file:JsQualifier("api")

package coil3.decode

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

internal external val awaitSkiko: Promise<JsAny>
