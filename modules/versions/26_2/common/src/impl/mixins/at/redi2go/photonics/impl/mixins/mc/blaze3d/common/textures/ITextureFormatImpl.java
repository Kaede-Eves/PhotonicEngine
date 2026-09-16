package at.redi2go.photonics.impl.mixins.mc.blaze3d.common.textures;

import at.redi2go.photonics.impl.mc.blaze3d.common.Devices;
import at.redi2go.photonics.api.gpu.textures.ITextureFormat;
import at.redi2go.photonics.impl.mc.blaze3d.common.GpuDeviceImpl;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ITextureFormat.class)
public interface ITextureFormatImpl {
    @Overwrite
	static ITextureFormat rgba() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgba();
	}

    @Overwrite
	static ITextureFormat r8() {
		return (ITextureFormat) Devices.current().getTextureFormats().r8();
	}

    @Overwrite
	static ITextureFormat rg8() {
		return (ITextureFormat) Devices.current().getTextureFormats().rg8();
	}

    @Overwrite
	static ITextureFormat rgb8() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgb8();
	}

    @Overwrite
	static ITextureFormat rgba8() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgba8();
	}

    @Overwrite
	static ITextureFormat r8Snorm() {
		return (ITextureFormat) Devices.current().getTextureFormats().r8Snorm();
	}

    @Overwrite
	static ITextureFormat rg8Snorm() {
		return (ITextureFormat) Devices.current().getTextureFormats().rg8Snorm();
	}

    @Overwrite
	static ITextureFormat rgb8Snorm() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgb8Snorm();
	}

    @Overwrite
	static ITextureFormat rgba8Snorm() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgba8Snorm();
	}

    @Overwrite
	static ITextureFormat r16() {
		return (ITextureFormat) Devices.current().getTextureFormats().r16();
	}

    @Overwrite
	static ITextureFormat rg16() {
		return (ITextureFormat) Devices.current().getTextureFormats().rg16();
	}

    @Overwrite
	static ITextureFormat rgb16() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgb16();
	}

    @Overwrite
	static ITextureFormat rgba16() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgba16();
	}

    @Overwrite
	static ITextureFormat r16Snorm() {
		return (ITextureFormat) Devices.current().getTextureFormats().r16Snorm();
	}

    @Overwrite
	static ITextureFormat rg16Snorm() {
		return (ITextureFormat) Devices.current().getTextureFormats().rg16Snorm();
	}

    @Overwrite
	static ITextureFormat rgb16Snorm() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgb16Snorm();
	}

    @Overwrite
	static ITextureFormat rgba16Snorm() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgba16Snorm();
	}

    @Overwrite
	static ITextureFormat r16f() {
		return (ITextureFormat) Devices.current().getTextureFormats().r16f();
	}

    @Overwrite
	static ITextureFormat rg16f() {
		return (ITextureFormat) Devices.current().getTextureFormats().rg16f();
	}

    @Overwrite
	static ITextureFormat rgb16f() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgb16f();
	}

    @Overwrite
	static ITextureFormat rgba16f() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgba16f();
	}

    @Overwrite
	static ITextureFormat r32f() {
		return (ITextureFormat) Devices.current().getTextureFormats().r32f();
	}

    @Overwrite
	static ITextureFormat rg32f() {
		return (ITextureFormat) Devices.current().getTextureFormats().rg32f();
	}

    @Overwrite
	static ITextureFormat rgb32f() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgb32f();
	}

    @Overwrite
	static ITextureFormat rgba32f() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgba32f();
	}

    @Overwrite
	static ITextureFormat r8i() {
		return (ITextureFormat) Devices.current().getTextureFormats().r8i();
	}

    @Overwrite
	static ITextureFormat rg8i() {
		return (ITextureFormat) Devices.current().getTextureFormats().rg8i();
	}

    @Overwrite
	static ITextureFormat rgb8i() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgb8i();
	}

    @Overwrite
	static ITextureFormat rgba8i() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgba8i();
	}

    @Overwrite
	static ITextureFormat r8ui() {
		return (ITextureFormat) Devices.current().getTextureFormats().r8ui();
	}

    @Overwrite
	static ITextureFormat rg8ui() {
		return (ITextureFormat) Devices.current().getTextureFormats().rg8ui();
	}

    @Overwrite
	static ITextureFormat rgb8ui() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgb8ui();
	}

    @Overwrite
	static ITextureFormat rgba8ui() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgba8ui();
	}

    @Overwrite
	static ITextureFormat r16i() {
		return (ITextureFormat) Devices.current().getTextureFormats().r16i();
	}

    @Overwrite
	static ITextureFormat rg16i() {
		return (ITextureFormat) Devices.current().getTextureFormats().rg16i();
	}

    @Overwrite
	static ITextureFormat rgb16i() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgb16i();
	}

    @Overwrite
	static ITextureFormat rgba16i() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgba16i();
	}

    @Overwrite
	static ITextureFormat r16ui() {
		return (ITextureFormat) Devices.current().getTextureFormats().r16ui();
	}

    @Overwrite
	static ITextureFormat rg16ui() {
		return (ITextureFormat) Devices.current().getTextureFormats().rg16ui();
	}

    @Overwrite
	static ITextureFormat rgb16ui() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgb16ui();
	}

    @Overwrite
	static ITextureFormat rgba16ui() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgba16ui();
	}

    @Overwrite
	static ITextureFormat r32i() {
		return (ITextureFormat) Devices.current().getTextureFormats().r32i();
	}

    @Overwrite
	static ITextureFormat rg32i() {
		return (ITextureFormat) Devices.current().getTextureFormats().rg32i();
	}

    @Overwrite
	static ITextureFormat rgb32i() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgb32i();
	}

    @Overwrite
	static ITextureFormat rgba32i() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgba32i();
	}

    @Overwrite
	static ITextureFormat r32ui() {
		return (ITextureFormat) Devices.current().getTextureFormats().r32ui();
	}

    @Overwrite
	static ITextureFormat rg32ui() {
		return (ITextureFormat) Devices.current().getTextureFormats().rg32ui();
	}

    @Overwrite
	static ITextureFormat rgb32ui() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgb32ui();
	}

    @Overwrite
	static ITextureFormat rgba32ui() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgba32ui();
	}

    @Overwrite
	static ITextureFormat rgba2() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgba2();
	}

    @Overwrite
	static ITextureFormat rgba4() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgba4();
	}

    @Overwrite
	static ITextureFormat r3g3b2() {
		return (ITextureFormat) Devices.current().getTextureFormats().r3g3b2();
	}

    @Overwrite
	static ITextureFormat rgb5a1() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgb5a1();
	}

    @Overwrite
	static ITextureFormat rgb565() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgb565();
	}

    @Overwrite
	static ITextureFormat rgb10a2() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgb10a2();
	}

    @Overwrite
	static ITextureFormat rgb10A2ui() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgb10A2ui();
	}

    @Overwrite
	static ITextureFormat r11fg11fb10f() {
		return (ITextureFormat) Devices.current().getTextureFormats().r11fg11fb10f();
	}

    @Overwrite
	static ITextureFormat rgb9e5() {
		return (ITextureFormat) Devices.current().getTextureFormats().rgb9e5();
	}
}
