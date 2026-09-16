# PhotonicEngine — Minecraft 26.2 fork

Photonics is free and open-source software, and can be downloaded from
https://modrinth.com/mod/photonics (Modrinth), or
https://github.com/Redi2Go/PhotonicEngine (GitHub). Anyone can modify and
distribute it under the terms of the GNU Lesser General Public License,
version 3.

---

This is a community fork of [Photonics](https://github.com/Redi2Go/PhotonicEngine)
by **Redi2Go** and **Essentuan**, a voxel ray-tracing extension for
[Iris](https://irisshaders.dev/). All of the rendering work is theirs.

The fork exists for one reason: to add **Minecraft 26.2** support. Upstream
targets 1.21.11, and 26.2 changed enough of Minecraft's rendering internals
that the port is substantial rather than a version bump. Everything else is
kept as close to upstream as possible so changes can flow back.

## What the 26.2 port changes

Minecraft 26.2 ships deobfuscated and rewrote the two systems Photonics'
block meshing was built on.

- **Block geometry.** `ModelBlockRenderer.tesselateBlock` now emits whole
  `BakedQuad`s into a `BlockQuadOutput` instead of streaming vertices into a
  `VertexConsumer`, takes a `BlockStateModel` rather than a parts list, and
  makes ambient occlusion and culling constructor flags.
- **Block entities.** `MultiBufferSource` is gone and `FeatureRenderDispatcher`
  builds its own buffers, so geometry is captured at
  `RenderTypeFeatureRenderer#getVertexBuilder` instead — which also lets the
  mod share the game's dispatcher rather than construct a second one.
- **Shader storage buffers.** Sodium 0.9 dropped its own `GlProgram` in favour
  of Blaze3D's pipeline system, so the SSBO binding moved to
  `GlCommandEncoder#trySetup`.
- **Build.** Fabric Loom 1.18.1 on Gradle 9.7.1 and Java 25, with identity
  mappings, because 26.2 has no intermediary namespace.

Full detail is in the commit history.

## Status

The 26.2 module compiles, produces a jar in the official namespace, and loads
in a clean non-development instance with every mixin applying. Shaderpack
integration is still being worked through. **It is not ready for general use.**

## Licence

Photonics is licensed under the [GNU Lesser General Public License, version 3](LICENSE.md),
with [additional terms](LICENSE-ADDITIONAL-TERMS.md) permitted under section 7
of the GPLv3. Those additional terms require the notice at the top of this file
to appear before any download link; please keep it if you redistribute.
