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

## The disabled-Photonics frame rate collapse

Symptom: with a pack that disables Photonics, the client ran at 4 fps. With
Photonics enabled and ReSTIR selected it ran at 82-118 fps. Same pack, same
settings.

Cause: Iris calls `preparePipeline` once per frame. `PipelineManagerMixin`
guarded its setup work with `IrisManager.hasPipeline()`. That is true once a
pipeline exists, so it works while Photonics is enabled -- but a pack that sets
`photonics.enabled=false` makes `createPipeline` return null, no pipeline ever
exists, and the guard never trips. Every frame re-read `ph_lights.json` from the
shaderpack, rebuilt the whole light configuration and registered another light
provider.

The fix tracks whether setup has *run*, not whether it *produced* anything, and
resets that when Iris destroys its pipeline. This is upstream code in
`modules/core`, so the 1.21.11 module has the same bug.

### How it was found, and how it was nearly missed

Three wrong conclusions came first, all mine, and each was reached by reasoning
from code rather than measuring:

1. Blamed the shaderpack's own `COLORED_LIGHTING=256`, twice.
2. Added a startup log proving `no pipeline created`, then claimed that settled
   it. It did not: proving no pipeline was built is not proving no work was
   done. The mixins run regardless of pipeline state, which had already been
   written down as a hypothesis and then dropped.
3. Called the conclusion "measured, not inferred" while having measured a
   different proposition than the one being asserted.

What actually settled it was removing the jar from the instance entirely --
Kaede's suggestion, after twice being told the mod was not involved. One
variable, 118 fps versus 4.

**Lesson: "component X reports doing nothing" is not evidence that component X
costs nothing.** Only removing it is. When a user says the same configuration
behaved differently on another build, that is a report about the build, not
about their configuration.

### Profiling

Java Flight Recorder settles these in one run, and the dev launcher can carry it:

    -XX:StartFlightRecording=delay=60s,duration=45s,filename=out.jfr,settings=profile

The delay matters -- without it the window closes during world load and captures
nothing useful. Then:

    jfr summary out.jfr
    jfr print --events jdk.ObjectAllocationSample --stack-depth 40 out.jfr

The summary showed 291329 GC phase events in 45 seconds, and the allocation
samples gave the exact stack, with 275 GB attributed to one call site.

## Both packs default to the renderer that does not exist

Complementary's `lib/common.glsl` has `#define PHOTONICS_LIGHTING_MODE 1`, where
1 is BASIC. BSL's bundled patch has `#define LIGHTING_MODE BASIC`. Both map to
`SharpPipeline`, which is empty, so a fresh install with either pack renders no
Photonics lighting and looks broken until the user finds the option and picks
ReSTIR. Anyone testing 0.4 will hit this first.

Confirming the active renderer is one line of the log now:

    Photonics renderer: RESTIR
    Photonics renderer: BASIC
    Photonics is disabled for this shaderpack; no pipeline created

That caught a case where the setting had fallen back to the pack default and the
report was "ray tracing stopped working".

## Open lead: framebuffers resize, pass viewports do not

Going windowed -> fullscreen leaves a hard seam with the lighting covering only a
rectangle the size of the old window. The lighting framebuffers do resize --
`FramebufferSize.Relative` reads the live window size and `SingleFramebuffer.bind`
recalculates -- so the mismatch is on the viewport side.

`PhotonicsRenderer.recalculateSizes()` overrides Iris' `CompositeRenderer` method
and pushes the framebuffer size into each pass' viewWidth/viewHeight. **Nothing
calls it.** Iris calls recalculateSizes on its own beginRenderer, prepareRenderer
and deferredRenderer fields; Photonics' renderers live in a `@Unique` phRenderers
list the pipeline mixin owns, which Iris cannot see.

Calling it from Iris' own resize path was tried and **made rendering worse** --
with ReSTIR active the result looked like Photonics was not running at all. So
the passes' viewWidth/viewHeight are evidently expected to stay at Iris' render
target size rather than the Photonics framebuffer size: Iris passes them to
shaders as uniforms, so at a render scale below 1 substituting the smaller buffer
size changes the shader math. That attempt was reverted and never committed.

Whatever the fix is, it is not "call the dead method". Worth asking upstream what
`updateSize` was meant for before trying again.
