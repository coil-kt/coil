import coil3.androidLibrary
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.screenshot)
}

androidLibrary(name = "coil3.test.composescreenshot") {
    buildFeatures {
        compose = true
    }
    testOptions {
        screenshotTests {
            imageDifferenceThreshold = 0.01f
        }
    }
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

dependencies {
    api(projects.coilCore)

    implementation(projects.coilComposeCore)
    implementation(projects.coilTest)

    testImplementation(projects.internal.testUtils)
    testImplementation(libs.bundles.test.jvm)

    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}

// Compose 1.8.0 requires JVM 11.
tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
}
tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget = JvmTarget.JVM_11
}
