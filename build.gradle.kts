import coil.by
import coil.groupId
import coil.versionName
import kotlinx.validation.ApiValidationExtension
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import java.net.URL

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath(libs.plugin.android)
        classpath(libs.plugin.kotlin)
        classpath(libs.plugin.dokka)
        classpath(libs.plugin.mavenpublish)
        classpath(libs.plugin.binarycompatibility)
        classpath(libs.plugin.ktlint)
    }
}

apply(plugin = "binary-compatibility-validator")

extensions.configure<ApiValidationExtension> {
    ignoredProjects += arrayOf(
        "coil-sample-common",
        "coil-sample-compose",
        "coil-sample-view",
        "coil-test"
    )
}

apply(plugin = "org.jetbrains.dokka")

tasks.withType<DokkaMultiModuleTask>().configureEach {
    outputDirectory by file("$rootDir/docs/api")
    removeChildTasks(listOf(
        project(":coil-sample-common"),
        project(":coil-sample-compose"),
        project(":coil-sample-view"),
        project(":coil-test")
    ))
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    group = project.groupId
    version = project.versionName

    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<KtlintExtension> {
        version by "0.43.2"
        disabledRules by setOf("indent", "max-line-length", "parameter-list-wrapping")
    }

    tasks.withType<DokkaTaskPartial>().configureEach {
        dokkaSourceSets.configureEach {
            jdkVersion by 8
            skipDeprecated by true
            suppressInheritedMembers by true

            externalDocumentationLink {
                url by URL("https://developer.android.com/reference/")
            }
            externalDocumentationLink {
                url by URL("https://kotlin.github.io/kotlinx.coroutines/")
            }
            externalDocumentationLink {
                url by URL("https://square.github.io/okhttp/4.x/okhttp/")
            }
            externalDocumentationLink {
                url by URL("https://square.github.io/okio/3.x/okio/")
                packageListUrl by URL("https://square.github.io/okio/3.x/okio/okio/package-list")
            }
        }
    }

    // Uninstall test APKs after running instrumentation tests.
    tasks.whenTaskAdded {
        if (name == "connectedDebugAndroidTest") {
            finalizedBy("uninstallDebugAndroidTest")
        }
    }
}
