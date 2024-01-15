import coil3.addAllMultiplatformTargets
import coil3.androidLibrary

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
}

addAllMultiplatformTargets()
androidLibrary(name = "coil3.test.utils")

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
                api(libs.junit)
                compileOnly(libs.robolectric)
            }
        }
    }
}
