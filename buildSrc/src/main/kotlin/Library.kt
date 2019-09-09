@file:Suppress("unused")

package coil

object Library {
    object AndroidX {
        const val annotation = "androidx.annotation:annotation:1.1.0"
        const val appCompat = "androidx.appcompat:appcompat:1.1.0"
        const val appCompatResources = "androidx.appcompat:appcompat-resources:1.1.0"
        const val constraintLayout = "androidx.constraintlayout:constraintlayout:1.1.3"
        const val recyclerView = "androidx.recyclerview:recyclerview:1.0.0"
        const val multiDex = "androidx.multidex:multidex:2.0.1"
        const val exifInterface = "androidx.exifinterface:exifinterface:1.0.0"
        const val collectionKtx = "androidx.collection:collection-ktx:1.1.0"
        const val coreKtx = "androidx.core:core-ktx:1.1.0"

        object LifeCycle {
            private const val version = "2.1.0"
            const val common = "androidx.lifecycle:lifecycle-common-java8:$version"
            const val extensions = "androidx.lifecycle:lifecycle-extensions:$version"
            const val liveData = "androidx.lifecycle:lifecycle-livedata:$version"
            const val viewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:$version"
        }

        object Material {
            private const val version = "1.0.0"
            const val material = "com.google.android.material:material:$version"
        }

        object Test {
            const val core = "androidx.test:core-ktx:1.2.0"
            const val junit = "androidx.test.ext:junit-ktx:1.1.1"
            const val rules = "androidx.test:rules:1.2.0"
            const val runner = "androidx.test:runner:1.2.0"
        }
    }

    object Kotlin {
        object Coroutines {
            private const val version = "1.3.0"
            const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
            const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
        }
    }

    object Square {
        object OkHttp {
            private const val version = "3.12.4"
            const val okHttp = "com.squareup.okhttp3:okhttp:$version"
            const val mockWebServer = "com.squareup.okhttp3:mockwebserver:$version"
        }

        object OkIo {
            private const val version = "2.4.0"
            const val okIo = "com.squareup.okio:okio:$version"
        }
    }

    object Robolectric {
        private const val version = "4.3"
        const val robolectric = "org.robolectric:robolectric:$version"
    }

    object Other {
        const val androidSvg = "com.caverock:androidsvg-aar:1.4"
    }
}
