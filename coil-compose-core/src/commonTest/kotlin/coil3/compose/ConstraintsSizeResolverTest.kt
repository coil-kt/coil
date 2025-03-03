package coil3.compose

import androidx.compose.ui.unit.Constraints
import coil3.compose.internal.toSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest

class ConstraintsSizeResolverTest {
    @Test
    fun `resolver size is cancellable`() = runTest {
        val resolver = ConstraintsSizeResolver()

        val deferred = async(start = CoroutineStart.UNDISPATCHED) {
            resolver.size()
        }

        deferred.cancelAndJoin()
        assertEquals(true, deferred.isCompleted)
    }

    @Test
    fun `resolver size is cancellable with two suspension points`() = runTest {
        val resolver = ConstraintsSizeResolver()

        val deferred1 = async(start = CoroutineStart.UNDISPATCHED) {
            resolver.size()
        }

        val deferred2 = async(start = CoroutineStart.UNDISPATCHED) {
            resolver.size()
        }

        deferred2.cancelAndJoin()
        assertEquals(true, deferred2.isCompleted)
        assertEquals(false, deferred1.isCancelled)
        assertEquals(false, deferred1.isCompleted)

        val c = Constraints()
        resolver.setConstraints(c)
        val result = deferred1.await()
        assertEquals(c.toSize(), result)
    }

    @Test
    fun `resolver size is cancellable with many suspension points`() = runTest {
        val resolver = ConstraintsSizeResolver()

        val deferred1 = async(start = CoroutineStart.UNDISPATCHED) {
            resolver.size()
        }

        val deferred2 = async(start = CoroutineStart.UNDISPATCHED) {
            resolver.size()
        }

        val deferred3 = async(start = CoroutineStart.UNDISPATCHED) {
            resolver.size()
        }

        deferred2.cancelAndJoin()
        assertEquals(true, deferred2.isCompleted)
        assertEquals(false, deferred1.isCancelled)
        assertEquals(false, deferred3.isCancelled)

        val c = Constraints()
        resolver.setConstraints(c)
        val result1 = deferred1.await()
        val result3 = deferred3.await()
        assertEquals(c.toSize(), result1)
        assertEquals(c.toSize(), result3)
    }
}
