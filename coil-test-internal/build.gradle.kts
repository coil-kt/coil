import coil3.addAllMultiplatformTargets
import coil3.nonAndroidMain
import coil3.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
}

addAllMultiplatformTargets()
setupLibraryModule(name = "coil3.test.internal")

kotlin {
    nonAndroidMain()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilBase)
                api(libs.coroutines.test)
                api(libs.okio.fakefilesystem)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
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
                api(libs.robolectric)
            }
        }
    }
}
