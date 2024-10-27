import coil3.androidLibrary
import coil3.applyCoilHierarchyTemplate

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
}

androidLibrary(name = "coil3.network.okhttp")

kotlin {
    applyCoilHierarchyTemplate()

    androidTarget()
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
                api(projects.coilNetworkCore)
                api(libs.okhttp.core)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.common)
                implementation(libs.okhttp.mockwebserver)
            }
        }
        androidUnitTest {
            dependencies {
                implementation(libs.bundles.test.jvm)
            }
        }
        androidInstrumentedTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.android)
                implementation(libs.okhttp.mockwebserver)
            }
        }
    }
}
