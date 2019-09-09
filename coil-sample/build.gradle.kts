import coil.Library
import coil.Library.Kotlin.Coroutines.android
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

    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))

    implementation(Library.AndroidX.appCompat)
    implementation(Library.AndroidX.constraintLayout)
    implementation(Library.AndroidX.coreKtx)
    implementation(Library.AndroidX.LifeCycle.extensions)
    implementation(Library.AndroidX.LifeCycle.liveData)
    implementation(Library.AndroidX.LifeCycle.viewModel)
    implementation(Library.AndroidX.multiDex)
    implementation(Library.AndroidX.recyclerView)

    implementation(Library.AndroidX.Material.material)
}
