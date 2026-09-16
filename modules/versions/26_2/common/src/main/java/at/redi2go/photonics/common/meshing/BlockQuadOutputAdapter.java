package at.redi2go.photonics.common.meshing;

import at.redi2go.photonics.core.rendering.world.bakery.BlockBuilder;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Vector3fc;

/**
 * Feeds Minecraft 26.2's block geometry into Photonics' {@link BlockBuilder}.
 *
 * <p>Up to 1.21.11 the mesher captured geometry by handing {@code ModelBlockRenderer} a
 * {@code VertexConsumer} and intercepting the vertex stream. Minecraft 26.2 deleted
 * {@code MultiBufferSource} and reworked that path: {@code tesselateBlock} now takes a
 * {@link BlockQuadOutput} and emits whole {@link BakedQuad}s rather than loose vertices.
 *
 * <p>That is a better fit for a voxelizer than the old stream was — quads arrive intact, with their
 * sprite and per-vertex colour, so nothing has to be reassembled. This adapter therefore does the
 * one job of expanding each quad into the four vertices {@code BlockBuilder} expects, leaving the
 * whole downstream meshing pipeline untouched.
 *
 * <p>Fluids do <em>not</em> come through here: {@code FluidRenderer.Output} still yields a
 * {@code VertexConsumer} in 26.2, so that path keeps its existing shape.
 */
public final class BlockQuadOutputAdapter implements BlockQuadOutput {
    private final BlockBuilder builder;

    public BlockQuadOutputAdapter(BlockBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void put(float offsetX, float offsetY, float offsetZ, BakedQuad quad, QuadInstance instance) {
        // BakedQuad positions are in 0..1 block space; the offset places the quad within the
        // section being meshed.
        for (int i = 0; i < 4; i++) {
            Vector3fc position = quad.position(i);
            long packedUv = quad.packedUV(i);

            this.builder
                .addVertex(
                    position.x() + offsetX,
                    position.y() + offsetY,
                    position.z() + offsetZ
                )
                // UVs are already in atlas space -- FaceBakery bakes them through sprite.getU()/getV()
                // at bake time, so they are used as-is rather than mapped through the sprite again.
                .setUv(UVPair.unpackU(packedUv), UVPair.unpackV(packedUv))
                // QuadInstance carries the resolved per-vertex colour, which is where block tint
                // (grass, foliage, redstone) and shading end up in 26.2.
                .setTint(instance.getColor(i));
        }
    }
}
