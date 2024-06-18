import coil3.addAllMultiplatformTargets
import coil3.androidLibrary
import coil3.androidUnitTest
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.github.takahirom.roborazzi")
}

addAllMultiplatformTargets()
androidLibrary(name = "coil3.test.roborazzi")

kotlin {
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }

    sourceSets {
        val jvmCommonTest by getting
        val jvmTest by getting

        androidUnitTest.dependencies {
            implementation(libs.roborazzi.compose)
            implementation(libs.roborazzi.core)
            implementation(libs.roborazzi.junit)
        }
        commonTest.dependencies {
            api(projects.coilCore)
            implementation(projects.coilComposeCore)
            implementation(projects.coilTest)
            implementation(projects.internal.testUtils)
        }
        jvmCommonTest.dependencies {
            implementation(compose.desktop.uiTestJUnit4)
            implementation(libs.bundles.test.jvm)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.coroutines.swing)
            implementation(libs.roborazzi.compose.desktop)
        }
    }
}

roborazzi {
    // RoborazziOptions.RecordOptions.outputDirectoryPath and roborazzi.output.dir seems to be ignored on desktop.
    // To workaround that this is used.
    outputDir = layout.projectDirectory.dir("src/jvmTest/snapshots/images")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}
