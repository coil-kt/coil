import coil3.addAllMultiplatformTargets
import coil3.multiplatformAndroidLibrary
import coil3.skikoAwtRuntimeDependency

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
}

addAllMultiplatformTargets(
    skikoVersion = libs.versions.skiko,
    enableJs = false,
    enableWasm = false,
)
multiplatformAndroidLibrary(name = "coil3.gif")

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
                implementation(libs.skiko)
                implementation(libs.compose.runtime)
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
                implementation(skikoAwtRuntimeDependency())
            }
        }
        getByName("jvmTest").resources.apply {
            srcDir(project(":internal:test-utils").projectDir.resolve("src/androidMain/assets"))
            include(
                "animated_3loops.gif",
                "animated_infinite.gif",
                "animated.webp",
                "frame*.png",
                "no_frame_delay.gif",
                "static.webp",
            )
        }
        getByName("androidHostTest") {
            kotlin.srcDir("src/androidUnitTest/kotlin")
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.jvm)
            }
        }
        getByName("androidDeviceTest") {
            kotlin.srcDir("src/androidInstrumentedTest/kotlin")
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.android)
            }
        }
    }
}
