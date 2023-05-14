import coil.setupAppModule

plugins {
    id("com.android.application")
    id("kotlin-android")
}

setupAppModule(namespace = "sample.compose") {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                "../coil-sample-common/shrinker-rules.pro",
                "../coil-sample-common/shrinker-rules-android.pro",
            )
            signingConfig = signingConfigs["debug"]
        }
        create("benchmark") {
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    implementation(projects.coilSampleCommon)
    implementation(projects.coilComposeSingleton)
    implementation(projects.coilGif)
    implementation(projects.coilSvg)
    implementation(projects.coilVideo)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.material)
}
