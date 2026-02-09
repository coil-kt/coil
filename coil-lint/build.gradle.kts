import coil3.setupPublishing
import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.vanniktech.maven.publish.base")
}

setupPublishing {
    configure(JavaLibrary(javadocJar = JavadocJar.Empty()))
}

kotlin {
    jvmToolchain(17)
}

// Lint dependencies require JVM 11+, so we must request JVM 17 compatible dependencies.
configurations.configureEach {
    if (isCanBeResolved) {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
    }
}

dependencies {
    compileOnly(libs.lint.api)
    compileOnly(libs.lint.checks)

    testImplementation(libs.lint.core)
    testImplementation(libs.lint.tests)
    testImplementation(libs.junit)
}

tasks.jar {
    manifest {
        attributes("Lint-Registry-v2" to "coil3.lint.CoilIssueRegistry")
    }
}
