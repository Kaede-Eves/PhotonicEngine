package at.redi2go.photonics.common.meshing.impl;

import at.redi2go.photonics.api.mc.Id;
import at.redi2go.photonics.common.mixins.meshing.OuterWrappedRenderTypeAccessor;
import at.redi2go.photonics.common.mixins.meshing.RenderSetupAccessor;
import at.redi2go.photonics.common.mixins.meshing.RenderTypeAccessor;
import at.redi2go.photonics.core.rendering.world.bakery.BlockBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.irisshaders.iris.layer.OuterWrappedRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Redirects block entity geometry into a {@link BlockBuilder} while a block is being meshed.
 *
 * <p>Up to 1.21.11 this was a {@code MultiBufferSource.BufferSource} subclass handed to
 * {@code FeatureRenderDispatcher}'s constructor: block entity renderers asked it for a buffer and
 * got the block builder back. Minecraft 26.2 deleted {@code MultiBufferSource} and the dispatcher
 * now builds its own buffers from {@code RenderBuffers}, so there is no longer anything to hand in.
 *
 * <p>The equivalent funnel in 26.2 is {@code RenderTypeFeatureRenderer#getVertexBuilder}, which
 * every render-type-based feature renderer calls to obtain its vertex sink. This class holds the
 * capture target for {@code RenderTypeFeatureRendererMixin} to consult, and keeps the render-type
 * to atlas resolution that the old buffer source did.
 *
 * <p>Meshing is single threaded per block and runs entirely inside
 * {@code McBlockRenderer#submitBlockEntity}, so a thread local target is enough.
 */
public final class BlockBuilderCapture {
    private static final ThreadLocal<BlockBuilder> TARGET = new ThreadLocal<>();

    private BlockBuilderCapture() {
    }

    public static void begin(BlockBuilder blockBuilder) {
        TARGET.set(blockBuilder);
    }

    public static void end() {
        TARGET.remove();
    }

    public static boolean isCapturing() {
        return TARGET.get() != null;
    }

    /**
     * Resolves the vertex sink for a render type. Only valid while {@link #isCapturing()}.
     */
    public static VertexConsumer capture(RenderType renderType) {
        BlockBuilder blockBuilder = TARGET.get();

        if (renderType instanceof OuterWrappedRenderType wrapped)
            renderType = ((OuterWrappedRenderTypeAccessor) wrapped).getWrapped();

        RenderSetup renderSetup = ((RenderTypeAccessor) renderType).getState();
        var textures = ((RenderSetupAccessor) (Object) renderSetup).getTextureSetup();
        var sampler0 = textures.get("Sampler0");

        // A render type with no albedo texture contributes nothing a voxelizer can use.
        if (sampler0 == null) return EmptyVertexConsumer.INSTANCE;

        blockBuilder.useAtlas((Id) (Object) sampler0.getLocation());

        return (VertexConsumer) blockBuilder;
    }
}
