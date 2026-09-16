package at.redi2go.photonics.impl.mc.blaze3d.opengl;

import at.redi2go.photonics.impl.mc.blaze3d.opengl.textures.GlTexture2D;
import at.redi2go.photonics.impl.mc.blaze3d.opengl.textures.IGlTexture;
import at.redi2go.photonics.impl.mixins.mc.blaze3d.systems.GpuDeviceAccessor;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.systems.RenderSystem;

public interface GlDebugLabelExt {
    void applyLabel(IGlTexture texture2D);

    static GlDebugLabelExt getInstance() {
        // 26.2: getDevice() returns the public GpuDevice, which wraps GlDevice as a private
        // backend field. See GpuDeviceAccessor.
        return (GlDebugLabelExt) ((GlDevice) ((GpuDeviceAccessor) (Object) RenderSystem.getDevice())
                .photonics$backend()).debugLabels();
    }
}
