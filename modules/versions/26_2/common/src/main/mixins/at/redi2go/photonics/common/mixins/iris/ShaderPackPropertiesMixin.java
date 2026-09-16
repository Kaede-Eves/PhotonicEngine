package at.redi2go.photonics.common.mixins.iris;

import at.redi2go.photonics.core.iris.IrisManager;
import at.redi2go.photonics.core.iris.IrisPackPath;
import net.irisshaders.iris.shaderpack.ShaderPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

/**
 * Lets a pack patch reach shader.properties, so its Photonics options appear in the shader menu.
 *
 * <p>Photonics patches files as they pass through Iris' include graph, which covers the GLSL but
 * not the properties: {@code ShaderPack} reads those separately with
 * {@code Files.readString(root.resolve(name))} and never consults the graph. The result is that a
 * patch's shader.properties block -- the {@code screen.photonics} entry, the lighting mode option,
 * and {@code photonics.supported} -- is produced but discarded, so a patched pack like BSL exposes
 * no Photonics settings at all.
 *
 * <p>Patching the string on its way out of the read is enough, and keeps the patch as the single
 * source of truth. Packs with native support are unaffected: they have no patch, and the patcher
 * returns their content untouched.
 */
@Mixin(ShaderPack.class)
public abstract class ShaderPackPropertiesMixin {
    @Inject(method = "readProperties", at = @At("RETURN"), cancellable = true)
    private static void photonics$patchProperties(
            Path root,
            String name,
            CallbackInfoReturnable<String> cir
    ) {
        String source = cir.getReturnValue();
        // Null means the pack has no such file; there is nothing to patch onto.
        if (source == null) return;

        var patcher = IrisManager.getShaderPatcher().orElse(null);
        if (patcher == null) return;

        String patched = patcher.readShaderFile(IrisPackPath.fromAbsolutePath("/" + name), path -> source);
        if (patched != null) cir.setReturnValue(patched);
    }
}
