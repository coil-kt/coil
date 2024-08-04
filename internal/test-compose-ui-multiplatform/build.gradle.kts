import coil3.addAllMultiplatformTargets
import coil3.androidLibrary
import coil3.skikoAwtRuntimeDependency
import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

addAllMultiplatformTargets()
androidLibrary(name = "coil3.test.composeuimultiplatform")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.coil)
                implementation(projects.coilComposeCore)
                implementation(compose.components.resources)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.common)
                @OptIn(ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
        jvmTest {
            dependencies {
                implementation(skikoAwtRuntimeDependency(libs.versions.skiko.get()))
            }
        }
    }
}
