plugins {
    id("net.fabricmc.fabric-loom-remap")
    `photonics-fabric`
}

// 26.2 has no intermediary at all (fabric-meta reports 0.0.0), and Iris/Sodium builds for it
// declare `Fabric-Mapping-Namespace: official`. Without this, Loom tries to remap their access
// wideners from 'official' and fails expecting 'intermediary'.
loom {
    noIntermediateMappings()
}


val mainLibs = libs262

dependencies {
    // Identity mappings rather than officialMojangMappings(): see ../build.gradle.kts.
    add("mappings", files("../mappings/identity-mappings-26.2.jar"))

    // Required by sodium
    modRuntimeOnly(mainLibs.fabric.api)

    // Iris 26.2 nests these rather than exposing them on the compile classpath, so the dev
    // classpath needs them declared explicitly or its shader pipeline fails to resolve.
    implementation(mainLibs.jcpp)
    implementation(mainLibs.glsl.transformer)
    runtimeOnly(mainLibs.antlr4.runtime)
}

tasks {
    processResources {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand(
                "photonics_version" to constants.versions.photonics.get(),
                "minecraft_version" to mainLibs.versions.minecraft.get(),
                "fabric_loader_version" to mainLibs.versions.fabric.loader.get()
            )
        }
    }
}
