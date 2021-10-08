import coil.Library
import coil.setupLibraryModule
import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.library")
    id("kotlin-android")
}

setupLibraryModule()

dependencies {
    implementation(Library.KOTLINX_COROUTINES_ANDROID)
    implementation(Library.KOTLINX_COROUTINES_TEST)

    implementation(Library.ANDROIDX_APPCOMPAT)
    implementation(Library.ANDROIDX_CORE)
    implementation(Library.ANDROIDX_LIFECYCLE_COMMON)

    implementation(Library.MATERIAL)

    implementation(Library.OKHTTP)
    implementation(Library.OKHTTP_MOCK_WEB_SERVER)

    implementation(Library.OKIO)

    implementation(Library.ANDROIDX_TEST_CORE)
    implementation(Library.ANDROIDX_TEST_JUNIT)

    implementation(Library.JUNIT)

    testImplementation(Library.JUNIT)
    testImplementation(kotlin("test-junit", KotlinCompilerVersion.VERSION))
}
