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

## Upstream 0.4 gaps found while testing on 26.2

None of these are 26.2 porting problems. Each was checked against the 1.21.11
module or the shipped jars before being classified.

**Sharp / Basic lighting is not implemented.** `SharpPipeline`'s constructor
calls `super(...)` and nothing else -- the same shape as `OffPipeline` -- and
its shader directory holds one file against ReSTIR's twenty-two. `BASIC` and
`SHARP` both map to it, so selecting either is identical to selecting Off.
ReSTIR is the only working renderer in 0.4.

**BSL has no working path in 0.4.** Two independent reasons. Its bundled patch
still declares `"supportedVersions": ["0.3.5"]` and defaults to
`#define LIGHTING_MODE BASIC`, which selects the unimplemented pipeline. And the
patched `shaders.properties` never reaches Iris: Photonics patches files as they
pass through the include graph, but `ShaderPack` reads shaders.properties
straight off disk with `loadProperties(Path, String)`. Iris 1.10.5 does the same
thing and neither version module has a hook for it, so the Photonics options
cannot appear in BSL's menu on either Minecraft version. Test with
Complementary.

**Shaderpack properties are parsed after the include graph.** Iris builds the
graph before reading shader.properties, so Photonics' own shader files are read
while `IrisManager`'s properties are still empty and `getPropertiesOrThrow`
threw, which Iris reported as an unloadable pack. Reproduces byte for byte on
1.21.11. `isPhotonicsEnabled` now answers from the patcher when properties are
not parsed yet.

**The voxel heap is fixed at a size that does not fit render distance 16.** See
the commit that raised it. Two further things worth knowing before touching it:
`GlBufferHeap.close()` only closes the GPU buffer and leaves its direct
ByteBuffer to the collector, and a pipeline reload constructs the new heap
before the old one is gone -- so a reload transiently needs twice the heap in
both VRAM and native memory. Unmeasured, but it scales with whatever size is
chosen.

## Testing without round-tripping through a human

`runClient` accepts program arguments, so the dev client can enter a world on
its own:

    ./gradlew :modules:versions:mc262:fabric:runClient \
        --args='--quickPlaySingleplayer "New World"'

Both of the crashes that only appeared in-world, and the heap measurement, came
from this. Two cautions learned the hard way. Do not `taskkill /F /IM java.exe`
to free the build -- it kills whatever client the user is testing in, and from
their side it is indistinguishable from a crash. And do not push an
instrumented build to a client someone is using: raising the heap to 2 GiB to
measure demand overflowed `Math.toIntExact` and crashed every pipeline
creation, which looked like a shaderpack-switching bug for several rounds.
