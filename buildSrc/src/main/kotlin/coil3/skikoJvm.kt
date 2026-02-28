package coil3

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

fun Project.skikoAwtRuntimeDependency(): String {
    val skikoVersion = extensions
        .getByType(VersionCatalogsExtension::class.java)
        .named("libs")
        .findVersion("skiko")
        .get()
        .requiredVersion
    return "org.jetbrains.skiko:skiko-awt-runtime-${jvmNativeTarget()}:$skikoVersion"
}

fun Project.composeDesktopCurrentOsDependency(): String {
    val composeVersion = extensions
        .getByType(VersionCatalogsExtension::class.java)
        .named("libs")
        .findVersion("jetbrains-compose")
        .get()
        .requiredVersion
    return "org.jetbrains.compose.desktop:desktop-jvm-${jvmNativeTarget()}:$composeVersion"
}

private fun jvmNativeTarget(): String {
    val osName = System.getProperty("os.name")
    val targetOs = when {
        osName == "Mac OS X" -> "macos"
        osName.startsWith("Win") -> "windows"
        osName.startsWith("Linux") -> "linux"
        else -> error("Unsupported OS: $osName")
    }

    val osArch = System.getProperty("os.arch")
    val targetArch = when (osArch) {
        "x86_64", "amd64" -> "x64"
        "aarch64" -> "arm64"
        else -> error("Unsupported arch: $osArch")
    }

    return "$targetOs-$targetArch"
}
