val mainLibs = libs262

/*
 * Minecraft 26.2 ships DEOBFUSCATED.
 *
 * Mojang no longer publishes `client_mappings` for it, and Fabric publishes no intermediary
 * (fabric-meta reports 0.0.0), so `loom.officialMojangMappings()` -- which every other version
 * module uses -- fails outright here. Iris and Sodium builds for 26.2 declare
 * `Fabric-Mapping-Namespace: official` for the same reason.
 *
 * The workaround is an identity tiny-v2 mapping: official -> intermediary -> named, all three
 * namespaces present and every class listed. A header-only file is NOT sufficient; Loader's
 * namespace resolution breaks on one. Paired with `noIntermediateMappings()`, which stops Loom
 * looking for an intermediary that does not exist.
 *
 * This is a DEVELOPMENT-ONLY construct and must never reach a published jar -- verify any release
 * build against a clean non-dev instance in the official namespace before shipping it.
 */
photonics {
    minecraft = mainLibs.versions.minecraft.get()
    javaVersion = JavaVersion.VERSION_21

    commonDependencies {
        // NB: mappings are NOT declared here. PhotonicsCommonDependenciesScope.mappings() casts
        // the created dependency to ExternalModuleDependency, so it only accepts a module
        // coordinate -- a local file collection throws ClassCastException at plugin-apply time.
        // Each subproject adds the identity mappings itself with a raw add() instead.

        // Used by fabric (for obvious reasons) & common for mixin dependencies
        fabricLoader(mainLibs.fabric.loader)

        shadow(sharedLibs.semver)
        shadow(sharedLibs.fastutil.concurrent.wrapper) {
            isTransitive = false
        }

        runtimeOnly(mainLibs.antlr4.runtime)
        implementation(mainLibs.glsl.transformer)
        implementation(mainLibs.jcpp)

        modImplementation(mainLibs.sodium)
        modImplementation(mainLibs.iris)
    }
}
