package at.redi2go.photonics.core.iris.rendering.sharp;

import at.redi2go.photonics.api.gpu.textures.ITextureFormat;
import at.redi2go.photonics.core.iris.pipeline.IrisPipeline;
import at.redi2go.photonics.core.iris.properties.PhotonicsProperties;
import at.redi2go.photonics.core.iris.rendering.PhotonicsPipeline;
import at.redi2go.photonics.core.iris.rendering.Pipelines;
import at.redi2go.photonics.core.rendering.lights.HandheldItemSupplier;
import at.redi2go.photonics.core.rendering.world.bakery.texture.AtlasDownloader;

import static at.redi2go.photonics.core.iris.pipeline.texture.AttachmentUsage.CREATE_SAMPLER;

/**
 * Direct lighting with no temporal state.
 *
 * <p>One pass: for every fragment, walk the light list, trace a shadow ray per light and sum the
 * attenuated contributions. There is no reservoir, no history and no denoiser, which is the whole
 * point of this mode -- a light change is visible the frame its voxels land, and the result carries
 * no sampling noise, because nothing is sampled stochastically.
 *
 * <p>The cost is the opposite trade to ReSTIR: every light within range is traced for every
 * fragment, so it scales with emitter count rather than with screen resolution. {@code maxSamples}
 * caps that, and the approximation shows up as hard shadow edges and no indirect light.
 */
public class SharpPipeline extends PhotonicsPipeline {
    public SharpPipeline(
            PhotonicsProperties phProperties,
            SharpProperties sharpProperties,
            AtlasDownloader atlasDownloader,
            HandheldItemSupplier handheldItemSupplier,
            IrisPipeline irisPipeline
    ) {
        super(phProperties, atlasDownloader, irisPipeline);

        // The fragment data and handheld passes are shared with ReSTIR; only the lighting differs.
        Pipelines.fragData(this, phProperties, irisPipeline);
        Pipelines.handheldLighting(this, handheldItemSupplier, phProperties, irisPipeline);

        sharpPipeline(irisPipeline);

        Pipelines.exposureHistory(this, irisPipeline);
    }

    private void sharpPipeline(IrisPipeline irisPipeline) {
        var framebuffer = irisPipeline.newFramebuffer(properties.getRenderScale())
                .addAttachment("sharp_diffuse", ITextureFormat.rgba16f(), CREATE_SAMPLER)
                .build(this::registerComponent);

        irisPipeline.newRenderer()
                .debugGroup("sharp")
                .withFragmentPrefix("/photonics/rendering/sharp/passes/")
                .withFramebuffer(framebuffer)
                .deferredPass("direct lighting", "sh0_direct.fsh", null)
                .build(this::registerRenderer);
    }
}
