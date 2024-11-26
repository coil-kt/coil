import coil3.addAllMultiplatformTargets
import coil3.androidLibrary
import coil3.skikoAwtRuntimeDependency

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
}

addAllMultiplatformTargets(libs.versions.skiko)
androidLibrary(name = "coil3.gif")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.core)
                implementation(libs.androidx.vectordrawable.animated)
            }
        }
        named("nonAndroidMain") {
            dependencies {
                implementation(projects.coilComposeCore)
                implementation(libs.skiko)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.common)
            }
        }
        jvmTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(skikoAwtRuntimeDependency(libs.versions.skiko.get()))
            }
        }
        androidUnitTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.jvm)
            }
        }
        androidInstrumentedTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.android)
            }
        }
    }
}
