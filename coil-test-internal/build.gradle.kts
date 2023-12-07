import coil3.addAllMultiplatformTargets
import coil3.androidLibrary
import coil3.nonAndroidMain

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
}

addAllMultiplatformTargets()
androidLibrary(name = "coil3.test.internal")

kotlin {
    nonAndroidMain()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
                api(libs.coroutines.test)
                api(libs.okio.fakefilesystem)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test.common)
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
