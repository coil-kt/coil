package coil3.compose.internal

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import coil3.test.utils.ComposeTestActivity
import coil3.util.Unconfined
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test

class ForwardingUnconfinedCoroutineDispatcherAndroidTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComposeTestActivity>()

    @Test
    fun scopeDoesNotDispatch() {
        composeTestRule.setContent {
            val scope = rememberForwardingUnconfinedCoroutineScope()
            (scope.coroutineContext.dispatcher as Unconfined).unconfined = true

            LaunchedEffect(Unit) {
                var immediate = false
                scope.launch {
                    immediate = true
                }
                assertTrue { immediate }
            }
        }
    }
    @Test
    fun scopeDoesDispatch() {
        composeTestRule.setContent {
            val scope = rememberForwardingUnconfinedCoroutineScope()
            (scope.coroutineContext.dispatcher as Unconfined).unconfined = false

            LaunchedEffect(Unit) {
                var immediate = false
                scope.launch {
                    immediate = true
                }
                assertFalse { immediate }
            }
        }
    }
}
