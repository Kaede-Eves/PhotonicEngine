rootProject.name = "buildSrc"

// buildSrc pins jvmToolchain(22). Rather than change that pin -- which would diverge from upstream
// for every version module, not just 26.2 -- let Gradle provision the JDK it asks for.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}


dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    // Reuse the version catalog from the main build.
    versionCatalog("sharedLibs", "../modules/shared_libs.toml")
    versionCatalog("mcLibs", "../modules/versions/mc_libs.toml")
}

















fun DependencyResolutionManagement.versionCatalog(name: String, path: String) {
    versionCatalogs {
        create(name) {
            from(files(path))
        }
    }
}