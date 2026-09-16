import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask

val phConfig = parent!!.extensions.getByName<PhotonicsExtension>("photonics")

loom {
    runConfigs {
        configureEach {
            generateRunConfig = true
        }
    }
}

dependencies {
    // Was hardcoded to "com.mojang:minecraft:1.21.11". photonics-common already uses the
    // extension value; this one did not, so every version module past the first was handed
    // Minecraft 1.21.11 while carrying its own mappings -- which surfaces as
    // "Cannot remap access widener from namespace 'official'" rather than as a version mismatch.
    add("minecraft", "com.mojang:minecraft:${phConfig.minecraft.get()}")
    phConfig._dependencyBlock.orNull?.execute(PhotonicsCommonDependenciesScope(this))

    val fabricLoader = _fabricLoader

    // Fabric loader is needed on common for mixin dependency.
    // Why not just include the mixin dependency raw? I have no clue, ask architectury.
    if (fabricLoader != null) {
        add("modImplementation", fabricLoader)
    }
}

val jarName = "photonics-${project.version}-${project.name}+MC-${phConfig.minecraft.get()}"

tasks {
    named<RemapJarTask>("remapJar") {
        archiveFileName = "$jarName.jar"

        dependsOn(shadowJar)
        inputFile = shadowJar.get().archiveFile;
    }

    named<RemapSourcesJarTask>("remapSourcesJar") {
        archiveFileName = "$jarName-sources.jar"
    }
}
