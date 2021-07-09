package coil.compose

import android.app.Application
import android.os.Handler
import android.os.StrictMode

class TestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 'MockWebServer.url()' does a network check internally and triggers strict mode.
        // To work around that in the tests, we allow network on main thread.
        Handler(mainLooper).post {
            val threadPolicy = StrictMode.ThreadPolicy.Builder()
                .detectNetwork()
                .permitNetwork()
                .build()
            StrictMode.setThreadPolicy(threadPolicy)
        }
    }
}
