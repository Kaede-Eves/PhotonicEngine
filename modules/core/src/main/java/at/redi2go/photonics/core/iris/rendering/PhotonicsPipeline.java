package at.redi2go.photonics.core.iris.rendering;

import at.redi2go.photonics.api.mc.Minecraft;
import at.redi2go.photonics.core.iris.pipeline.IrisPipeline;
import at.redi2go.photonics.core.iris.pipeline.IrisRenderer;
import at.redi2go.photonics.core.iris.properties.PhotonicsProperties;
import at.redi2go.photonics.core.iris.rendering.restir.RestirProperties;
import at.redi2go.photonics.core.rendering.AbstractRenderingComponent;
import at.redi2go.photonics.core.rendering.RenderingComponent;
import at.redi2go.photonics.core.rendering.SectionManager;
import at.redi2go.photonics.core.rendering.lights.BufferLightList;
import at.redi2go.photonics.core.rendering.lights.HandheldItemSupplier;
import at.redi2go.photonics.core.rendering.world.allocator.buffer.BufferPaletteTexture;
import at.redi2go.photonics.core.rendering.world.allocator.buffer.BufferWorldAllocator;
import at.redi2go.photonics.core.rendering.world.bakery.texture.AtlasDownloader;
import at.redi2go.photonics.core.rendering.world.compiler.ChunkCompiler;
import at.redi2go.photonics.core.rendering.world.compiler.WorldCompiler;
import at.redi2go.photonics.core.rendering.world.registry.WorldRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class PhotonicsPipeline extends AbstractRenderingComponent {
    private static final int ROOT_VOXEL_DEPTH = 3;

    protected final PhotonicsProperties properties;
    private final List<IrisRenderer> renderers = new ArrayList<>();

    public PhotonicsPipeline(
            PhotonicsProperties properties,
            AtlasDownloader atlasDownloader,
            IrisPipeline pipeline,
            @Nullable RenderingComponent... components
    ) {
        super(components);
        this.properties = properties;
        registerComponent(pipeline);

        registerResource(atlasDownloader);
        var sectionManager = registerComponent(new SectionManager(Minecraft::getRenderDistance));

        // 512 MiB (the previous value) is not enough for render distance 16: measured, the voxel
        // heap passed it about a minute after world load and was still climbing at 576 MiB. Once
        // it fills, every voxel allocation fails and the ray tracer quietly loses the world --
        // lighting reverts to looking vanilla mid-session.
        //
        // This is a fixed number rather than one scaled from render distance, so it is generous at
        // short distances and would still be too small at 32. GlBufferHeap mirrors this in a direct
        // ByteBuffer sized by Math.toIntExact, which also caps any heap just under 2 GiB.
        var worldAllocator = registerComponent(new BufferWorldAllocator(1 << 30));
        var paletteTexture = registerComponent(new BufferPaletteTexture(2048, 600));

        var worldRegistry = new WorldRegistry(worldAllocator, paletteTexture, atlasDownloader);

        var builtSectionQueue = sectionManager.<ChunkCompiler.BuildResult>newTaskQueue(WorldCompiler.MAX_SECTIONS_PER_RUN << 1, true);
        var worldCompiler = registerComponent(new WorldCompiler(
                ROOT_VOXEL_DEPTH,
                worldAllocator,
                paletteTexture,
                builtSectionQueue,
                worldRegistry
        ));

        registerComponent(new ChunkCompiler(
                sectionManager,
                builtSectionQueue,
                worldRegistry
        ));

        registerComponent(
                new BufferLightList(
                        sectionManager,
                        properties.getLightListProperties().getSize(),
                        worldCompiler::origin
                )
        );
    }

    public <T extends IrisRenderer> T registerRenderer(T component) {
        if (component != null) {
            if (component instanceof RenderingComponent renderingComponent)
                registerComponent(renderingComponent);

            renderers.add(component);
        }

        return component;
    }

    public void onRender() {
        renderers.forEach(IrisRenderer::renderAll);
    }

    @FunctionalInterface
    interface Supplier<T extends PhotonicsPipeline, P> {
        T create(PhotonicsProperties phProperties, P rendererProperties, AtlasDownloader atlasDownloader, HandheldItemSupplier handheldItemSupplier, IrisPipeline irisPipeline);
    }
}
