import coil3.addAllMultiplatformTargets
import coil3.androidInstrumentedTest
import coil3.androidLibrary
import coil3.androidUnitTest
import coil3.jvmCommon
import coil3.nonAndroidMain
import coil3.nonJsMain
import coil3.nonJsTest
import coil3.nonJvmCommon

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
    id("dev.drewhamilton.poko")
}

addAllMultiplatformTargets()
androidLibrary(name = "coil3.core")

kotlin {
    jvmCommon()
    nonAndroidMain()
    nonJsMain()
    nonJsTest()
    nonJvmCommon()

    sourceSets {
        commonMain {
            dependencies {
                api(libs.coroutines.core)
                api(libs.kotlin.stdlib)
                api(libs.okio)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.common)
            }
        }
        named("nonJvmCommon") {
            dependencies {
                implementation(libs.kotlinx.immutable.collections)
            }
        }
        named("nonAndroidMain") {
            dependencies {
                api(libs.skiko)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.annotation)
                implementation(libs.androidx.appcompat.resources)
                implementation(libs.androidx.core)
                implementation(libs.androidx.exifinterface)
                implementation(libs.androidx.profileinstaller)
                api(libs.androidx.lifecycle.runtime)
                api(libs.coroutines.android)
            }
        }
        androidUnitTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.jvm)
            }
        }
        androidInstrumentedTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.android)
            }
        }
        jsTest {
            dependencies {
                // https://github.com/square/okio/issues/1163
                implementation(devNpm("node-polyfill-webpack-plugin", "2.0.1"))
            }
        }
    }
}
