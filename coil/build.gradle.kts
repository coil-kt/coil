import coil3.addAllMultiplatformTargets
import coil3.multiplatformAndroidLibrary

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
}

addAllMultiplatformTargets(libs.versions.skiko)
multiplatformAndroidLibrary(name = "coil3.singleton")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.common)
            }
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.jvm)
            }
        }
        getByName("androidDeviceTest") {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.android)
            }
        }
    }
}
