import coil3.addAllMultiplatformTargets
import coil3.androidInstrumentedTest
import coil3.androidUnitTest
import coil3.nonAndroidMain
import coil3.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
    id("org.jetbrains.compose")
}

addAllMultiplatformTargets()
setupLibraryModule(name = "coil3.compose.core")

compose {
    kotlinCompilerPlugin = libs.jetbrains.compose.compiler.get().toString()
}

kotlin {
    nonAndroidMain()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
                api(compose.foundation)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.google.drawablepainter)
            }
        }
        androidUnitTest {
            dependencies {
                implementation(projects.coilTestInternal)
                implementation(libs.bundles.test.jvm)
            }
        }
        androidInstrumentedTest {
            dependencies {
                implementation(projects.coilTestInternal)
                implementation(libs.bundles.test.android)
                implementation(libs.androidx.compose.ui.test)
            }
        }
    }
}
