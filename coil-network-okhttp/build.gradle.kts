import coil3.androidLibrary
import coil3.androidUnitTest
import coil3.applyCoilHierarchyTemplate

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
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
    }
}
