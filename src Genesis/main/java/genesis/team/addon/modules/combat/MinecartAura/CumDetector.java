package genesis.team.addon.modules.combat.MinecartAura;

import genesis.team.addon.util.ProximaUtil.BlockUtil.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CumDetector {
    public static boolean isInPiramidka(BlockPos pos, boolean doubles) {
        BlockPos blockPos = pos;
        int air = 0;

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) continue;

            BlockState state = mc.world.getBlockState(blockPos.offset(direction));

            if (state.getBlock() != Blocks.BEDROCK && state.getBlock() != Blocks.OBSIDIAN) {
                if (!doubles || direction == Direction.DOWN) return false;

                air++;

                for (Direction dir : Direction.values()) {
                    if (dir == direction.getOpposite() || dir == Direction.UP) continue;

                    BlockState blockState1 = mc.world.getBlockState(blockPos.offset(direction).offset(dir));

                    if (blockState1.getBlock() != Blocks.BEDROCK && blockState1.getBlock() != Blocks.OBSIDIAN) {
                        return false;
                    }
                }
            }
        }

        return air < 2;
    }

    public static boolean isSwallow(BlockPos pos) {
        return BlockUtil.getBlock(pos) == Blocks.ANVIL || BlockUtil.getBlock(pos) == Blocks.CHIPPED_ANVIL || BlockUtil.getBlock(pos) == Blocks.DAMAGED_ANVIL;
    }

    public static boolean isCum(BlockPos pos) {
        return BlockUtil.getBlock(pos) == Blocks.COBWEB || BlockUtil.getBlock(pos) == Block.getBlockFromItem(Items.STRING);
    }

    public static boolean isRetarded(PlayerEntity p) {
        BlockPos pos = p.getBlockPos();
        return isCockHard(pos) || isSwallow(pos) || isCum(pos) || !isAnal(pos);
    }

    public static boolean isCockHard(BlockPos pos) {
        return BlockUtil.getBlock(pos) == Blocks.ENDER_CHEST || BlockUtil.getBlock(pos) == Blocks.OBSIDIAN;
    }

    public static boolean isAnal(BlockPos pos) {
        return BlockUtil.getBlock(pos) == Blocks.BEDROCK;
    }

    public static boolean isNetvision(BlockPos pos) {
        return pos.equals(mc.player.getBlockPos());
    }
}
