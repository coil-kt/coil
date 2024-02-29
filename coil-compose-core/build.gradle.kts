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
    id("androidx.baselineprofile")
}

addAllMultiplatformTargets()
androidLibrary(name = "coil3.compose.core")

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
                implementation(compose.desktop.uiTestJUnit4)
            }
        }
    }
}

baselineProfile {
    mergeIntoMain = true
    saveInSrc = true
    baselineProfileOutputDir = "."
    filter {
        include("coil3.compose.**")
    }
}

dependencies {
    baselineProfile(projects.internal.benchmark)
}
