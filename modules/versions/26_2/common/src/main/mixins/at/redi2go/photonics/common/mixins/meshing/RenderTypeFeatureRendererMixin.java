package at.redi2go.photonics.common.mixins.meshing;

import at.redi2go.photonics.common.meshing.impl.BlockBuilderCapture;
import at.redi2go.photonics.common.meshing.impl.EmptyVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures block entity geometry while a block is being meshed.
 *
 * <p>This is the 26.2 replacement for handing {@code FeatureRenderDispatcher} a custom
 * {@code MultiBufferSource}. Every render-type-based feature renderer -- models, block models,
 * items, shadows, flames, text -- funnels through this one method to obtain its vertex sink, so it
 * is the single point at which the game's geometry can be diverted into a {@code BlockBuilder}.
 *
 * <p>Because the funnel is shared, it also serves as the suppression point for the decorative
 * features the voxelizer does not want baked into a block: shadows, flames, name tags and text get
 * a sink that discards them. Particles need no such treatment -- {@code QuadParticleFeatureRenderer}
 * does not extend {@code RenderTypeFeatureRenderer} and so never reaches this method.
 *
 * <p>Outside of meshing {@link BlockBuilderCapture} holds no target and this injection is a no-op,
 * which is what lets Photonics share the game's own dispatcher rather than construct a second one.
 */
@Mixin(RenderTypeFeatureRenderer.class)
public abstract class RenderTypeFeatureRendererMixin {
    @Inject(method = "getVertexBuilder", at = @At("HEAD"), cancellable = true)
    private void photonics$captureIntoBlockBuilder(
            RenderType renderType,
            CallbackInfoReturnable<VertexConsumer> cir
    ) {
        if (!BlockBuilderCapture.isCapturing()) return;

        // Through (Object), because javac only sees the mixin class here, not the target it is
        // merged into.
        Object self = this;
        if (self instanceof ShadowFeatureRenderer
                || self instanceof FlameFeatureRenderer
                || self instanceof NameTagFeatureRenderer
                || self instanceof TextFeatureRenderer) {
            cir.setReturnValue(EmptyVertexConsumer.INSTANCE);
            return;
        }

        cir.setReturnValue(BlockBuilderCapture.capture(renderType));
    }
}
