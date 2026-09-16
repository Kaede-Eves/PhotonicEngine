package at.redi2go.photonics.common.meshing;

import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelManager;
import at.redi2go.photonics.api.mc.Id;
import at.redi2go.photonics.common.iris.IrisUtil;
import at.redi2go.photonics.common.meshing.impl.BlockBuilderCapture;
import at.redi2go.photonics.common.meshing.impl.BlockSetBuilder;
import at.redi2go.photonics.core.rendering.world.bakery.BlockBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class McBlockRenderer {
    private static final Id BLOCK_ATLAS = (Id) (Object) TextureAtlas.LOCATION_BLOCKS;
    private static final Set<Fluid> WHITELISTED_FLUIDS = Set.of(Fluids.LAVA, Fluids.FLOWING_LAVA);

    private final RandomSource randomSource = RandomSource.create();
    private final PoseStack poseStack = new PoseStack();

    // 26.2 removed BlockRenderDispatcher and Minecraft.getBlockRenderer() entirely. Models, fluid
    // models and block colours all come off ModelManager now.
    private final ModelManager modelManager = Minecraft.getInstance().getModelManager();

    /**
     * Our own model renderer rather than the game's.
     *
     * <p>Ambient occlusion and culling became CONSTRUCTOR flags in 26.2 -- tesselateBlock branches
     * internally to tesselateAmbientOcclusion or tesselateFlat -- so 1.21.11's explicit
     * tesselateWithoutAO call is expressed here instead. AO stays off because the ray tracer
     * computes its own occlusion and baked AO would darken twice; culling stays off because a
     * voxelizer wants every face, not just the ones visible from outside.
     */
    private final ModelBlockRenderer modelRenderer = new ModelBlockRenderer(
            false,
            false,
            Minecraft.getInstance().getBlockColors()
    );

    private final FluidRenderer fluidRenderer = new FluidRenderer(modelManager.getFluidStateModelSet());

    private final SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();

    /**
     * Photonics' own render state and feature dispatcher, kept separate from the game's.
     *
     * <p>26.2 no longer takes buffer sources as constructor arguments, which made sharing the
     * game's dispatcher look reasonable -- but a dispatcher owns a single PreparedFrame, and
     * meshing runs inside the game's frame, so reusing it re-enters that frame and throws
     * "PreparedFrame already in use". Frame isolation, not the buffer sources, is why this is a
     * separate instance.
     *
     * <p>One worker is enough: a block is meshed on one thread at a time. The lightmap still comes
     * from the game's GameRenderer, because prepareFrame reads it from there rather than from this
     * state; only the options come from ours, and those are populated eagerly.
     */
    private final GameRenderState gameRenderState = new GameRenderState();
    private final LevelRenderState levelRenderState = gameRenderState.levelRenderState;

    private final RenderBuffers renderBuffers = new RenderBuffers(1);
    private final FeatureRenderDispatcher featureRenderDispatcher = new FeatureRenderDispatcher(
            renderBuffers,
            modelManager,
            Minecraft.getInstance().getAtlasManager(),
            Minecraft.getInstance().font,
            gameRenderState
    );

    private final SimpleMeshState.HashStorage hashStorage = new SimpleMeshState.HashStorage();

    public McMeshState extractMeshState(
            Vector3i blockChunkOffset,
            BlockPos blockPos,
            BlockState blockState,
            BlockAndTintGetter blockAndTintGetter
    ) {
        if (blockState.getBlock() == Blocks.END_GATEWAY)
            return EmptyMeshState.INSTANCE;

        int blockId = IrisUtil.getBlockId(blockState);
        FluidState fluidState = blockState.getFluidState();

        List<BlockStateModelPart> parts;
        if (blockState.getRenderShape() == RenderShape.MODEL) {
            parts = new ArrayList<>();

            randomSource.setSeed(blockState.getSeed(blockPos));
            modelManager.getBlockStateModelSet().get(blockState).collectParts(randomSource, parts);
        } else parts = List.of();

        if (blockState.hasBlockEntity()) return new DynamicMeshState(blockId, fluidState, parts);
        if (WHITELISTED_FLUIDS.contains(fluidState.getType())) return new DynamicMeshState(blockId, fluidState, parts);

        if (parts.isEmpty())
            return EmptyMeshState.INSTANCE;

        var meshState = new SimpleMeshState(blockState.getBlock(), blockId, parts);
        meshState.computeHash(
                hashStorage,
                blockState,
                blockPos,
                blockAndTintGetter
        );

        return meshState;
    }

    public void meshBlock(
            McMeshState meshState,
            Vector3i blockChunkOffset,
            BlockPos pos,
            BlockState blockState,
            BlockAndTintGetter blockAndTintGetter,
            BlockBuilder builder
    ) {
        if (meshState == EmptyMeshState.INSTANCE) return;

        builder.useBlockId(meshState.blockId());

        FluidState fluidState = meshState.fluidState();
        if (!fluidState.isEmpty()) {
            submitFluid(
                    pos,
                    blockAndTintGetter,
                    builder,
                    blockState,
                    fluidState
            );
        }

        if (blockState.hasBlockEntity()) {
            submitBlockEntity(
                    pos,
                    blockState,
                    blockAndTintGetter,
                    builder
            );
        }

        List<BlockStateModelPart> parts = meshState.blockModel();
        if (!parts.isEmpty()) {
            submitBlock(
                    pos,
                    blockState,
                    blockAndTintGetter,
                    builder,
                    parts
            );
        }
    }

    private void submitFluid(
            BlockPos blockPos,
            BlockAndTintGetter blockAndTintGetter,
            BlockBuilder builder,
            BlockState blockState,
            FluidState fluidState
    ) {
        if (!WHITELISTED_FLUIDS.contains(fluidState.getType())) return;

        builder.useAtlas(BLOCK_ATLAS);
        builder.useOffset(
                -(blockPos.getX() & 15),
                -(blockPos.getY() & 15),
                -(blockPos.getZ() & 15)
        );

        // 26.2: fluids go through FluidRenderer, whose Output still hands back a VertexConsumer --
        // so this path keeps its shape, it just gets reached differently.
        fluidRenderer.tesselate(
                blockAndTintGetter,
                blockPos,
                layer -> (VertexConsumer) builder,
                blockState,
                fluidState
        );
    }

    private void submitBlock(
            BlockPos pos,
            BlockState blockState,
            BlockAndTintGetter blockAndTintGetter,
            BlockBuilder builder,
            List<BlockStateModelPart> parts
    ) {
        builder.useAtlas(BLOCK_ATLAS);
        builder.useOffset(0f, 0f, 0f);

        // 26.2 emits whole BakedQuads into a BlockQuadOutput instead of streaming vertices into a
        // VertexConsumer, and takes the BlockStateModel rather than a parts list -- there is no
        // longer any way to tesselate a subset. Parts are still collected in extractMeshState, but
        // only for the dedup hash.
        modelRenderer.tesselateBlock(
                new BlockQuadOutputAdapter(builder),
                0f,
                0f,
                0f,
                blockAndTintGetter,
                pos,
                blockState,
                modelManager.getBlockStateModelSet().get(blockState),
                blockState.getSeed(pos)
        );
    }

    private static final Set<Block> FULL_BLOCK_ENTITY_REQUIRED_FOR = new BlockSetBuilder()
            .addBlock(Blocks.PLAYER_HEAD)
            .addBlock(Blocks.PLAYER_WALL_HEAD)
            .build();

    private static final Set<Block> LEVEL_REQUIRED_FOR = new BlockSetBuilder()
            .addBlock(Blocks.CHEST)
            .build();

    private BlockEntity copyBlockEntity(BlockState blockState) {
        EntityBlock entityBlock = (EntityBlock) blockState.getBlock();
        return entityBlock.newBlockEntity(new BlockPos(0, 0, 0), blockState);
    }

    private BlockEntity fetchBlockEntity(BlockPos blockPos, BlockState blockState) {
        try {
            var level = Minecraft.getInstance().level;
            if (level == null || !level.isInValidBounds(blockPos)) return copyBlockEntity(blockState);

            var chunk = level.getChunkAt(blockPos);
            return chunk.getBlockEntity(blockPos);
        } catch (Exception e) {
            return copyBlockEntity(blockState);
        }
    }

    private void submitBlockEntity(
            BlockPos blockPos,
            BlockState blockState,
            BlockAndTintGetter blockAndTintGetter,
            BlockBuilder builder
    ) {
        builder.useOffset(0f, 0f, 0f);
        BlockBuilderCapture.begin(builder);

        levelRenderState.reset();

        try {
            BlockEntity entity = FULL_BLOCK_ENTITY_REQUIRED_FOR.contains(blockState.getBlock()) ?
                    fetchBlockEntity(blockPos, blockState) :
                    copyBlockEntity(blockState);

            if (entity == null) return;

            if (LEVEL_REQUIRED_FOR.contains(blockState.getBlock()))
                entity.setLevel((Level) blockAndTintGetter);

            BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer =
                    Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);

            if (renderer == null) return;

            var renderState = renderer.createRenderState();
            renderer.extractRenderState(entity, renderState, 0.8f, levelRenderState.cameraRenderState.pos, null);

            poseStack.pushPose();
            renderer.submit(renderState, poseStack, submitNodeStorage, levelRenderState.cameraRenderState);
            poseStack.popPose();

            // Prepare only, never execute. Preparing is where the feature renderers build their
            // geometry, and so where RenderTypeFeatureRendererMixin diverts it into the block
            // builder; executing is the actual drawing, which is both pointless for a voxelizer
            // and invalid on a chunk compiler thread. renderAllFeatures would do both.
            try (var frame = featureRenderDispatcher.prepareFrame(submitNodeStorage)) {
                // The geometry has already landed in the block builder by this point.
            }
        } finally {
            BlockBuilderCapture.end();
        }
    }
}
