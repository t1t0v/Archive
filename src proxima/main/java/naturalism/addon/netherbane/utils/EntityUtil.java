package naturalism.addon.netherbane.utils;

import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EntityUtil {
    private static Map<Entity, Long> targets = new ConcurrentHashMap<>();

    public static void addTarget(Entity target) {
        targets.put(target, System.currentTimeMillis());
    }

    public static double getPlayerSpeed(PlayerEntity player) {
        if (player == null) return 0;

        double tX = Math.abs(player.getX() - player.prevX);
        double tZ = Math.abs(player.getZ() - player.prevZ);
        double length = Math.sqrt(tX * tX + tZ * tZ);

        Timer timer = Modules.get().get(Timer.class);
        if (timer.isActive()) length *= Modules.get().get(Timer.class).getMultiplier();

        return length * 20;
    }

    public static boolean isBurrowed(LivingEntity target) {
        assert mc.world != null;
        BlockPos pos = target.getBlockPos();
        return BlockUtil.getBlock(pos) == Blocks.OBSIDIAN || BlockUtil.getBlock(pos) == Blocks.BEDROCK || BlockUtil.getBlock(pos) == Blocks.ENDER_CHEST || BlockUtil.getBlock(pos) == Blocks.ANVIL || BlockUtil.getBlock(pos) == Blocks.DAMAGED_ANVIL || BlockUtil.getBlock(pos) == Blocks.CHIPPED_ANVIL || BlockUtil.getBlock(pos) == Blocks.CRYING_OBSIDIAN || BlockUtil.getBlock(pos) == Blocks.ANCIENT_DEBRIS || BlockUtil.getBlock(pos) == Blocks.NETHERITE_BLOCK;
    }

    public static boolean isSurrounded(LivingEntity target) {
        assert mc.world != null;

        return !mc.world.getBlockState(target.getBlockPos().add(1, 0, 0)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(-1, 0, 0)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(0, 0, 1)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(0, 0, -1)).isAir();
    }

    public static boolean isInGreenHole(LivingEntity target) {
        assert mc.world != null;

        return mc.world.getBlockState(target.getBlockPos().add(1, 0, 0)).getBlock() == Blocks.BEDROCK && mc.world.getBlockState(target.getBlockPos().add(-1, 0, 0)).getBlock() == Blocks.BEDROCK && mc.world.getBlockState(target.getBlockPos().add(0, 0, 1)).getBlock() == Blocks.BEDROCK && mc.world.getBlockState(target.getBlockPos().add(0, 0, -1)).getBlock() == Blocks.BEDROCK;
    }

    public static boolean isSelfTraped(PlayerEntity player, double range){
        List<BlockPos> array = BlockUtil.getSelfTrapPos(player).stream().filter(blockPos -> BlockUtil.distance(new BlockPos(mc.player.getEyePos()), blockPos) < range).collect(Collectors.toList());
        for (BlockPos pos : array){
            if (BlockUtil.isAir(pos) || BlockUtil.getBlock(pos) == Blocks.LAVA || BlockUtil.getBlock(pos) == Blocks.FIRE || BlockUtil.getBlock(pos) instanceof BedBlock){
                return false;
            }
        }
        return true;
    }

    public static boolean isInHole(boolean doubles, Entity entity) {
        if (!Utils.canUpdate()) return false;

        BlockPos blockPos = entity.getBlockPos();
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

    public static List<PlayerEntity> getTargetsInRange(Double enemyRange) {
        List<PlayerEntity> stream = mc.world.getPlayers()
            .stream()
            .filter(e -> e != mc.player)
            .filter(e -> e.isAlive())
            .filter(e -> !Friends.get().isFriend((PlayerEntity) e))
            .filter(e -> ((PlayerEntity) e).getHealth() > 0)
            .filter(e -> mc.player.distanceTo(e) < enemyRange)
            .sorted(Comparator.comparing(e -> mc.player.distanceTo(e)))
            .collect(Collectors.toList());

        return stream;
    }

    public static ArrayList<BlockPos> getSurroundBlocks(PlayerEntity player){
        ArrayList<BlockPos> poses = new ArrayList<>();
        BlockPos ppos = player.getBlockPos();
        for (Direction direction : Direction.values()){
            if (direction != Direction.DOWN && direction != Direction.UP) poses.add(ppos.offset(direction));
        }
        return poses;
    }

    public static Direction getMovementDirection(PlayerEntity player){
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

        return null;
    }
}
