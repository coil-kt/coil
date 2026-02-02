import coil3.addAllMultiplatformTargets
import coil3.androidOnlyLibrary

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
    id("dev.drewhamilton.poko")
}

addAllMultiplatformTargets(libs.versions.skiko)
androidOnlyLibrary(name = "coil3.test.utils")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
                api(projects.coilNetworkCore)
                api(libs.bundles.test.common)
                api(libs.coroutines.test)
                api(libs.kotlinx.datetime)
            }
        }
        androidMain {
            dependencies {
                api(libs.androidx.activity)
                api(libs.androidx.appcompat.resources)
                api(libs.androidx.core)
                api(libs.androidx.test.core)
                api(libs.androidx.test.junit)
                compileOnly(libs.robolectric)
            }
        }
        named("jvmCommonMain") {
            dependencies {
                api(libs.kotlin.test.junit)
                api(libs.junit)
            }
        }
    }
}
