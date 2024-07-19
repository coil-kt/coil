import coil3.addAllMultiplatformTargets
import coil3.androidLibrary
import coil3.androidUnitTest

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
}

addAllMultiplatformTargets()
androidLibrary(name = "coil3.network.ktor3")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
                api(projects.coilNetworkCore)
                api(libs.ktor3.core)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.common)
                implementation(libs.ktor3.mock)
            }
        }
        androidUnitTest {
            dependencies {
                implementation(libs.bundles.test.jvm)
            }
        }
    }
}
