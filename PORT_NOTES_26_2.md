# 26.2 port notes (0.4 fork)

## Verified API changes (checked against the jars, not guessed)

| 1.21.11 | 26.2 |
| --- | --- |
| `net.minecraft.world.level.BlockAndTintGetter` | `net.minecraft.client.renderer.block.BlockAndTintGetter` |
| `...renderer.state.LevelRenderState` | `...renderer.state.level.LevelRenderState` |
| `...renderer.block.model.BakedQuad` | `...resources.model.geometry.BakedQuad` (now a record) |
| `...resources.model.AtlasManager` | `...resources.model.sprite.AtlasManager` |
| `...renderer.block.model.BlockModelPart` | `...renderer.block.dispatch.BlockStateModelPart` |
| `com.mojang.blaze3d.textures.TextureFormat` | `com.mojang.blaze3d.GpuFormat` (`RGBA8` -> `RGBA8_UNORM`, `pixelSize()` -> `blockSize()`) |
| `BakedQuad.tintIndex()` | `BakedQuad.materialInfo().tintIndex()` |
| `BlockColors.getColor(state, getter, pos, i)` | `getTintSource(state, i).colorInWorld(state, getter, pos)` |
| `Minecraft.getMainRenderTarget()` | `Minecraft.gameRenderer.mainRenderTarget()` |
| `Minecraft.getBlockRenderer()` | gone; use `ModelManager` + `new ModelBlockRenderer(ao, cull, blockColors)` |
| `CommandEncoder.mapBuffer(buf, r, w)` | `buf.map(r, w)` |
| `LevelRenderer.renderLevel(...)` | `LevelRenderer.render(...)`, matrices + Camera -> `CameraRenderState` |
| `GpuDevice` (interface) | `GpuDevice` (wrapper class); `GlDevice implements GpuDeviceBackend` |
| `CommandEncoder` (interface) | wrapper class; `GlCommandEncoder implements CommandEncoderBackend` |
| `BufferStorage$Immutable.tryMapBufferPersistent` | gone; `canPersistentMap` is a `GlBuffer.Direct` constructor flag |
| `GlCommandEncoder.inRenderPass` | gone; render passes are objects now |
| Sodium `GlProgram.bind()` | gone; hook `GlCommandEncoder#trySetup` |
| Iris composite `RenderPass.drawIndexed` | `GlStateManager._drawElements` |
| Iris `Blaze3dRenderTargetExt.iris$bindFramebuffer` | `RenderTargetInterface.iris$bindFramebuffer` |

## Lessons

**Compiling proves nothing about mixins.** A mixin's target is a string, so a
renamed method or a changed descriptor is a crash on first launch, not a
compile error. Two audit scripts live in the scratchpad approach used here:
one checks every `@Mixin` target, `method =`, `@Accessor`, `@Invoker` and
`@Shadow` against the real jars; the other checks every `@At(target = ...)`
descriptor. Run both before launching -- it is much faster than one crash per
cycle. Known false positives: Photonics' own interfaces (not on the javap
classpath), constructors, bare nested class names, and inherited methods such
as `Enum.name()`.

**Run the control before blaming the port.** The BSL shaderpack failure looked
like a 26.2 regression. Running the same pack against the 1.21.11 module gave a
byte-identical stack trace, which made it an upstream bug in 0.4 alpha and
changed what the right fix was.

## Build

Fabric Loom 1.18.1, Gradle 9.7.1, Java 25. 26.2 has no intermediary namespace,
so the module carries identity tiny-v2 mappings and `noIntermediateMappings()`.

`org.gradle.configuration-cache=true` in gradle.properties does not survive the
Gradle 9.7.1 upgrade: `processResources` in `photonics-version.gradle.kts`
captures the Kotlin script object in its `from(...) { into(...) }` blocks, which
Gradle 9.7.1 refuses to serialise. Build with `--no-configuration-cache` until
that is restructured. Only the version modules past the first are affected,
because the `from(...)` blocks are guarded by `project.name != "common"`.
