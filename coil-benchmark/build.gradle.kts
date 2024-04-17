import coil.setupTestModule
import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
    id("com.android.test")
    id("kotlin-android")
}

setupTestModule(name = "coil.benchmark", config = true) {
    // TODO: temporary, should pass instead
    val targetProject = "compose" //System.getProperty("project", "view")
    defaultConfig {
        minSdk = 23
        buildConfigField("String", "PROJECT", "\"$targetProject\"")
        // TODO temporary, should pass with run configuration
        testInstrumentationRunnerArguments["androidx.benchmark.fullTracing.enable"] = "true"
        // TODO just temporary
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "LOW-BATTERY"
        testInstrumentationRunnerArguments["androidx.benchmark.profiling.mode"]= "MethodTracing"
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
                create<ManagedVirtualDevice>("pixel7Api33") {
                    device = "Pixel 7"
                    apiLevel = 33
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
    implementation("androidx.tracing:tracing-perfetto:1.0.0")
    implementation("androidx.tracing:tracing-perfetto-binary:1.0.0")
}

androidComponents {
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmark"
    }
}
