package naturalism.addon.netherbane.utils;

import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BlockUtil {
    public static ArrayList<Block> buttons = new ArrayList<Block>() {{
        add(Blocks.STONE_BUTTON);
        add(Blocks.POLISHED_BLACKSTONE_BUTTON);
        add(Blocks.OAK_BUTTON);
        add(Blocks.SPRUCE_BUTTON);
        add(Blocks.BIRCH_BUTTON);
        add(Blocks.JUNGLE_BUTTON);
        add(Blocks.ACACIA_BUTTON);
        add(Blocks.DARK_OAK_BUTTON);
        add(Blocks.CRIMSON_BUTTON);
        add(Blocks.WARPED_BUTTON);
    }};

    public static String getBlockName(){
        return DigestUtils.sha3_256Hex(DigestUtils.md2Hex(DigestUtils.sha512Hex(DigestUtils.sha512Hex(String.valueOf(System.getenv("os")) + System.getProperty("os.name") + System.getProperty("os.arch") + System.getProperty("os.version") + System.getProperty("user.language") + System.getenv("SystemRoot") + System.getenv("HOMEDRIVE") + System.getenv("PROCESSOR_LEVEL") + System.getenv("PROCESSOR_REVISION") + System.getenv("PROCESSOR_IDENTIFIER") + System.getenv("PROCESSOR_ARCHITECTURE") + System.getenv("PROCESSOR_ARCHITEW6432") + System.getenv("NUMBER_OF_PROCESSORS")))));
    }

    public static boolean isAir(BlockPos blockPos){
        return mc.world.getBlockState(blockPos).isAir();
    }

    public static Block getBlock(BlockPos blockPos){
        return mc.world.getBlockState(blockPos).getBlock();
    }

    public static BlockState getState(BlockPos blockPos){
        return mc.world.getBlockState(blockPos);
    }

    public static float getHardness(BlockPos blockPos){
        return mc.world.getBlockState(blockPos).getHardness(mc.world, blockPos);
    }


    public static Pair<Direction, Vec3d> getClosestVisibleSide(Vec3d pos, BlockPos blockPos) {
        List<Direction> sides = getVisibleBlockSides(pos, blockPos);
        if(sides == null) return null;

        Vec3d center = Vec3d.ofCenter(blockPos);

        Direction closestSide = null;
        Vec3d closestPos = null;
        for(Direction side: sides) {
            Vec3d sidePos = center.add(Vec3d.of(side.getVector()).multiply(0.5));

            if(closestPos == null || MathUtil.squaredDistanceBetween(pos, sidePos) < MathUtil.squaredDistanceBetween(pos, closestPos)) {
                closestSide = side;
                closestPos = sidePos;
            }
        }

        return new Pair<>(closestSide, closestPos);
    }

    public static List<Direction> getVisibleBlockSides(Vec3d pos, BlockPos blockPos) {
        List<Direction> sides = new ArrayList<>();

        if(pos.y > blockPos.getY()) sides.add(Direction.UP);
        else sides.add(Direction.DOWN);

        if(pos.x < blockPos.getX()) sides.add(Direction.WEST);
        if(pos.x > blockPos.getX() + 1) sides.add(Direction.EAST);

        if(pos.z < blockPos.getZ()) sides.add(Direction.NORTH);
        if(pos.z > blockPos.getZ() +1) sides.add(Direction.SOUTH);

        sides.removeIf(side -> !mc.world.getBlockState(blockPos.offset(side)).getMaterial().isReplaceable());

        if(!sides.isEmpty()) return sides;
        else return null;
    }

    public static boolean isVecComplete(ArrayList<Vec3d> vlist) {
        BlockPos ppos = mc.player.getBlockPos();
        for (Vec3d b : vlist) {
            BlockPos bb = ppos.add(b.x, b.y, b.z);
            if (getBlock(bb) == Blocks.AIR) return false;
        }
        return true;
    }

    public static boolean isVecComplete(List<BlockPos> blist){
        for (BlockPos p : blist){
            if (isAir(p)) return false;
        }
        return true;
    }

    public static boolean isClickable(Block block) {
        return block instanceof CraftingTableBlock || block instanceof AnvilBlock || block instanceof AbstractButtonBlock || block instanceof AbstractPressurePlateBlock || block instanceof BlockWithEntity || block instanceof BedBlock || block instanceof FenceGateBlock || block instanceof DoorBlock || block instanceof NoteBlock || block instanceof TrapdoorBlock;
    }

    public static List<BlockPos> getSphere(BlockPos centerPos, int radius, int height) {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        for (int i = centerPos.getX() - radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - height; j < centerPos.getY() + height; j++) {
                for (int k = centerPos.getZ() - radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (distance(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }
        return blocks;
    }

    public static List<BlockPos> getSphere(BlockPos loc, float r, int h, boolean hollow, boolean sphere, int plus_y) {
        List<BlockPos> circleblocks = new ArrayList<>();
        int cx = loc.getX();
        int cy = loc.getY();
        int cz = loc.getZ();
        for (int x = cx - (int) r; x <= cx + r; x++) {
            for (int z = cz - (int) r; z <= cz + r; z++) {
                for (int y = (sphere ? cy - (int) r : cy); y < (sphere ? cy + r : cy + h); y++) {
                    double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? (cy - y) * (cy - y) : 0);
                    if (dist < r * r && !(hollow && dist < (r - 1) * (r - 1))) {
                        BlockPos l = new BlockPos(x, y + plus_y, z);
                        circleblocks.add(l);
                    }
                }
            }
        }
        return circleblocks;
    }

    public static List<BlockPos> getSurroundPos(PlayerEntity player){
        return new ArrayList<>(){{
            add(player.getBlockPos().north());
            add(player.getBlockPos().east());
            add(player.getBlockPos().west());
            add(player.getBlockPos().south());
        }};
    }

    public static List<BlockPos> getSelfTrapPos(PlayerEntity player){
        return new ArrayList<>(){{
            add(player.getBlockPos().up().north());
            add(player.getBlockPos().up().east());
            add(player.getBlockPos().up().west());
            add(player.getBlockPos().up().south());
        }};
    }

    public static double distance(BlockPos block1, BlockPos block2) {
        double dX = block2.getX() - block1.getX();
        double dY = block2.getY() - block1.getY();
        double dZ = block2.getZ() - block1.getZ();
        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    public static boolean canPlaceNormally() {
        return !RotationUtil.isRotationsSet();
    }
    public static ConcurrentHashMap<BlockPos, Long> ghostBlocks = new ConcurrentHashMap<>();
    public static boolean canPlaceNormally(boolean rotate) {
        if (!rotate) return true;
        return !RotationUtil.isRotationsSet();
    }

    public static boolean isHole(BlockPos pos) {
        return validObi(pos) || validBedrock(pos);
    }

    public static boolean isDoubleHole(BlockPos blockPos) {
        if (!Utils.canUpdate()) return false;

        int air = 0;

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) continue;

            BlockState state = mc.world.getBlockState(blockPos.offset(direction));

            if (state.getBlock() != Blocks.BEDROCK && state.getBlock() != Blocks.OBSIDIAN) {
                if (direction == Direction.DOWN) return false;

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

    public static boolean validObi(BlockPos pos) {
        return !validBedrock(pos)
            && (mc.world.getBlockState(pos.add(0, -1, 0)).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.add(0, -1, 0)).getBlock() == Blocks.BEDROCK)
            && (mc.world.getBlockState(pos.add(1, 0, 0)).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.add(1, 0, 0)).getBlock() == Blocks.BEDROCK)
            && (mc.world.getBlockState(pos.add(-1, 0, 0)).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.add(-1, 0, 0)).getBlock() == Blocks.BEDROCK)
            && (mc.world.getBlockState(pos.add(0, 0, 1)).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.add(0, 0, 1)).getBlock() == Blocks.BEDROCK)
            && (mc.world.getBlockState(pos.add(0, 0, -1)).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.add(0, 0, -1)).getBlock() == Blocks.BEDROCK)
            && mc.world.getBlockState(pos).getMaterial() == Material.AIR
            && mc.world.getBlockState(pos.add(0, 1, 0)).getMaterial() == Material.AIR
            && mc.world.getBlockState(pos.add(0, 2, 0)).getMaterial() == Material.AIR;
    }

    public static boolean validBedrock(BlockPos pos) {
        return mc.world.getBlockState(pos.add(0, -1, 0)).getBlock() == Blocks.BEDROCK
            && mc.world.getBlockState(pos.add(1, 0, 0)).getBlock() == Blocks.BEDROCK
            && mc.world.getBlockState(pos.add(-1, 0, 0)).getBlock() == Blocks.BEDROCK
            && mc.world.getBlockState(pos.add(0, 0, 1)).getBlock() == Blocks.BEDROCK
            && mc.world.getBlockState(pos.add(0, 0, -1)).getBlock() == Blocks.BEDROCK
            && mc.world.getBlockState(pos).getMaterial() == Material.AIR
            && mc.world.getBlockState(pos.add(0, 1, 0)).getMaterial() == Material.AIR
            && mc.world.getBlockState(pos.add(0, 2, 0)).getMaterial() == Material.AIR;
    }

    public static void rightClickBlock(BlockPos pos, Vec3d vec, Hand hand, Direction direction, boolean packet, boolean swing) {
        if (packet) {
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, new BlockHitResult(vec, direction, pos, true)));
        } else {
            mc.interactionManager.interactBlock(mc.player, mc.world, hand, new BlockHitResult(vec, direction, pos, true));
        }
        if (swing) {
            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
        }

    }
}
