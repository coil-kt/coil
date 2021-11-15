package coil.util

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.ActivityAction
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Launch [TestActivity] and invoke [action]. */
fun withTestActivity(action: ActivityAction<TestActivity>) {
    launchActivity<TestActivity>().use { scenario ->
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity(action)
    }
}

/**
 * Get a reference to the [ActivityScenario]'s [Activity].
 *
 * NOTE: [ActivityScenario.onActivity] explicitly recommends against holding a
 * reference to the [Activity] outside of its scope. However, it should be safe
 * as long we use [ActivityScenarioRule].
 */
val <T : Activity> ActivityScenario<T>.activity: T
    get() {
        lateinit var activity: T
        runBlocking(Dispatchers.Main.immediate) {
            // onActivity is executed synchronously when called from the main thread.
            onActivity { activity = it }
        }
        return activity
    }
