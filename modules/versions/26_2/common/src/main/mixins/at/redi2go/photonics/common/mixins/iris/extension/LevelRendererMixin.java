package at.redi2go.photonics.common.mixins.iris.extension;

import at.redi2go.photonics.core.iris.IrisManager;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Starts a Photonics frame when the level render begins.
 *
 * <p>26.2 renamed {@code renderLevel} to {@code render} and reworked its parameters: the three
 * loose matrices and the {@code Camera} collapsed into a {@code CameraRenderState} plus a single
 * projection matrix.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(
            method = "render",
            at = @At("HEAD"),
            order = 900
    )
    public void photonics$onFrameBegin(
            GraphicsResourceAllocator graphicsResourceAllocator,
            DeltaTracker deltaTracker,
            boolean bl,
            CameraRenderState cameraRenderState,
            Matrix4fc matrix4fc,
            GpuBufferSlice gpuBufferSlice,
            Vector4f vector4f,
            boolean bl2,
            CallbackInfo ci
    ) {
        IrisManager.onFrameBegin();
    }
}
