plugins {
    `kotlin-dsl`
}

kotlin {
    // Was 22. Minecraft 26.2 requires Loom 1.18+ (earlier Loom cannot remap access wideners from
    // the 'official' namespace, which Iris and Sodium's 26.2 builds declare), and Loom 1.18.1 is
    // compiled for Java 25 -- a Java 22 toolchain cannot resolve it at all.
    //
    // This only affects the convention plugins; each version module still sets its own
    // `javaVersion` (1.21.11 stays on 21).
    jvmToolchain(25)
}

repositories {
    maven("https://maven.fabricmc.net") {
        name = "Fabric"
    }

    maven("https://maven.minecraftforge.net/") {
        name = "Forge"
    }

    maven("https://maven.architectury.dev/") {
        name = "Architectury"
    }

    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"

        content {
            includeGroup("maven.modrinth")
        }
    }

    google()
    mavenCentral()

    gradlePluginPortal()
}

dependencies {
    implementation(plugin(sharedLibs.plugins.idea.gradle))
    implementation(plugin(sharedLibs.plugins.shadow))

    implementation(plugin(mcLibs.plugins.loom.gradle))
}

fun plugin(plugin: Provider<PluginDependency>): Provider<String> =
    plugin.map {
        val pluginId = it.pluginId
        val version = it.version

        "$pluginId:$pluginId.gradle.plugin:$version"
    }
