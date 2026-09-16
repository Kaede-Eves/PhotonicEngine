package at.redi2go.photonics.impl.mixins.mc.blaze3d.systems;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the GL backend behind the public device.
 *
 * <p>Minecraft 26.2 split Blaze3D into a public API and a backend: {@code RenderSystem.getDevice()}
 * now returns a {@link GpuDevice}, which holds the real {@code GlDevice} in a private field, where
 * 1.21.11 returned the {@code GlDevice} directly. Anything that needs GL-specific state -- debug
 * labels, for instance -- has to reach through this.
 */
@Mixin(GpuDevice.class)
public interface GpuDeviceAccessor {
    @Accessor("backend")
    GpuDeviceBackend photonics$backend();
}
