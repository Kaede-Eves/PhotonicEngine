plugins {
    id("net.fabricmc.fabric-loom-remap")
    `photonics-common`
}

// 26.2 has no intermediary at all (fabric-meta reports 0.0.0), and Iris/Sodium builds for it
// declare `Fabric-Mapping-Namespace: official`. Without this, Loom tries to remap their access
// wideners from 'official' and fails expecting 'intermediary'.
loom {
    noIntermediateMappings()
}


dependencies {
    // Identity mappings rather than officialMojangMappings(): see ../build.gradle.kts.
    add("mappings", files("../mappings/identity-mappings-26.2.jar"))
}
