import coil3.addAllMultiplatformTargets
import coil3.androidInstrumentedTest
import coil3.androidLibrary
import coil3.androidUnitTest

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
    id("dev.drewhamilton.poko")
    id("org.jetbrains.compose")
}

addAllMultiplatformTargets()
androidLibrary(name = "coil3.core")

compose {
    kotlinCompilerPlugin = libs.jetbrains.compose.compiler.get().toString()
    experimental.web.application {}
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.coroutines.core)
                api(libs.kotlin.stdlib)
                api(libs.okio)
                api(compose.foundation)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.common)
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
    }
}
