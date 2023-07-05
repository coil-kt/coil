import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("app.cash.paparazzi")
}

setupLibraryModule(name = "coil.test.paparazzi") {
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidx.compose.compiler.get()
    }
}

dependencies {
    api(projects.coilBase)

    implementation(projects.coilComposeBase)
    implementation(projects.coilTest)

    testImplementation(projects.coilTestInternal)
    testImplementation(libs.bundles.test.jvm)
}

// https://github.com/diffplug/spotless/issues/1572
afterEvaluate {
    tasks {
        named("spotlessKotlin").configure {
            dependsOn(named("testDebugUnitTest"))
            dependsOn(named("testReleaseUnitTest"))
        }
    }
}
