package at.redi2go.photonics.common.mixins.meshing;

import at.redi2go.photonics.common.meshing.impl.BlockBuilderCapture;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.GizmoFeatureRenderer;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Keeps the two feature renderers that touch {@code RenderSystem} out of Photonics' meshing.
 *
 * <p>Meshing runs on the Photonics chunk compiler threads, so anything reaching
 * {@code RenderSystem} throws "Rendersystem called from wrong thread". Two renderers do so while
 * preparing a frame: {@code QuadParticleFeatureRenderer#finishPrepare}, which the dispatcher calls
 * for every renderer whether or not anything was submitted to it, and
 * {@code GizmoFeatureRenderer#buildGroup}.
 *
 * <p>Neither contributes geometry a voxelizer wants -- particles and debug gizmos are not part of
 * a block -- so skipping them costs nothing. Skipping them matters beyond the thread assertion:
 * a throw inside {@code prepareFrame} means the {@code PreparedFrame} never reaches the caller's
 * try-with-resources, so it is never closed and every later mesh fails with "PreparedFrame already
 * in use".
 */
public final class OffThreadFeatureRendererMixin {
    private OffThreadFeatureRendererMixin() {
    }

    @Mixin(QuadParticleFeatureRenderer.class)
    public static abstract class Particles {
        @Inject(method = "finishPrepare", at = @At("HEAD"), cancellable = true)
        private void photonics$skipWhileMeshing(FeatureFrameContext context, CallbackInfo ci) {
            if (BlockBuilderCapture.isCapturing()) ci.cancel();
        }

        @Inject(method = "prepareGroup", at = @At("HEAD"), cancellable = true)
        private void photonics$skipGroupWhileMeshing(
                FeatureFrameContext context,
                List<?> submits,
                boolean sorted,
                CallbackInfo ci
        ) {
            if (BlockBuilderCapture.isCapturing()) ci.cancel();
        }
    }

    @Mixin(GizmoFeatureRenderer.class)
    public static abstract class Gizmos {
        @Inject(method = "buildGroup", at = @At("HEAD"), cancellable = true)
        private void photonics$skipWhileMeshing(
                FeatureFrameContext context,
                List<?> submits,
                CallbackInfo ci
        ) {
            if (BlockBuilderCapture.isCapturing()) ci.cancel();
        }
    }
}
