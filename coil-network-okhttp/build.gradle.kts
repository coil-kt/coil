import coil3.applyCoilHierarchyTemplate
import coil3.multiplatformAndroidLibrary

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
}

multiplatformAndroidLibrary(name = "coil3.network.okhttp")

kotlin {
    applyCoilHierarchyTemplate()

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
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.bundles.test.jvm)
            }
        }
        getByName("androidDeviceTest") {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.android)
                implementation(libs.okhttp.mockwebserver)
            }
        }
    }
}
