package at.redi2go.photonics.impl.mixins.mc.blaze3d.opengl.buffer;

import at.redi2go.photonics.api.gpu.buffers.BufferUsage;
import at.redi2go.photonics.impl.mc.blaze3d.opengl.buffer.GlBufferHeap;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Keeps Photonics' own buffers out of persistent mapping.
 *
 * <p>Up to 1.21.11 this vetoed {@code tryMapBufferPersistent} at the point of use. 26.2 removed
 * that method: whether a buffer can be persistently mapped is now decided once, at creation, and
 * passed to {@code GlBuffer.Direct} as a constructor flag that {@code Immutable#createBuffer}
 * hardcodes to true. The veto therefore moves to that argument.
 *
 * <p>Both {@code createBuffer} overloads take the usage flags as their only int parameter, so
 * {@code argsOnly} resolves it unambiguously in each.
 */
@Mixin(targets = "com.mojang.blaze3d.opengl.BufferStorage$Immutable")
public abstract class ImmutableBufferStorageMixin {
    @ModifyArg(
            method = "createBuffer",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/opengl/GlBuffer$Direct;<init>(Lcom/mojang/blaze3d/opengl/DirectStateAccess;IJIZ)V"
            ),
            index = 4
    )
    private boolean photonics$vetoPersistentMapping(
            boolean canPersistentMap,
            @Local(argsOnly = true) @BufferUsage int usage
    ) {
        if ((usage & GlBufferHeap.NO_PERSISTENCE_MAPPING) != 0) return false;

        return canPersistentMap;
    }
}
