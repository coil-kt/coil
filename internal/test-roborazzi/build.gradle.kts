import coil3.addAllMultiplatformTargets
import coil3.androidLibrary

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.github.takahirom.roborazzi")
}

addAllMultiplatformTargets(libs.versions.skiko)
androidLibrary(name = "coil3.test.roborazzi")

kotlin {
    jvm {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(projects.coilCore)
            implementation(projects.coilComposeCore)
            implementation(projects.coilTest)
            implementation(projects.internal.testUtils)
        }
        androidUnitTest.dependencies {
            implementation(libs.roborazzi.compose)
            implementation(libs.roborazzi.core)
            implementation(libs.roborazzi.junit)
        }
        named("jvmCommonTest").dependencies {
            implementation(libs.bundles.test.jvm)
            implementation(compose.desktop.uiTestJUnit4)
        }
        jvmTest.dependencies {
            implementation(libs.roborazzi.compose.desktop)
            implementation(compose.desktop.currentOs)
        }
    }
}

roborazzi {
    // RoborazziOptions.RecordOptions.outputDirectoryPath and roborazzi.output.dir seems to be ignored on desktop.
    // To workaround that this is used.
    outputDir = layout.projectDirectory.dir("src/jvmTest/snapshots/images")
}
