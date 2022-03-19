package at.petrak.hexcasting.common.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

public class BlockEntitySlate extends BlockEntity {
    private static final Random RANDOM = new Random();

    public static final String TAG_PATTERN = "pattern";

    public BlockEntitySlate(BlockPos pos, BlockState state) {
        super(HexBlocks.SLATE_TILE.get(), pos, state);
    }

    public static class Renderer implements BlockEntityRenderer<BlockEntitySlate> {
        private final BlockRenderDispatcher dispatcher;

        public Renderer(BlockEntityRendererProvider.Context ctx) {
            this.dispatcher = ctx.getBlockRenderDispatcher();
        }

        @Override
        public void render(BlockEntitySlate tile, float pPartialTick, PoseStack ps,
            MultiBufferSource buffer, int light, int overlay) {

            // uh
        }
    }
}
