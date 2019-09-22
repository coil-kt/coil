import coil.Library
import coil.compileSdk
import coil.targetSdk
import coil.versionCode
import coil.versionName
import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdkVersion(project.compileSdk)
    defaultConfig {
        applicationId = "coil.sample"
        minSdkVersion(16)
        targetSdkVersion(project.targetSdk)
        versionCode = project.versionCode
        versionName = project.versionName
        multiDexEnabled = true
        resConfigs("en")
        vectorDrawables.useSupportLibrary = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("shrinker-rules.pro", "shrinker-rules-android.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":coil-default"))
    implementation(project(":coil-gif"))
    implementation(project(":coil-svg"))

    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))

    implementation(Library.ANDROIDX_APPCOMPAT)
    implementation(Library.ANDROIDX_CONSTRAINT_LAYOUT)
    implementation(Library.ANDROIDX_CORE)
    implementation(Library.ANDROIDX_LIFECYCLE_EXTENSIONS)
    implementation(Library.ANDROIDX_LIFECYCLE_LIVE_DATA)
    implementation(Library.ANDROIDX_LIFECYCLE_VIEW_MODEL)
    implementation(Library.ANDROIDX_MULTIDEX)
    implementation(Library.ANDROIDX_RECYCLER_VIEW)

    implementation(Library.MATERIAL)
}
