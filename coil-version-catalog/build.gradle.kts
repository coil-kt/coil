import coil3.publicModules
import coil3.setupPublishing
import com.vanniktech.maven.publish.VersionCatalog

plugins {
    id("version-catalog")
    id("com.vanniktech.maven.publish.base")
}

catalog {
    versionCatalog {
        val version = rootProject.version.toString()
        val group = rootProject.group.toString()

        val versionAlias = version("coil", version)

        rootProject.subprojects
            .filter { subproject ->
                subproject.name in publicModules
            }.forEach { project ->
                library(project.name, group, project.name).versionRef(versionAlias)
            }
    }
}

setupPublishing {
    configure(VersionCatalog())
}
