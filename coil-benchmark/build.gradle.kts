import coil.setupTestModule
import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
    id("com.android.test")
    id("kotlin-android")
}

setupTestModule(name = "coil.benchmark", config = true) {
    val targetProject = System.getProperty("project", "view")
    defaultConfig {
        minSdk = 23
        buildConfigField("String", "PROJECT", "\"$targetProject\"")
    }
    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += listOf("release")
        }
    }
    testOptions {
        managedDevices {
            devices {
                create<ManagedVirtualDevice>("pixel6Api31") {
                    device = "Pixel 6"
                    apiLevel = 31
                    systemImageSource = "aosp"
                }
            }
        }
    }
    targetProjectPath = ":coil-sample-$targetProject"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.androidx.test.espresso)
    implementation(libs.androidx.test.junit)
    implementation(libs.androidx.test.uiautomator)
}

androidComponents {
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmark"
    }
}
