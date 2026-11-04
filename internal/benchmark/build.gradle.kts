import coil3.MANAGED_DEVICE_NAME
import coil3.androidTest
import coil3.configureManagedDevice

plugins {
    id("com.android.test")
    id("androidx.baselineprofile.producer")
}

androidTest(name = "coil3.benchmark", config = true) {
    val targetProject = System.getProperty("project", "compose-android")
    defaultConfig {
        minSdk = 28
        buildConfigField("String", "PROJECT", "\"$targetProject\"")
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        // Enables Composition Tracing for benchmarks
        // testInstrumentationRunnerArguments["androidx.benchmark.fullTracing.enable"] = "true"
        // Enables Method tracing for benchmarks. Be aware this skews the performance results,
        // so don't use it for measuring exact timing
        // testInstrumentationRunnerArguments["androidx.benchmark.profiling.mode"] = "MethodTracing"
    }
    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs["debug"]
            matchingFallbacks += "release"
        }
    }
    configureManagedDevice(testOptions.managedDevices)
    targetProjectPath = ":samples:$targetProject"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

baselineProfile {
    managedDevices += MANAGED_DEVICE_NAME
    useConnectedDevices = false
    enableEmulatorDisplay = false
}

dependencies {
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.androidx.test.espresso)
    implementation(libs.androidx.test.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.tracing.perfetto)
    implementation(libs.androidx.tracing.perfetto.binary)
}
