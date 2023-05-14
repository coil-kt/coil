import coil.by
import coil.groupId
import coil.privateModules
import coil.versionName
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessExtensionPredeclare
import java.net.URL
import kotlinx.validation.ApiValidationExtension
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradlePlugin.android)
        classpath(libs.gradlePlugin.kotlin)
        classpath(libs.gradlePlugin.mavenPublish)
        classpath(libs.gradlePlugin.paparazzi)
        classpath(libs.gradlePlugin.roborazzi)
    }
}

plugins {
    alias(libs.plugins.binaryCompatibility)
    alias(libs.plugins.dokka)
    alias(libs.plugins.spotless)
}

extensions.configure<ApiValidationExtension> {
    ignoredProjects += privateModules
}

tasks.withType<DokkaMultiModuleTask>().configureEach {
    outputDirectory by file("$rootDir/docs/api")
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    // Necessary to publish to Maven.
    group = groupId
    version = versionName

    tasks.withType<DokkaTaskPartial>().configureEach {
        dokkaSourceSets.configureEach {
            jdkVersion by 8
            failOnWarning by true
            skipDeprecated by true
            suppressInheritedMembers by true

            externalDocumentationLink {
                url by URL("https://developer.android.com/reference/")
            }
            externalDocumentationLink {
                url by URL("https://kotlinlang.org/api/kotlinx.coroutines/")
            }
            externalDocumentationLink {
                url by URL("https://square.github.io/okhttp/4.x/")
                packageListUrl by URL("https://colinwhite.me/okhttp3-package-list") // https://github.com/square/okhttp/issues/7338
            }
            externalDocumentationLink {
                url by URL("https://square.github.io/okio/3.x/okio/")
                packageListUrl by URL("https://square.github.io/okio/3.x/okio/okio/package-list")
            }
        }
    }

    dependencies {
        modules {
            module("org.jetbrains.kotlin:kotlin-stdlib-jdk7") {
                replacedBy("org.jetbrains.kotlin:kotlin-stdlib")
            }
            module("org.jetbrains.kotlin:kotlin-stdlib-jdk8") {
                replacedBy("org.jetbrains.kotlin:kotlin-stdlib")
            }
        }
    }

    // Uninstall test APKs after running instrumentation tests.
    tasks.whenTaskAdded {
        if (name == "connectedDebugAndroidTest") {
            finalizedBy("uninstallDebugAndroidTest")
        }
    }

    apply(plugin = "com.diffplug.spotless")

    val configureSpotless: SpotlessExtension.() -> Unit = {
        kotlin {
            target("**/*.kt", "**/*.kts")
            ktlint(libs.ktlint.get().version)
            endWithNewline()
            indentWithSpaces()
            trimTrailingWhitespace()
        }
    }

    if (project === rootProject) {
        spotless { predeclareDeps() }
        extensions.configure<SpotlessExtensionPredeclare>(configureSpotless)
    } else {
        extensions.configure(configureSpotless)
    }
}
