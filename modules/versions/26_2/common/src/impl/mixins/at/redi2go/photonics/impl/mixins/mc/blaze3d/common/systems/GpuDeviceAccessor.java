package at.redi2go.photonics.impl.mixins.mc.blaze3d.common.systems;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the backend behind 26.2's device wrapper.
 *
 * <p>Up to 1.21.11 {@code GpuDevice} was the interface that {@code GlDevice} implemented, so
 * {@code RenderSystem.getDevice()} handed back the OpenGL device itself and Photonics' duck
 * interface rode along on it. 26.2 split the two: {@code GpuDevice} is now a concrete wrapper
 * class and {@code GlDevice} implements {@code GpuDeviceBackend} behind it. The duck interface
 * still belongs on the backend, where the OpenGL state actually lives, so getting to it means
 * unwrapping first.
 */
@Mixin(GpuDevice.class)
public interface GpuDeviceAccessor {
    @Accessor("backend")
    GpuDeviceBackend photonics$backend();
}
