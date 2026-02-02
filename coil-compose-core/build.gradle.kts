import coil3.addAllMultiplatformTargets
import coil3.androidOnlyLibrary
import coil3.applyJvm11OnlyToJvmTarget

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
    id("dev.drewhamilton.poko")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("androidx.baselineprofile")
}

addAllMultiplatformTargets(libs.versions.skiko, enableNativeLinux = false)
androidOnlyLibrary(name = "coil3.compose.core")

// https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html
dependencies {
    "debugImplementation"(libs.androidx.compose.ui.test.manifest)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
                api(compose.foundation)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.kotlin.test)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.google.drawablepainter)
            }
        }
        named("androidUnitTest") {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.jvm)
            }
        }
        named("androidInstrumentedTest") {
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

// Compose 1.8.0 requires JVM 11 only for the JVM target.
applyJvm11OnlyToJvmTarget()
