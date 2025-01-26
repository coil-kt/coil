import coil3.addAllMultiplatformTargets
import coil3.androidLibrary
import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
    id("dev.drewhamilton.poko")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("androidx.baselineprofile")
}

addAllMultiplatformTargets(libs.versions.skiko)
androidLibrary(name = "coil3.compose.core") {
    dependencies {
        debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.6")
    }
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
                @OptIn(ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
        named("jvmCommonTest").dependencies {
            implementation(libs.bundles.test.jvm)
            implementation(compose.desktop.uiTestJUnit4)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
        }
        androidMain {
            dependencies {
                implementation(libs.google.drawablepainter)
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
