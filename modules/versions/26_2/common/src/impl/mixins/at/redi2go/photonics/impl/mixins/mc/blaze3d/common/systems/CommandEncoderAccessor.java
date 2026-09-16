package at.redi2go.photonics.impl.mixins.mc.blaze3d.common.systems;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reaches the backend behind 26.2's command encoder wrapper.
 *
 * <p>The same API/backend split as {@link GpuDeviceAccessor}: {@code CommandEncoder} became a
 * concrete wrapper and {@code GlCommandEncoder} implements {@code CommandEncoderBackend} behind
 * it. Its {@code backend()} is protected, hence an invoker rather than a plain cast.
 */
@Mixin(CommandEncoder.class)
public interface CommandEncoderAccessor {
    @Invoker("backend")
    CommandEncoderBackend photonics$backend();
}
