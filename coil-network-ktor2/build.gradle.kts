import coil3.addAllMultiplatformTargets
import coil3.androidLibrary
import coil3.androidUnitTest

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
}

addAllMultiplatformTargets(enableWasm = false)
androidLibrary(name = "coil3.network.ktor2")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
                api(projects.coilNetworkCore)
                api(libs.ktor2.core)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.common)
                implementation(libs.ktor2.mock)
            }
        }
        androidUnitTest {
            dependencies {
                implementation(libs.bundles.test.jvm)
            }
        }
    }
}
