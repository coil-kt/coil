package coil.compose

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.StrictMode

class TestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 'MockWebServer.url()' does a network check internally and triggers strict mode.
        // To work around that in the tests, we allow network on main thread.
        registerActivityLifecycleCallbacks(
            object : DefaultActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedState: Bundle?) {
                    val threadPolicy = StrictMode.ThreadPolicy.Builder()
                        .detectAll()
                        .permitNetwork()
                        .build()
                    StrictMode.setThreadPolicy(threadPolicy)
                }
            }
        )
    }

    private interface DefaultActivityLifecycleCallbacks : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, savedState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }
}
