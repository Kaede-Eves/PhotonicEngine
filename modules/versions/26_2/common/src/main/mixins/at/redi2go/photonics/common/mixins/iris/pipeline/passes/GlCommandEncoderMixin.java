package at.redi2go.photonics.common.mixins.iris.pipeline.passes;

import at.redi2go.photonics.common.iris.IrisUtil;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Binds Photonics' shader storage buffers whenever a shader program is bound.
 *
 * <p>Up to 1.21.11 this hung off Sodium's own {@code GlProgram#bind}. Sodium 0.9 dropped that class
 * and now goes through Blaze3D's pipeline system, where {@code GlCommandEncoder#trySetup} is what
 * actually calls {@code glUseProgram} and records the result. Injecting there covers terrain and
 * every other pipeline in one place, including Iris' {@code ExtendedShader}, which extends
 * Blaze3D's {@code GlProgram}.
 *
 * <p>The Iris composite, final and shadow passes bind their programs directly rather than through
 * a render pass, so they keep their own dedicated injections.
 */
@Mixin(GlCommandEncoder.class)
public abstract class GlCommandEncoderMixin {
    @Shadow
    private GlProgram lastProgram;

    @Inject(method = "trySetup", at = @At("RETURN"))
    private void photonics$bindBuffers(CallbackInfoReturnable<Boolean> cir) {
        // A failed setup leaves no program bound.
        if (!cir.getReturnValueZ() || lastProgram == null) return;

        IrisUtil.bindBuffers(lastProgram.getProgramId());
    }
}
