import coil.publicModules
import coil.setupPublishing
import com.vanniktech.maven.publish.JavaPlatform

plugins {
    id("java-platform")
    id("com.vanniktech.maven.publish.base")
}

setupPublishing {
    configure(JavaPlatform())
}

dependencies {
    constraints {
        for (subproject in rootProject.subprojects) {
            if (subproject.name in publicModules) {
                api(subproject)
            }
        }
    }
}
