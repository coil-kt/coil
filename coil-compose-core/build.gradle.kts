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
androidLibrary(name = "coil3.compose.core")

compose {
    kotlinCompilerPlugin = libs.jetbrains.compose.compiler.get().toString()
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
                api(compose.foundation)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.google.drawablepainter)
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
                implementation(libs.androidx.compose.ui.test)
            }
        }
    }
}
