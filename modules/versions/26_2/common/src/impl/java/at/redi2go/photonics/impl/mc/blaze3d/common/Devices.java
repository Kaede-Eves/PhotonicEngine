package at.redi2go.photonics.impl.mc.blaze3d.common;

import at.redi2go.photonics.impl.mixins.mc.blaze3d.systems.GpuDeviceAccessor;
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Reaches Photonics' device implementation through 26.2's public device wrapper.
 *
 * <p>1.21.11 returned the GL device straight from {@code RenderSystem.getDevice()}, so the duck
 * interface could be cast onto it directly. 26.2 wraps it, and the duck interface stays on the
 * backend where the GL state is, so every such cast has to unwrap first -- which is worth exactly
 * one place rather than the sixty-odd call sites in {@code ITextureFormatImpl}.
 */
public final class Devices {
    private Devices() {
    }

    public static GpuDeviceImpl current() {
        return (GpuDeviceImpl) ((GpuDeviceAccessor) (Object) RenderSystem.getDevice()).photonics$backend();
    }
}
