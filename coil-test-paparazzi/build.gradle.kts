import coil3.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("app.cash.paparazzi")
    id("org.jetbrains.compose")
}

setupLibraryModule(name = "coil3.test.paparazzi")

compose {
    kotlinCompilerPlugin = libs.jetbrains.compose.compiler.get().toString()
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
