package coil3.util

import android.app.Application

class TestApplication : Application() {
    val activityLifecycleCallbacks = mutableSetOf<ActivityLifecycleCallbacks>()

    override fun registerActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks) {
        super.registerActivityLifecycleCallbacks(callback)
        activityLifecycleCallbacks += callback
    }

    override fun unregisterActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks) {
        super.unregisterActivityLifecycleCallbacks(callback)
        activityLifecycleCallbacks -= callback
    }
}
