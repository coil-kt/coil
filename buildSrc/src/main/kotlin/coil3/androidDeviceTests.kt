package coil3

import com.android.build.api.dsl.ManagedDevices
import com.android.build.api.dsl.ManagedVirtualDevice
import com.android.build.api.dsl.ManagedVirtualDevice.PageAlignment
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

const val MANAGED_DEVICE_NAME = "pixel10"

fun Project.configureManagedDevice(managedDevices: ManagedDevices) {
    managedDevices.allDevices.create<ManagedVirtualDevice>(MANAGED_DEVICE_NAME) {
        device = "Pixel 10"
        apiLevel = project.targetSdk
        systemImageSource = "aosp"
        pageAlignment = PageAlignment.FORCE_4KB_PAGES
    }
}

fun Project.configureAndroidDeviceTests(managedDevices: ManagedDevices, testTaskSuffix: String) {
    configureManagedDevice(managedDevices)

    val adb = extensions.getByType(AndroidComponentsExtension::class.java).sdkComponents.adb
    val devices = providers.exec {
        commandLine(adb.get().asFile, "devices")
    }.standardOutput.asText

    tasks.register("androidDeviceTest") {
        group = "verification"
        description = "Runs Android device tests on any connected device or the managed Android device if none is connected."
        dependsOn(androidDeviceTestTask(devices.get(), testTaskSuffix))
    }
}

private fun androidDeviceTestTask(devices: String, testTaskSuffix: String): String {
    val connected = devices.lineSequence().any {
        it.trim().split(Regex("\\s+")).getOrNull(1) == "device"
    }
    return (if (connected) "connected" else MANAGED_DEVICE_NAME) + testTaskSuffix
}
