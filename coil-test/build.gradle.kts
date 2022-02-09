import coil.setupLibraryModule
import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.library")
    id("kotlin-android")
}

setupLibraryModule()

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.test.core)
    implementation(libs.androidx.test.junit)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.test)
    implementation(libs.junit)
    implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.okhttp.mockwebserver)
    implementation(libs.okio)
    testImplementation(kotlin("test-junit", KotlinCompilerVersion.VERSION))
}
