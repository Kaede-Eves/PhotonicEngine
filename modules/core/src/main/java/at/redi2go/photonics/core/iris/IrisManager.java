package at.redi2go.photonics.core.iris;

import at.redi2go.photonics.core.Photonics;
import at.redi2go.photonics.core.iris.patching.ShaderPatcher;
import at.redi2go.photonics.core.iris.pipeline.DefineHolder;
import at.redi2go.photonics.core.iris.pipeline.IrisPipeline;
import at.redi2go.photonics.core.iris.pipeline.buffer.IBufferHolder;
import at.redi2go.photonics.core.iris.pipeline.texture.ISamplerHolder;
import at.redi2go.photonics.core.iris.pipeline.uniform.IDynamicUniformHolder;
import at.redi2go.photonics.core.iris.pipeline.uniform.IUniformHolder;
import at.redi2go.photonics.core.iris.properties.PhotonicsProperties;
import at.redi2go.photonics.core.iris.properties.impl.PropertiesManager;
import at.redi2go.photonics.core.iris.rendering.PhotonicsPipeline;
import at.redi2go.photonics.core.iris.rendering.PhotonicsRenderer;
import at.redi2go.photonics.core.rendering.lights.HandheldItemSupplier;
import at.redi2go.photonics.core.rendering.world.bakery.texture.AtlasDownloader;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

public class IrisManager {
    private static final PropertiesManager propertiesManager = new PropertiesManager();

    private static @Nullable ShaderPatcher activePatcher = null;
    private static @Nullable PhotonicsProperties activeProperties = null;
    private static @Nullable PhotonicsPipeline activePipeline = null;

    public static Optional<ShaderPatcher> getShaderPatcher() {
        return  Optional.ofNullable(activePatcher);
    }

    public static ShaderPatcher getShaderPatcherOrThrow() {
        return getShaderPatcher().orElseThrow();
    }

    public static Optional<PhotonicsProperties> getProperties() {
        return Optional.ofNullable(activeProperties);
    }

    public static PhotonicsProperties getPropertiesOrThrow() {
        return getProperties().orElseThrow();
    }

    /**
     * Whether Photonics is enabled for the pack currently being loaded.
     *
     * <p>Iris builds a pack's include graph before it parses shader.properties, so shader files
     * are read -- Photonics' own among them -- while {@link #getProperties()} is still empty.
     * Asking {@link #getPropertiesOrThrow()} at that point throws and Iris reports the pack as
     * unloadable.
     *
     * <p>The answer is already known by then even so. A pack that declares photonics.enabled is
     * handled by the patcher as natively supported, and one that does not gets the flag forced on
     * by {@code setProperties} under exactly the condition {@code setupShaderPatcher} recorded --
     * so the patcher's own decision is the same signal, just available earlier.
     */
    public static boolean isPhotonicsEnabled() {
        return getProperties()
                .map(PhotonicsProperties::isEnabled)
                .orElseGet(() -> propertiesManager.isForceEnabled()
                        || (activePatcher != null && activePatcher.packSupportsPhotonics()));
    }

    public static boolean hasPipeline() {
        return activePipeline != null;
    }

    public static void setupShaderPatcher(@NonNls IrisPack pack, boolean patchEnabled) {
        Objects.requireNonNull(pack, "pack");

        activePatcher = new ShaderPatcher(pack);
        propertiesManager.setForceEnabled(!pack.ph$supportsPhotonics() && activePatcher.hasPatch() && patchEnabled);
    }

    public static void setupProperties(@NonNls Properties properties, @NonNls Logger logger) {
        Objects.requireNonNull(properties, "properties");
        Objects.requireNonNull(logger, "logger");

        destroyEverything(false);
        propertiesManager.setProperties(properties, logger);

        activeProperties = propertiesManager.getProperties(PhotonicsProperties.class);
    }
    
    public static void setupPipeline(
            @NonNls Supplier<AtlasDownloader> atlasDownloaderSupplier,
            @NonNls Supplier<HandheldItemSupplier> handheldItemSupplierSupplier,
            @NonNls IrisPipeline irisPipeline
    ) {
        Objects.requireNonNull(atlasDownloaderSupplier, "atlasDownloaderSupplier");
        Objects.requireNonNull(handheldItemSupplierSupplier, "handheldItemSupplierSupplier");
        Objects.requireNonNull(irisPipeline, "irisPipeline");

        if (activeProperties == null) throw new IllegalStateException("The renderer has not been set up");
        if (activePipeline != null) throw new IllegalStateException("Pipeline has already been created");
        
        activePipeline = PhotonicsRenderer.createPipeline(
                propertiesManager,
                atlasDownloaderSupplier,
                handheldItemSupplierSupplier,
                irisPipeline
        );

        // Say plainly whether Photonics is doing anything for this pack. Without it the only way
        // to answer "is Photonics even running?" is to read the pack's properties and infer, which
        // is guesswork when a pack decides enablement itself -- Complementary sets
        // photonics.enabled from its own lighting mode, so selecting Off there removes Photonics
        // from the frame entirely rather than picking the OFF renderer.
        if (activePipeline == null)
            Photonics.LOGGER.info("Photonics is disabled for this shaderpack; no pipeline created");
        else
            Photonics.LOGGER.info("Photonics renderer: {}", activeProperties.getRenderer());
    }

    public static void onRender() {
        var pipeline = activePipeline;
        if (pipeline != null)
            pipeline.onRender();
    }

    public static void onFrameBegin() {
        var pipeline = activePipeline;
        if (pipeline != null)
            pipeline.onFrameBegin();
    }

    public static void onSectionAdded(int x, int y, int z) {
        var pipeline = activePipeline;
        if (pipeline != null)
            pipeline.onSectionAdded(x, y, z);
    }

    public static void onSectionChanged(int x, int y, int z) {
        var pipeline = activePipeline;
        if (pipeline != null)
            pipeline.onSectionChanged(x, y, z);
    }

    public static void registerVersionDefines(DefineHolder defines) {
        defines.stringDefine("PHOTONICS", "");
        defines.stringDefine("PHOTONICS_VERSION", Photonics.getVersionString());
    }

    public static void registerDefines(DefineHolder defines) {
        propertiesManager.registerDefines(defines);
    }

    public static void registerUniforms(IUniformHolder uniforms) {
        var pipeline = activePipeline;
        if (pipeline != null)
            pipeline.registerUniforms(uniforms);
    }

    public static void registerDynamicUniforms(IDynamicUniformHolder dynamicUniforms) {
        var pipeline = activePipeline;
        if (pipeline != null)
            pipeline.registerDynamicUniforms(dynamicUniforms);
    }

    public static void registerBuffers(IBufferHolder buffers) {
        var pipeline = activePipeline;
        if (pipeline != null)
            pipeline.registerBuffers(buffers);
    }

    public static void registerCustomTextures(ISamplerHolder samplers) {
        var pipeline = activePipeline;
        if (pipeline != null)
            pipeline.registerCustomTextures(samplers);
    }

    public static void destroyEverything() {
        destroyEverything(true);
    }

    private static void destroyEverything(boolean destroyPatcher) {
        var pipeline = activePipeline;
        if (pipeline == null) return;

        activePatcher = destroyPatcher ? null : activePatcher;
        activePipeline = null;
        pipeline.close();
    }

    private IrisManager() {
    }
}
