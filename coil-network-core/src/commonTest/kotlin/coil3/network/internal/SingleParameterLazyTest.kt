package coil3.network.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class SingleParameterLazyTest {

    @Test
    fun getOnlyInitializesOnce() {
        val input = "input"
        val output = "output"
        var invocations = 0
        val lazy = singleParameterLazy<String, String> {
            assertEquals(input, it)
            invocations++
            output
        }
        lazy.get(input)
        lazy.get(input)
        lazy.get(input)

        assertEquals(1, invocations)
    }
}
