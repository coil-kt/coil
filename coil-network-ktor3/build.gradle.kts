import coil3.addAllMultiplatformTargets
import coil3.multiplatformAndroidLibrary

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
}

addAllMultiplatformTargets(libs.versions.skiko)
multiplatformAndroidLibrary(name = "coil3.network.ktor3")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
                api(projects.coilNetworkCore)
                api(libs.ktor3.core)
            }
        }
        named("nonJvmCommonMain") {
            dependencies {
                implementation(libs.kotlinx.io.okio)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.common)
                implementation(libs.ktor3.mock)
            }
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.bundles.test.jvm)
            }
        }
    }
}
