package naturalism.addon.netherbane.utils;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PredictionUtil {
    private BlockPos getPlayerPosAfter(PlayerEntity player, int tick){
        BlockPos pos = player.getBlockPos();
        return pos;
    }

    private static Direction getMovementDirection(PlayerEntity player){
        Vec3d velocity = player.getVelocity();

        double x = velocity.x;
        double z = velocity.z;

        if (x < 0 && z < 0){
            if (x < z){
                return Direction.WEST;
            }
            else {
                return Direction.NORTH;
            }
        }
        if (x < 0 && z > 0){
            if (Math.abs(x) > z){
                return Direction.WEST;
            }else {
                return Direction.SOUTH;
            }
        }
        if (x > 0 && z > 0){
            if (x > z) {
                return Direction.EAST;
            }
            else {
                return Direction.SOUTH;
            }
        }
        if (x > 0 && z < 0){
            if (x > Math.abs(z)) {
                return Direction.EAST;
            }
            else {
                return Direction.NORTH;
            }

        }

        return Direction.WEST;
    }

    public static List<BlockPos> getAllPossiblePos(PlayerEntity player, int tick){
        double speed = EntityUtil.getPlayerSpeed(player);
        int count = (int) (speed / 20) * tick;

        return BlockUtil.getSphere(player.getBlockPos(), count, count).stream().filter(blockPos -> BlockUtil.isAir(blockPos) || BlockUtil.getBlock(blockPos) == Blocks.WATER || BlockUtil.getBlock(blockPos) == Blocks.LAVA || BlockUtil.getBlock(blockPos) == Blocks.FIRE).collect(Collectors.toList());
    }

    public static PlayerEntity getNextPlayerEntity(PlayerEntity player, List<BlockPos> array, int tick){
        double speed = EntityUtil.getPlayerSpeed(player);
        int count = (int) (speed / 20) * tick;
        if (count == 0) return player;

        Direction movement = getMovementDirection(player);

        return new PlayerEntity(mc.world, Objects.requireNonNull(getNextPos(getNearestPos(player.getBlockPos(), movement, count, tick), getAllPossiblePos(player, tick))), player.getYaw(), player.getGameProfile()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
    }

    private static BlockPos getNextPos(BlockPos nearest, List<BlockPos> array){
        array.sort(Comparator.comparing(blockPos -> BlockUtil.distance(nearest, blockPos)));
        if (array.isEmpty()) return null;
        return array.get(0);
    }

    private static BlockPos getNearestPos(BlockPos start, Direction move, int count, int tick){
        switch (move){
            case NORTH -> {
                return new BlockPos(start.add(0, 0, -count));
            }
            case SOUTH -> {
                return new BlockPos(start.add(0, 0, +count));
            }
            case EAST ->  {
                return new BlockPos(start.add(+count, 0, 0));
            }
            case WEST -> {
                return new BlockPos(start.add(-count, 0, 0));
            }
        }
        return null;
    }


}
